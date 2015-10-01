import javafx.application.Platform;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


public class P2PGame extends UnicastRemoteObject implements IPeer {

    public static final long PING_INTERVAL = 2000;

    public static final String NAME_PEER = "FRIENDLY_PEER";

    private RMIServer rmiServer;

    private final ILogger logger;

    private final GameUI.UIController uiController;

    private final IPeer self;

    private final GameClient gameClient = new GameClient();

    private PrimaryServer primaryServer = new NilPrimaryServer();

    private BackupServer backupServer = new NilBackupServer();

    private final ExecutorService exec = Executors.newCachedThreadPool(runnable -> {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setDaemon(true);
        return thread;
    });

    public P2PGame(GameParams params, ILogger logger, GameUI.UIController uiController) throws RemoteException {
        this.self = this;
        this.logger = logger;
        this.uiController = uiController;
        start(params);
    }

    private void start(GameParams params) {
        if (params instanceof GameParams.PrimaryParams) {
            GameParams.PrimaryParams primaryParams = (GameParams.PrimaryParams) params;
            exec.execute(() -> {
                try {
                    GameParams.HostPort hostPort = params.getHostPort();
                    P2PGame.this.rmiServer = createServer(this, true, hostPort.getPort(), logger);
                    P2PGame.this.primaryServer = new BootstrappingPrimaryServer(
                            primaryParams.getBoardSize(), primaryParams.getTreasureCount()
                    );
                    P2PGame.this.primaryServer.start();
                    P2PGame.this.gameClient.connectToPrimary(hostPort.getHost(), hostPort.getPort());
                } catch (Exception e) {
                    logger.serverLogError("Unable to bind server.", e);
                }

            });
        } else if (params instanceof GameParams.NonPrimaryParams) {
            exec.execute(() -> {
                try {
                    GameParams.HostPort hostPort = params.getHostPort();
                    P2PGame.this.rmiServer = createServer(this, false, hostPort.getPort(), logger);
                    P2PGame.this.gameClient.connectToPrimary(hostPort.getHost(), hostPort.getPort());
                } catch (Exception e) {
                    logger.serverLogError("Unable to bind server.", e);
                }
            });
        }
    }

