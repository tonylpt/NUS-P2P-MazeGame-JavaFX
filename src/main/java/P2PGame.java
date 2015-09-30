import javafx.application.Platform;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author lpthanh
 */
public class P2PGame extends UnicastRemoteObject implements IPeer {

    public static final long PING_INTERVAL = 4000;

    public static final String NAME_PEER = "FRIENDLY_PEER";

    private final RMIServer rmiServer;

    private final ILogger logger;

    private final GameUI.UIController uiController;

    private final IPeer self;

    private final GameClient gameClient = new GameClient();

    private PrimaryServer primaryServer = new NilPrimaryServer();

    private BackupServer backupServer = new NilBackupServer();

    private final ExecutorService exec = Executors.newCachedThreadPool();

    public P2PGame(GameParams params, ILogger logger, GameUI.UIController uiController) throws RemoteException {
        this.self = this;
        this.logger = logger;
        this.uiController = uiController;
        this.rmiServer = createServer(params.isPrimary(), params.getHostPort().getPort(), logger);

        if (this.rmiServer == null) {
            throw new RuntimeException("Unable to create Peer object.");
        }

        start(params);
    }

    private void start(GameParams params) {
        if (params instanceof GameParams.PrimaryParams) {
            GameParams.PrimaryParams primaryParams = (GameParams.PrimaryParams) params;
            primaryServer = new BootstrappingPrimaryServer(
                    primaryParams.getBoardSize(), primaryParams.getTreasureCount()
            );

            exec.execute(primaryServer::start);
        } else if (params instanceof GameParams.NonPrimaryParams) {
            GameParams.HostPort hostPort = params.getHostPort();
            exec.execute(() -> gameClient.connectToPrimary(hostPort.getHost(), hostPort.getPort()));
        }
    }

    /**
     * Try to binds to a port, for both primary server and non-server players.
     * This is because each player can potentially become a server.
     */
    private static RMIServer createServer(boolean primary, int port, ILogger logger) throws RemoteException {
        if (primary) {
            // try to create registry on the port

            logger.serverLog("Attempting to listen on port " + port);
            Registry registry = LocateRegistry.createRegistry(port);
            logger.serverLog("Server registry was successfully created on port " + port);
            return new RMIServer(registry, port);

        } else {

            // try to find an available port to create the registry on
            Random randomizer = new Random();
            boolean retry = false;
            int retryLeft = 10;

            do {
                try {
                    int listenPort = port + randomizer.nextInt(1000);
                    logger.clientLog("Attempting to bind a client on port " + listenPort);
                    Registry registry = LocateRegistry.createRegistry(listenPort);
                    logger.clientLog("Client registry was successfully created on port " + listenPort);
                    return new RMIServer(registry, listenPort);

                } catch (RemoteException e) {
                    // the exception may have been caused by the port's unavailability
                    retry = true;
                    if (retryLeft-- <= 0) {
                        // retry too many times already
                        throw e;
                    }
                }
            } while (retry);
        }

        return null;
    }

    /**
     * Every peer acts as an RMI Server. This class is to contain the port that the peer listens on,
     * and the registry created on that port.
     */
    private static class RMIServer {

        private final Registry registry;

        private final int listenPort;

        public RMIServer(Registry registry, int listenPort) {
            this.registry = registry;
            this.listenPort = listenPort;
        }

        public Registry getRegistry() {
            return registry;
        }

        public int getListenPort() {
            return listenPort;
        }

    }

    /**
     * Run by Client
     */
    @Override
    public void callClientGameStarted(GameState gameState) throws RemoteException {
        gameClient.callClientGameStarted(gameState);
    }

    /**
     * Run by Primary Server
     */
    @Override
    public IReply.JoinReply callPrimaryJoin(IPeer peer) throws RemoteException {
        return primaryServer.callPrimaryJoin(peer);
    }

    /**
     * Run by Primary Server
     */
    @Override
    public IReply.MoveReply callPrimaryMove(IPeer peer,
                                            Move.Direction direction,
                                            String playerId,
                                            String authCode) throws RemoteException {

        return primaryServer.callPrimaryMove(peer, direction, playerId, authCode);
    }

    /**
     * Run by Primary Server
     */
    @Override
    public IReply.PingReply callPrimaryPing(IPeer peer,
                                            String playerId,
                                            String authCode) throws RemoteException {

        return primaryServer.callPrimaryPing(peer, playerId, authCode);
    }

