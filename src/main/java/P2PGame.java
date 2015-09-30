import com.tc.p2p.Peer;
import com.tc.p2p.Reply;
import javafx.application.Platform;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static IReply.PingReply.PromotionStatus.*;

/**
 * @author lpthanh
 */
public class P2PGame extends UnicastRemoteObject implements IPeer {

    public static final String NAME_PEER = "FRIENDLY_PEER";

    private final RMIServer rmiServer;

    private final ILogger logger;

    private final GameUI.UIController uiController;

    private final IPeer self;

    private final GameClient gameClient = new GameClient();

    private PrimaryServer primaryServer = new NilPrimaryServer();

    private BackupServer backupServer = new NilBackupServer();


    public P2PGame(GameParams params, ILogger logger, GameUI.UIController uiController) throws RemoteException {
        this.self = this;
        this.logger = logger;
        this.uiController = uiController;
        this.rmiServer = createServer(params.isPrimary(), params.getHostPort().getPort(), logger);

        if (this.rmiServer == null) {
            throw new RuntimeException("Unable to create Peer object.");
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

    private void initialConnectToPrimary(String host, int port) {
        try {
            logger.clientLog("Connecting to Primary Server at " + host + ":" + port);
            Registry registry = LocateRegistry.getRegistry(host, port);
            IPeer primaryServer = (IPeer) registry.lookup(NAME_PEER);
            IReply.JoinReply reply = primaryServer.callPrimaryJoin(this);

            if (!reply.isAccepted()) {
                logger.clientLog("Join request was rejected by Primary Server.");
                return;
            }

            logger.clientLog("Player has joined the game, with ID=" + reply.getPlayerId());
            if (reply.shouldBecomeBackup()) {
                logger.clientLog("Player was promoted to be the Backup Server.");
            }

            if (reply instanceof Reply.JoinDeclined) {
                Reply.JoinDeclined joinReply = (Reply.JoinDeclined) reply;
                System.out.println("Join Failed. Reason: " + joinReply.reason);
            } else if (reply instanceof Reply.JoinSucceeded) {
                Reply.JoinSucceeded joinReply = (Reply.JoinSucceeded) reply;
                System.out.println("Join success. Player ID = " + joinReply.getPlayerId());
            } else if (reply instanceof Reply.JoinAsBackup) {
                Reply.JoinAsBackup joinReply = (Reply.JoinAsBackup) reply;
                System.out.println("Join success. Should become backup. Player ID = " + joinReply.getPlayerId());
                joinAsBackup(joinReply.getGameState());
            } else {
                throw new RuntimeException("Unrecognized response: " + reply.getClass());
            }

        } catch (RemoteException | NotBoundException e) {
            System.err.println("Unable to connect to the Primary Server");
            e.printStackTrace();
            System.exit(0);
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
    public IReply.MoveReply callPrimaryMove(IPeer peer, char direction, String playerId, String authCode) throws RemoteException {
        return primaryServer.callPrimaryMove(peer, direction, playerId, authCode);
    }

    /**
     * Run by Primary Server
     */
    @Override
    public IReply.PingReply callPrimaryPing(IPeer peer, String playerId, String authCode) throws RemoteException {
        return primaryServer.callPrimaryPing(peer, playerId, authCode);
    }

    /**
     * Run by Backup Server
     */
    @Override
    public IReply callBackupUpdate(GameState gameState) throws RemoteException {
        return null;
    }

    /**
     * Run by Backup Server
     */
    @Override
    public IReply.PingReply callBackupPing() throws RemoteException {
        return backupServer.callBackupPing();
    }

    /**
     * Run by Backup Server
     */
    @Override
    public IReply callBackupOnPrimaryDied(IPeer peer, String playerId, String authCode) throws RemoteException {
        return null;
    }

    private class GameClient {

        public void callClientGameStarted(GameState gameState) {
            Platform.runLater(() -> {
                uiController.onGameStarted(gameState);
            });
        }

    }

    /**
     * This parent class contains the basic implementation for a primary server's logic.
     */
    private abstract class PrimaryServer {

        protected GameState gameState;

        protected ServerSecrets serverSecrets;

        protected final Object gameStateLock = new Object();

        protected Map<String, Long> playerLastAccessMillis = new HashMap<>();

        protected boolean promoteNewBackupServer;

        protected PrimaryServer() {

        }

        public abstract IReply.JoinReply callPrimaryJoin(IPeer peer);

        /**
         * Obtain and verify the player object from player ID and auth code
         */
        private Player authenticatePlayer(IPeer peer, String playerId, String authCode) {
            Player player = null;

            // search for player from ID
            for (Player onePlayer : gameState.getPlayerList()) {
                if (playerId.equals(onePlayer.getId())) {
                    player = onePlayer;
                    break;
                }
            }

            if (player == null) {
                // not found
                return null;
            }

            // verify auth codes
            String correctAuthCode = serverSecrets.getAuthCodes().get(playerId);
            if (correctAuthCode == null || !correctAuthCode.equals(authCode)) {
                // incorrect code
                return null;
            }

            // verify remote object
            IPeer savedPeer = serverSecrets.getPeers().get(playerId);
            if (savedPeer == null || peer == null || savedPeer.hashCode() != peer.hashCode()) {
                return null;
            }

            return player;
        }

        public IReply.MoveReply callPrimaryMove(IPeer peer, char direction, String playerId, String authCode) {
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
                updatePlayerAlive(player);
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

                boolean illegalMove = false;

                switch (direction) {
                    case 'N': {
                        if (player.getPosY() + 1 >= gameState.getBoardSize()) {
                            // check for illegal move
                            illegalMove = true;
                            break;
                        }

                        for (Player onePlayer : gameState.getPlayerList()) {
                            if (onePlayer.getPosX() == player.getPosX() &&
                                    onePlayer.getPosY() == player.getPosY() + 1) {
                                // crashing
                                illegalMove = true;
                                break;
                            }
                        }

                        if (!illegalMove) {
                            player.setPosY(player.getPosY() + 1);
                            getTreasures(player);
                        }

                        break;
                    }

                    case 'S': {
                        if (player.getPosY() - 1 < 0) {
                            // check for illegal move
                            illegalMove = true;
                            break;
                        }

                        for (Player onePlayer : gameState.getPlayerList()) {
                            if (onePlayer.getPosX() == player.getPosX() &&
                                    onePlayer.getPosY() == player.getPosY() - 1) {
                                // crashing
                                illegalMove = true;
                                break;
                            }
                        }

                        if (!illegalMove) {
                            player.setPosY(player.getPosY() - 1);
                            getTreasures(player);
                        }
                        break;
                    }

                    case 'E': {
                        if (player.getPosX() + 1 >= gameState.getBoardSize()) {
                            // check for illegal move
                            illegalMove = true;
                            break;
                        }

                        for (Player onePlayer : gameState.getPlayerList()) {
                            if (onePlayer.getPosX() == player.getPosX() + 1 &&
                                    onePlayer.getPosY() == player.getPosY()) {
                                // crashing
                                illegalMove = true;
                                break;
                            }
                        }

                        if (!illegalMove) {
                            player.setPosX(player.getPosX() + 1);
                            getTreasures(player);
                        }

                        break;
                    }

                    case 'W': {
                        if (player.getPosX() - 1 < 0) {//check for illegal move
                            illegalMove = true;
                            break;
                        }
                        for (Player onePlayer : gameState.getPlayerList()) {
                            if (onePlayer.getPosX() == player.getPosX() - 1 &&
                                    onePlayer.getPosY() == player.getPosY()) {
                                illegalMove = true;
                                break;
                            }
                        }
                        if (!illegalMove) {
                            player.setPosX(player.getPosX() - 1);
                            getTreasures(player);
                        }
                        break;
                    }

                    case 'O':
                        //no move
                        break;

                    default:// illegal move
                        illegalMove = true;
                        break;

                }

                try {
                    backupServer.callBackupUpdate(gameState, serverSecrets);
                } catch (RemoteException e) {
                    // backup is down
                    logger.serverLog("Backup Server is down");

                    // check if peer is not the primary server!!
                    IPeer primaryServer = gameState.getServerConfig().getPrimaryServer();
                    if (peer.hashCode() == primaryServer.hashCode()) {
                        logger.serverLog("Waiting for the next peer to become the new Backup");
                        return IReply.MoveReply.createReply(gameState, illegalMove);
                    } else {
                        gameState.getServerConfig().setBackupServer(peer);
                        return new IReply.MoveReply(PROMOTED_TO_BACKUP, gameState, serverSecrets, illegalMove);
                    }

                }

                return IReply.MoveReply.createReply(gameState, illegalMove);
            }
        }

        protected final void getTreasures(Player player) {
            synchronized (gameStateLock) {
                List<Treasure> treasureList = gameState.getTreasureList();

                // search for the treasure
                for (Treasure oneTreasure : treasureList) {
                    if (oneTreasure.getAssignedPlayerId() == null &&
                            oneTreasure.getPosX() == player.getPosX() &&
                            oneTreasure.getPosY() == player.getPosY()) {
                        oneTreasure.setAssignedPlayerId(player.getId());
                        player.setTreasureCount(player.getTreasureCount() + 1);
                        break;
                    }
                }
            }
        }

        protected void updateBackup() {
            synchronized (gameStateLock) {
                IPeer backupServer = gameState.getServerConfig().getBackupServer();
                try {
                    backupServer.callBackupUpdate(gameState, serverSecrets);
                } catch (RemoteException e) {
                    // backup died
                    logger.serverLog("Backup Server cannot be reached");
                    promoteNewBackupServer = true;
                }
            }
        }

        /**
         * Called when receive a request from player, so know that it's alive.
         */
        protected void updatePlayerAlive(Player player) {
            playerLastAccessMillis.put(player.getId(), System.currentTimeMillis());
        }

        public IReply.PingReply callPrimaryPing(IPeer peer, String playerId, String authCode) {
            Player player = authenticatePlayer(peer, playerId, authCode);
            if (player == null) {
                logger.serverLog("Receive illegal ping from player ID: " + playerId + " due to invalid ID or auth code.");
                return IReply.PingReply.createUpdate(gameState);
            }

            updatePlayerAlive(player);

            synchronized (gameStateLock) {

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

        /**
         * Creating the server, supplying the board size and treasure count from command-line arguments.
         */
        public BootstrappingPrimaryServer(int boardSize, int treasureCount) {
            this.gameState = new GameState(boardSize);
            this.treasureCount = treasureCount;
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
                        playerLastAccessMillis.put(player.getId(), System.currentTimeMillis());

                    } catch (RemoteException e) {
                        // peer has died
                        logger.serverLog("Failed to signal player [" + player.getId() + "]");
                        player.setAlive(false);
                    }
                }

                logger.serverLog("Finished signalling all peers");
            }

            for (Player player : gameState.getPlayerList()) {
                playerLastAccessMillis.put(player.getId(), System.currentTimeMillis());
            }
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
        public IReply.MoveReply callPrimaryMove(IPeer peer, char direction, String playerId, String authCode) {
            throw new IllegalStateException("Invalid Method Call");
        }

        @Override
        public IReply.PingReply callPrimaryPing(IPeer peer, String playerId, String authCode) {
            throw new IllegalStateException("Invalid Method Call");
        }
    }


    public class BackupServer {

        private final Peer owner;

        private GameState gameState;

        private final Object gameStateLock = new Object();

        public BackupServer(Peer owner, GameState gameState) {
            this.owner = owner;
            this.gameState = gameState;
        }


        public Reply update(GameState gameState) {
            // deep - copy from game state
            System.out.println("backup gamestate updated!");
            this.gameState = gameState;
            return new Reply.UpdateReply(this.gameState);
        }

        public Reply primaryDied(Peer peer) throws RemoteException {

            Peer backupServer = gameState.getServerConfig().getBackupServer();
            Peer primaryServer = gameState.getServerConfig().getPrimaryServer();
            //ping primary to see if really died.
            try {
                primaryServer.ping();
            } catch (RemoteException e) {
                //really died
                if (peer.hashCode() == backupServer.hashCode()) {
                    System.out.println("This is the backup server moving, no primary server to process...");
                    //TODO if back server moving, how? no move??
                    return new Reply.MoveReply(IS_BACKUP, gameState, false);
                } else {
                    this.gameState.getServerConfig().setPrimaryServer(peer);
                    return new Reply.MoveReply(PROMOTED_TO_PRIMARY, gameState, false);
                }
            }
            //never die
            return new Reply.MoveReply(NONE, gameState, false);
        }

        public Reply ping() throws RemoteException {
            return new Reply.PingReply(this.gameState);
        }

        public IReply.PingReply callBackupPing() {
            return null;
        }
    }

}