    /**
     * Try to binds to a port, for both primary server and non-server players.
     * This is because each player can potentially become a server.
     */
    private static RMIServer createServer(IPeer peer, boolean primary, int port, ILogger logger)
            throws RemoteException, AlreadyBoundException {

        Registry registry = null;
        int listeningPort = 0;

        if (primary) {
            // try to create registry on the port

            listeningPort = port;
            logger.serverLog("Attempting to listen on port " + listeningPort);
            registry = LocateRegistry.createRegistry(port);
            registry.bind(NAME_PEER, peer);
            logger.serverLog("Server registry was successfully created on port " + listeningPort);

        } else {

            // try to find an available port to create the registry on
            Random randomizer = new Random();
            boolean retry = false;
            int retryLeft = 10;

            do {
                try {
                    listeningPort = port + randomizer.nextInt(1000);
                    logger.clientLog("Attempting to bind a client on port " + listeningPort);
                    registry = LocateRegistry.createRegistry(listeningPort);
                    registry.bind(NAME_PEER, peer);
                    logger.clientLog("Client registry was successfully created on port " + listeningPort);
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

        return new RMIServer(registry, listeningPort);
    }

    public GameClient getGameClient() {
        return gameClient;
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

    boolean isSelf(String playerId) {
        return playerId.equals(gameClient.getPlayerId());
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

    class GameClient {

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
            Platform.runLater(() -> uiController.onGameStateUpdated(gameState));
        }

        public void callClientGameStarted(GameState gameState) {
            this.gameState = gameState;
            Platform.runLater(() -> uiController.onGameStarted(gameState));
            startPulseChecking();
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
                this.authCode = reply.getAuthCode();

                logger.clientLog("Player has joined the game, with ID = [" + getPlayerId() + "]");

                if (reply.shouldBecomeBackup()) {
                    logger.clientLog("Player was promoted to be the Backup Server.");
                    backupServer = new ActiveBackupServer();
                }

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
                primaryDown(primaryServer);
            }
        }

        private void primaryDown(IPeer primaryServer) {
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

        private void processPingReply(IReply.PingReply reply) {
            switch (reply.getPromotionStatus()) {
                case PROMOTED_TO_PRIMARY: {
                    setGameState(reply.getGameState());

                    logger.clientLog("Promoted to Primary Server");
                    primaryServer = new InheritingPrimaryServer(reply.getGameState(), reply.getServerSecrets());
                    primaryServer.start();
                    break;
                }
                case PROMOTED_TO_BACKUP: {
                    setGameState(reply.getGameState());

                    logger.clientLog("Promoted to Backup Server");
                    backupServer = new ActiveBackupServer(reply.getGameState(), reply.getServerSecrets());
                    break;
                }
                default: {
                    setGameState(reply.getGameState());
                }
            }
        }

        public void sendMoveAsync(Move.Direction dir) {
            exec.submit(() -> {
                IPeer primaryServer = gameState.getServerConfig().getPrimaryServer();

                try {
                    // contact primary
                    IReply.MoveReply reply = primaryServer.callPrimaryMove(self, dir, playerId, authCode);
                    if (reply.isIllegalMove()) {
                        logger.clientLog("Move was not allowed");
                    }

                    processPingReply(reply);

                } catch (RemoteException e) {
                    primaryDown(primaryServer);
                }
            });
        }
    }

    private abstract class Server {

        protected GameState gameState;

        protected ServerSecrets serverSecrets;

        protected final Object gameStateLock = new Object();

        protected final void setRolePrimary(String playerId) {
            if (gameState == null) {
                return;
            }

            gameState.getPlayerList().forEach(player -> {
                if (player.getId().equals(playerId)) {
                    player.setRole(PeerRole.PRIMARY_SERVER);
                } else if (player.getRole() == PeerRole.PRIMARY_SERVER) {
                    // clear the primary role of all others
                    player.setRole(PeerRole.NON_SERVER);
                }
            });
        }

        protected final void setRoleBackup(String playerId) {
            if (gameState == null) {
                return;
            }

            gameState.getPlayerList().forEach(player -> {
                if (player.getId().equals(playerId)) {
                    player.setRole(PeerRole.BACKUP_SERVER);
                } else if (player.getRole() == PeerRole.BACKUP_SERVER) {
                    // clear the backup role of all others
                    player.setRole(PeerRole.NON_SERVER);
                }
            });
        }

        protected final void setDead(String playerId) {
            for (Player player : gameState.getPlayerList()) {
                if (player.getId().equals(playerId)) {
                    player.setAlive(false);
                    player.setRole(PeerRole.DEAD);
                    break;
                }
            }
        }
    }

    /**
     * This parent class contains the basic implementation for a primary server's logic.
     */
    private abstract class PrimaryServer extends Server {

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

                if (gameState.getRunningState() == RunningState.GAME_ENDED) {
                    return IReply.MoveReply.createIllegal(gameState);
                }


                // check and update game state
                Move move = new Move(direction, playerId);
                boolean illegalMove = gameState.processMove(move, player);

                // check for game ending
                boolean gameEnded = true;
                for (Treasure oneTreasure : gameState.getTreasureList()) {
                    if (oneTreasure.getAssignedPlayerId() == null) {
                        gameEnded = false;
                    }
                }

                if (gameEnded) {
                    logger.serverLog("Game Over");
                    gameState.setRunningState(RunningState.GAME_ENDED);
                }

                if (!updateBackup()) {
                    // failed to update to backup
                    // only try to promote if peer is not the primary server
                    if (!peer.equals(self)) {
                        if (promotePeerAsBackupIfNeeded(playerId, peer)) {
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
                if (!player.isAlive()) {
                    return;
                }

                IPeer peer = serverSecrets.getPeers().get(player.getId());
                Long lastAccessMillis = peerLastAccessMillis.get(peer);
                if (lastAccessMillis == null) {
                    return;
                }

                long silentPeriod = System.currentTimeMillis() - lastAccessMillis;
                if (silentPeriod > 2 * PING_INTERVAL) {
                    if (isSelf(player.getId())) {
                        // peer is primary server. ignoring.
                    } else if (peer.equals(gameState.getServerConfig().getBackupServer())) {
                        logger.serverLog("Backup Server seems dormant. Retrying.");
                        if (!updateBackup()) {
                            setDead(player.getId());
                        }
                    } else {
                        setDead(player.getId());
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
        protected boolean promotePeerAsBackupIfNeeded(String playerId, IPeer peer) {
            if (promoteNewBackupServer) {
                if (peer.equals(self)) {
                    return false;
                }

                synchronized (gameStateLock) {
                    if (promoteNewBackupServer) {
                        promoteNewBackupServer = false;
                        gameState.getServerConfig().setBackup(playerId, peer);
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

            if (isSelf(playerId)) {
                return IReply.PingReply.createUpdate(gameState);
            }

            boolean promoted = promotePeerAsBackupIfNeeded(playerId, peer);
            if (promoted) {
                player.setRole(PeerRole.BACKUP_SERVER);
                logger.serverLog("Promoting [" + playerId + "] as Backup Server");
                return IReply.PingReply.createPromoteToBackup(gameState, serverSecrets);
            } else {
                return IReply.PingReply.createUpdate(gameState);
            }
        }


    }

    /**
     * This class represents a primary server that creates/bootstraps the game
     * initially.
     */
    private class BootstrappingPrimaryServer extends PrimaryServer {

        private final AtomicLong nextPlayerIds = new AtomicLong(0);

        private final int treasureCount;

        private String bootstrappingPlayerId;

        /**
         * Creating the server, supplying the board size and treasure count from command-line arguments.
         */
        public BootstrappingPrimaryServer(int boardSize, int treasureCount) {
            this.gameState = new GameState(boardSize);
            this.serverSecrets = new ServerSecrets();
            this.treasureCount = treasureCount;
        }

        public void start() {
            startAccepting();
        }

        public void startAccepting() {
            bootstrappingPlayerId = getNextPlayerId();

            gameState.setRunningState(RunningState.ACCEPTING_PLAYERS);
            gameState.getServerConfig().setPrimary(bootstrappingPlayerId, self);

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
                    String authCode = getNextAuthCode();

                    Player player = new Player(null, 0, 0, 0, true, PeerRole.NON_SERVER);

                    // If this is the first peer, it's the primary server
                    if (gameState.getPlayerList().isEmpty()) {
                        player.setId(bootstrappingPlayerId);
                        player.setRole(PeerRole.PRIMARY_SERVER);
                    } else {
                        player.setId(getNextPlayerId());
                    }

                    String playerId = player.getId();

                    gameState.getPlayerList().add(player);

                    // update the server secrets
                    serverSecrets.getAuthCodes().put(playerId, authCode);
                    serverSecrets.getPeers().put(playerId, peer);

                    // If this is the second peer, it's the backup server
                    boolean becomeBackup = gameState.getPlayerList().size() == 2;
                    if (becomeBackup) {
                        player.setRole(PeerRole.BACKUP_SERVER);
                        logger.serverLog("Adding player [" + playerId + "] and designating it as Backup Server");
                        gameState.getServerConfig().setBackup(playerId, peer);
                        return IReply.JoinReply.createApproveAsBackupReply(playerId, authCode);
                    } else {
                        logger.serverLog("Adding player [" + playerId + "]");
                        return IReply.JoinReply.createApproveAsNormalReply(playerId, authCode);
                    }
                } else {
                    logger.serverLog("Rejecting a new peer attempting to join");
                    return IReply.JoinReply.createDeclineReply();
                }
            }
        }

        private void startGame() {
            synchronized (gameStateLock) {
                if (gameState.getServerConfig().getBackupServer() == null) {
                    // there was no other joiners
                    logger.serverLogError("There was no other player in the game.", null);
                    return;
                }

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
            logger.serverLog("Starting Primary Server");
            this.gameState = gameState;
            this.serverSecrets = serverSecrets;
        }

        public void start() {
            startPulseChecking();
        }

        @Override
        public IReply.JoinReply callPrimaryJoin(IPeer peer) {
            // No longer accepts joining
            logger.serverLog("Rejecting a new peer attempting to join");
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


    private abstract class BackupServer extends Server {

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
                            setDead(gameState.getServerConfig().getPrimaryPlayerId());
                            if (!isSelf(playerId)) {
                                player.setAlive(true);
                                player.setRole(PeerRole.PRIMARY_SERVER);

                                logger.serverLog("Promoting [" + playerId + "] as new Primary Server");
                                gameState.getServerConfig().setPrimary(playerId, peer);
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
            logger.serverLog("Starting Backup Server");
        }

        public ActiveBackupServer(GameState gameState, ServerSecrets serverSecrets) {
            logger.serverLog("Starting Backup Server");
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