    /**
     * Run by Backup Server
     */
    @Override
    public void callBackupUpdate(GameState gameState, ServerSecrets serverSecrets) throws RemoteException {

        backupServer.callBackupUpdate(gameState, serverSecrets);
    }

    /**
     * Run by Backup Server
     */
    @Override
    public IReply.PingReply callBackupOnPrimaryDied(IPeer peer,
                                                    String playerId,
                                                    String authCode,
                                                    IPeer deadPrimary) throws RemoteException {

        return backupServer.callBackupOnPrimaryDied(peer, playerId, authCode, deadPrimary);
    }

    private class GameClient {

        private GameState gameState;

        private String playerId;

        private String authCode;

        public String getPlayerId() {
            return playerId;
        }

        public void setPlayerId(String playerId) {
            this.playerId = playerId;
        }

        public String getAuthCode() {
            return authCode;
        }

        public void setAuthCode(String authCode) {
            this.authCode = authCode;
        }

        private void setGameState(GameState gameState) {
            this.gameState = gameState;
            Platform.runLater(() -> uiController.onGameStarted(gameState));
        }

        public void callClientGameStarted(GameState gameState) {
            setGameState(gameState);
        }

        public boolean connectToPrimary(String host, int port) {
            try {
                logger.clientLog("Connecting to Primary Server at " + host + ":" + port);

                Registry registry = LocateRegistry.getRegistry(host, port);
                IPeer primaryServer = (IPeer) registry.lookup(NAME_PEER);
                IReply.JoinReply reply = primaryServer.callPrimaryJoin(self);

                if (!reply.isAccepted()) {
                    logger.clientLog("Join request was rejected by Primary Server.");
                    return false;
                }

                this.playerId = reply.getPlayerId();
                logger.clientLog("Player has joined the game, with ID = [" + getPlayerId() + "]");

                if (reply.shouldBecomeBackup()) {
                    logger.clientLog("Player was promoted to be the Backup Server.");
                    backupServer = new ActiveBackupServer();
                }

                startPulseChecking();

                return true;

            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
                logger.clientLogError("Unable to connect to primary server", e);
                return false;
            }
        }

        public void startPulseChecking() {
            final Timer timer = new Timer();
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    doPulseCheck();
                }
            };

            timer.scheduleAtFixedRate(task, 0, PING_INTERVAL);
        }

        private void doPulseCheck() {
            IPeer primaryServer = gameState.getServerConfig().getPrimaryServer();

            try {
                // contact primary
                IReply.PingReply reply = primaryServer.callPrimaryPing(self, playerId, authCode);
                processPingReply(reply);

            } catch (RemoteException e) {
                logger.clientLog("Primary Server is down. Contacting Backup Server.");

                // contact backup
                IPeer backupServer = gameState.getServerConfig().getBackupServer();
                try {
                    IReply.PingReply reply = backupServer.callBackupOnPrimaryDied(self, playerId, authCode, primaryServer);
                    processPingReply(reply);
                } catch (RemoteException e1) {
                    // both server is down. sos
                    logger.clientLog("Both servers are down without alternative.");
                }
            }
        }

        private void processPingReply(IReply.PingReply reply) {
            switch (reply.getPromotionStatus()) {
                case PROMOTED_TO_PRIMARY: {
                    primaryServer = new InheritingPrimaryServer(reply.getGameState(), reply.getServerSecrets());
                    setGameState(reply.getGameState());
                    break;
                }
                case PROMOTED_TO_BACKUP: {
                    backupServer = new ActiveBackupServer(reply.getGameState(), reply.getServerSecrets());
                    setGameState(reply.getGameState());
                    break;
                }
                default: {
                    setGameState(reply.getGameState());
                }
            }
        }

    }

    /**
     * This parent class contains the basic implementation for a primary server's logic.
     */
    private abstract class PrimaryServer {

        protected GameState gameState;

        protected ServerSecrets serverSecrets;

        protected final Object gameStateLock = new Object();

        protected Map<IPeer, Long> peerLastAccessMillis = new HashMap<>();

        protected boolean promoteNewBackupServer;

        protected PrimaryServer() {

        }

        public void start() {

        }

        /**
         * Check through all the peers to know if they are still alive, based on their
         * last request's timing.
         */
        public void startPulseChecking() {
            final Timer timer = new Timer();
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    doPulseCheck();
                }
            };

            timer.scheduleAtFixedRate(task, 0, (int) (PING_INTERVAL * 1.5));
        }

        public abstract IReply.JoinReply callPrimaryJoin(IPeer peer);

        /**
         * Obtain and verify the player object from player ID and auth code
         */
        private Player authenticatePlayer(IPeer peer, String playerId, String authCode) {
            Player player = gameState.searchById(playerId);

            if (player == null || !serverSecrets.auth(peer, playerId, authCode)) {
                // not found
                return null;
            }

            return player;
        }

        public IReply.MoveReply callPrimaryMove(IPeer peer,
                                                Move.Direction direction,
                                                String playerId,
                                                String authCode) {

            synchronized (gameStateLock) {
                IPeer backupServer = gameState.getServerConfig().getBackupServer();
                if (backupServer == null) {
                    throw new Error("Backup Server is unavailable");
                }

                Player player = authenticatePlayer(peer, playerId, authCode);
                if (player == null) {
                    logger.serverLog("Reject move request from player ID: " + playerId + " due to invalid ID or auth code.");
                    return IReply.MoveReply.createIllegal(gameState);
                }

                // passed all preliminary checks
                updatePeerAlive(peer);
                logger.serverLog("Player [" + playerId + "] is making a move");

                boolean gameEnded = true;
                for (Treasure oneTreasure : gameState.getTreasureList()) {
                    if (oneTreasure.getAssignedPlayerId() == null) {
                        gameEnded = false;
                    }
                }

                if (gameEnded) {
                    gameState.setRunningState(RunningState.GAME_ENDED);
                    return IReply.MoveReply.createIllegal(gameState);
                }


                // check and update game state
                Move move = new Move(direction, playerId);
                boolean illegalMove = gameState.processMove(move, player);

                if (!updateBackup()) {
                    // failed to update to backup
                    // only try to promote if peer is not the primary server
                    if (!peer.equals(self)) {
                        if (promotePeerAsBackupIfNeeded(peer)) {
                            return IReply.MoveReply.createPromoteToBackup(gameState, serverSecrets, illegalMove);
                        }
                    }
                }

                return IReply.MoveReply.createReply(gameState, illegalMove);
            }
        }

        /**
         * @return whether backup is still up
         */
        protected boolean updateBackup() {
            synchronized (gameStateLock) {
                IPeer backupServer = gameState.getServerConfig().getBackupServer();
                try {
                    backupServer.callBackupUpdate(gameState, serverSecrets);
                    updatePeerAlive(backupServer);
                    return true;
                } catch (RemoteException e) {
                    // backup died
                    logger.serverLog("Backup Server cannot be reached");
                    promoteNewBackupServer = true;
                    return false;
                }
            }
        }

        /**
         * Called when receive a request from player, so know that it's alive.
         */
        protected void updatePeerAlive(IPeer peer) {
            if (gameState.getServerConfig().getBackupServer().equals(peer)) {
                synchronized (gameStateLock) {
                    // double synchronized to ensure correctness and performance
                    if (gameState.getServerConfig().getBackupServer().equals(peer)) {
                        promoteNewBackupServer = false;
                    }
                }
            }

            peerLastAccessMillis.put(peer, System.currentTimeMillis());
        }

        /**
         * Check the liveliness statuses of the peers
         */
        protected void doPulseCheck() {
            gameState.getPlayerList().forEach(player -> {
                IPeer peer = serverSecrets.getPeers().get(player.getId());
                Long lastAccessMillis = peerLastAccessMillis.get(peer);
                if (lastAccessMillis == null) {
                    return;
                }

                long silentPeriod = System.currentTimeMillis() - lastAccessMillis;
                if (silentPeriod > 2 * PING_INTERVAL) {
                    if (peer.equals(self)) {
                        // peer is primary server. ignoring.
                    } else if (peer.equals(gameState.getServerConfig().getBackupServer())) {
                        logger.serverLog("Backup Server seems dormant. Retrying.");
                        if (!updateBackup()) {
                            player.setAlive(false);
                        }
                    } else {
                        player.setAlive(false);
                        logger.serverLog("Player [" + player.getId() + "] seems dormant.");
                    }
                }
            });
        }

        /**
         * Attempt to check and promote a peer as backup.
         *
         * @return true if promoted
         */
        protected boolean promotePeerAsBackupIfNeeded(IPeer peer) {
            if (promoteNewBackupServer) {
                synchronized (gameStateLock) {
                    if (promoteNewBackupServer) {
                        promoteNewBackupServer = false;
                        gameState.getServerConfig().setBackupServer(peer);
                        return true;
                    }
                }
            }

            return false;
        }

        public IReply.PingReply callPrimaryPing(IPeer peer, String playerId, String authCode) {
            Player player = authenticatePlayer(peer, playerId, authCode);
            if (player == null) {
                logger.serverLog("Receive illegal ping from player ID: " + playerId + " due to invalid ID or auth code.");
                return IReply.PingReply.createUpdate(gameState);
            }

            updatePeerAlive(peer);

            boolean promoted = promotePeerAsBackupIfNeeded(peer);
            return promoted ? IReply.PingReply.createPromoteToBackup(gameState, serverSecrets) :
                    IReply.PingReply.createUpdate(gameState);
        }


    }

    /**
     * This class represents a primary server that creates/bootstraps the game
     * initially.
     */
    private class BootstrappingPrimaryServer extends PrimaryServer {

        private final AtomicLong nextPlayerIds = new AtomicLong(0);

        private final int treasureCount;

        /**
         * Creating the server, supplying the board size and treasure count from command-line arguments.
         */
        public BootstrappingPrimaryServer(int boardSize, int treasureCount) {
            this.gameState = new GameState(boardSize);
            this.treasureCount = treasureCount;
        }

        public void start() {
            startAccepting();
        }

        public void startAccepting() {
            gameState.setRunningState(RunningState.ACCEPTING_PLAYERS);
            gameState.getServerConfig().setPrimaryServer(self);

            logger.serverLog("Start accepting connections for 20 seconds");

            final Timer timer = new Timer();
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    startGame();

                    timer.cancel();
                    timer.purge();
                }
            };

            Calendar c = Calendar.getInstance();
            c.add(Calendar.SECOND, 20);
            timer.schedule(task, c.getTime());
        }

        @Override
        public IReply.JoinReply callPrimaryJoin(IPeer peer) {
            synchronized (gameStateLock) {
                if (gameState.getRunningState() == RunningState.ACCEPTING_PLAYERS) {
                    // generate player ID and auth code for this player
                    String playerId = getNextPlayerId();
                    String authCode = getNextAuthCode();

                    Player player = new Player(playerId, 0, 0, 0);
                    gameState.getPlayerList().add(player);

                    // update the server secrets
                    serverSecrets.getAuthCodes().put(playerId, authCode);
                    serverSecrets.getPeers().put(playerId, peer);

                    boolean becomeBackup = gameState.getPlayerList().size() == 2;
                    if (becomeBackup) {
                        logger.serverLog("Adding player [" + playerId + "] and designating it as Backup Server");
                        gameState.getServerConfig().setBackupServer(peer);
                        return IReply.JoinReply.createApproveAsBackupReply(playerId, authCode);
                    } else {
                        return IReply.JoinReply.createApproveAsNormalReply(playerId, authCode);
                    }
                } else {
                    return IReply.JoinReply.createDeclineReply();
                }
            }
        }

        private void startGame() {
            synchronized (gameStateLock) {
                logger.serverLog("Game is started. No longer accepting players.");
                gameState.initRandom(treasureCount);
                gameState.setRunningState(RunningState.GAME_STARTED);

                updateBackup();

                // signalling all peers
                Map<String, IPeer> peerMap = serverSecrets.getPeers();
                for (Player player : gameState.getPlayerList()) {
                    try {
                        IPeer peer = peerMap.get(player.getId());
                        if (peer == null) {
                            throw new Error("IPeer was not saved for this ID: " + player.getId());
                        }

                        peer.callClientGameStarted(gameState);
                        updatePeerAlive(peer);

                    } catch (RemoteException e) {
                        // peer has died
                        logger.serverLog("Failed to signal player [" + player.getId() + "]");
                        player.setAlive(false);
                    }
                }

                logger.serverLog("Finished signalling all peers");
            }

            startPulseChecking();
        }

        protected final String getNextPlayerId() {
            return "Player-" + nextPlayerIds.incrementAndGet();
        }

        protected final String getNextAuthCode() {
            return "AUTH-" + UUID.randomUUID().toString();
        }

    }

    /**
     * This class represents a primary server that just come alive after
     * another primary server has died.
     */
    private class InheritingPrimaryServer extends PrimaryServer {

        public InheritingPrimaryServer(GameState gameState, ServerSecrets serverSecrets) {
            this.gameState = gameState;
            this.serverSecrets = serverSecrets;
        }

        public void start() {
            startPulseChecking();
        }

        @Override
        public IReply.JoinReply callPrimaryJoin(IPeer peer) {
            // No longer accepts joining
            return IReply.JoinReply.createDeclineReply();
        }

    }

    /**
     * This class represents a placeholder for a peer that is not a
     * primary server. No method should be called on this class, and thus
     * any method call will throw an error.
     */
    private class NilPrimaryServer extends PrimaryServer {

        @Override
        public IReply.JoinReply callPrimaryJoin(IPeer peer) {
            throw new IllegalStateException("Invalid Method Call");
        }

        @Override
        public IReply.MoveReply callPrimaryMove(IPeer peer,
                                                Move.Direction direction,
                                                String playerId,
                                                String authCode) {

            throw new IllegalStateException("Invalid Method Call");
        }

        @Override
        public IReply.PingReply callPrimaryPing(IPeer peer,
                                                String playerId,
                                                String authCode) {

            throw new IllegalStateException("Invalid Method Call");
        }
    }


    private abstract class BackupServer {

        protected GameState gameState;

        protected ServerSecrets serverSecrets;

        protected final Object gameStateLock = new Object();

        public void callBackupUpdate(GameState gameState, ServerSecrets serverSecrets) {
            logger.serverLog("Receive update from Primary Server");
            synchronized (gameStateLock) {
                this.gameState = gameState;
                this.serverSecrets = serverSecrets;
            }
        }

        /**
         * Obtain and verify the player object from player ID and auth code
         */
        protected Player authenticatePlayer(IPeer peer, String playerId, String authCode) {
            Player player = gameState.searchById(playerId);

            if (player == null || !serverSecrets.auth(peer, playerId, authCode)) {
                // not found
                return null;
            }

            return player;
        }

        public IReply.PingReply callBackupOnPrimaryDied(IPeer peer,
                                                        String playerId,
                                                        String authCode,
                                                        IPeer deadPrimary) {

            Player player = authenticatePlayer(peer, playerId, authCode);
            if (player == null) {
                logger.serverLog("Receive PrimaryDied notification from invalid peer, ID : " + playerId);
                return IReply.PingReply.createUpdate(gameState);
            }

            if (deadPrimary.equals(gameState.getServerConfig().getPrimaryServer())) {
                synchronized (gameStateLock) {
                    IPeer primaryServer = gameState.getServerConfig().getPrimaryServer();
                    if (deadPrimary.equals(primaryServer)) {
                        // ping primary to see if it really died.
                        try {
                            primaryServer.callPrimaryPing(self, gameClient.getPlayerId(), gameClient.getAuthCode());
                        } catch (RemoteException e) {
                            if (!peer.equals(self)) {
                                this.gameState.getServerConfig().setPrimaryServer(peer);
                                return IReply.PingReply.createPromoteToPrimary(gameState, serverSecrets);
                            }
                        }
                    }
                }
            }

            return IReply.PingReply.createUpdate(gameState);
        }
    }

    /**
     * Represents a backup server that is running
     */
    private class ActiveBackupServer extends BackupServer {

        public ActiveBackupServer() {

        }

        public ActiveBackupServer(GameState gameState, ServerSecrets serverSecrets) {
            this.gameState = gameState;
            this.serverSecrets = serverSecrets;
        }

    }

    /**
     * Represents a backup server as a placeholder for a peer that is not functioning as a backup server
     */
    private class NilBackupServer extends BackupServer {

        @Override
        public void callBackupUpdate(GameState gameState, ServerSecrets serverSecrets) {
            throw new IllegalStateException("Invalid Method Call");
        }

        @Override
        public IReply.PingReply callBackupOnPrimaryDied(IPeer peer, String playerId, String authCode, IPeer deadPrimary) {
            throw new IllegalStateException("Invalid Method Call");
        }
    }
}
