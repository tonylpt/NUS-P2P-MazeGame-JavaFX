import com.tc.model.Treasure;
import com.tc.p2p.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tc.p2p.Reply.MoveReply.PromotionStatus.*;

/**
 * @author lpthanh
 */
public class P2PGame {

    public static final String NAME_PRIMARY_SERVER = "YUMMY_KING";

    public static final String NAME_BACKUP_SERVER = "YUMMY_QUEEN";


    private final RMIServer rmiServer;

    private final ILogger logger;

    public P2PGame(GameParams params, ILogger logger) {
        this.logger = logger;
        this.rmiServer = createServer(params.isPrimary(), params.getHostPort().getPort(), logger);

        if (this.rmiServer == null) {
            throw new RuntimeException("Unable to create Peer object");
        }
    }

    /**
     * @return null if error
     */
    private static RMIServer createServer(boolean primary, int port, ILogger logger) {
        Registry registry = null;
        int listenPort = 0;

        if (primary) {
            // try to create registry on the port

            try {
                listenPort = port;
                logger.serverLog("Attempting to listen on port " + listenPort);
                registry = LocateRegistry.createRegistry(listenPort);
                logger.serverLog("Server registry was successfully created on port " + listenPort);
            } catch (RemoteException e) {
                throw new RuntimeException("Cannot create Primary peer", e);
            }

        } else {

            // try to find an available port to create the registry on
            Random randomizer = new Random();
            boolean retry = false;
            do {
                try {
                    listenPort = port + randomizer.nextInt(1000);
                    logger.clientLog("Attempting to bind a client on port " + listenPort);
                    registry = LocateRegistry.createRegistry(listenPort);
                    logger.clientLog("Client registry was successfully created on port " + listenPort);
                } catch (RemoteException e) {
                    // the exception may have been caused by the port's unavailability
                    retry = true;
                }
            } while (retry);
        }

        return new RMIServer(registry, listenPort);
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

    private class GameClient extends UnicastRemoteObject implements IPeer.IClient {

        public GameClient() throws RemoteException {

        }

        @Override
        public IReply callClientGameStarted(String playerID, GameState gameState) throws RemoteException {
            return null;
        }

    }

    public class PrimaryServer {

        private final Peer owner;

        private GameState gameState = new GameState();

        private final Object gameStateLock = new Object();

        private final AtomicInteger nextIds = new AtomicInteger(0);

        private final Timer timer = new Timer("primary-server");

        public PrimaryServer(Peer owner, int N, int M) {
            this.owner = owner;
            initGameState(N, M); //just set server here, gamestate not initialized.
        }

        //for change of primary server.
        public PrimaryServer(Peer owner, GameState gamestate) {
            this.owner = owner;
            //TODO deep update gamestate to latest from backup;
            this.gameState = gamestate;
            this.gameState.getServerConfig().setPrimaryServer(gamestate.getServerConfig().getPrimaryServer());
            this.gameState.getServerConfig().setBackupServer(gamestate.getServerConfig().getBackupServer());

        }

        private void initGameState(int N, int M) {
            gameState.getServerConfig().setPrimaryServer(owner);
            gameState.setBoardSizeN(N);
            gameState.setTreasureCountM(M);
            gameState.setRunningState(RunningState.ACCEPTING_PLAYERS);
        }

        public void startInitialTimer() {
            final Timer timer = new Timer();
            final TimerTask task = new TimerTask() {

                @Override
                public void run() {
                    System.out.println("20secs passed, Game START ");
                    startGame();

                    timer.cancel();
                    timer.purge();
                }
            };

            Calendar c = Calendar.getInstance();
            c.add(Calendar.SECOND, 20);
            timer.schedule(task, c.getTime());
        }


        private void startGame() {
            synchronized (gameStateLock) {
                gameState.setRunningState(RunningState.GAME_STARTED);

                // TODO initiate the game board + treasures + assign IDs
                // add those to the game state
                // call gameStarted() on all the peers with the game state

                gameState.initialize();

                try {
                    gameState.getServerConfig().getBackupServer().callBackupUpdate(gameState);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                for (Player player : gameState.getPlayerList()) {
                    if (!player.isAlive()) {
                        try {
                            player.getPeer().callClientGameStarted(player.getPlayerID(), gameState);
                        } catch (RemoteException e) {
                            // peer has died
                            player.setAlive(false);
                        }
                    }
                }
            }
        }


        public Reply join(Peer peer) throws RemoteException {
            synchronized (gameStateLock) {
                if (gameState.getRunningState() == RunningState.ACCEPTING_PLAYERS) {
                    System.out.println("Adding player!!!");
                    String playerId = getNextPlayerId();
                    Player player = new Player(playerId, 0, 0, 0, peer);
                    gameState.getPlayerList().add(player);
                    boolean becomeBackup = gameState.getPlayerList().size() == 2;
                    if (becomeBackup) {
                        gameState.getServerConfig().setBackupServer(peer);
                        return new Reply.JoinAsBackup(playerId, gameState);
                    } else {
                        return new Reply.JoinSucceeded(playerId);
                    }
                } else {
                    return new Reply.JoinDeclined("No longer accepting player");
                }
            }
        }

        private void getTreasures(Player player) {
            List<Treasure> treasureList = gameState.getTreasureList();
            for (Treasure oneTreasure : treasureList) {
                if (oneTreasure.getAssignedPlayerID() == null &&
                        oneTreasure.getCordx() == player.getCordx() &&
                        oneTreasure.getCordy() == player.getCordy()) {
                    oneTreasure.setAssignedPlayerID(player.getPlayerID());
                    player.setTreasureCount(player.getTreasureCount() + 1);
                }
            }
        }

        public Reply move(Peer peer, char direction, String playerID) throws RemoteException {
            Peer backupServer = gameState.getServerConfig().getBackupServer();
            boolean illegalMove = false;
            if (backupServer == null) {
                throw new Error("Backup Server is unavailable");
            }

            System.out.println("Player is making a move!");


            boolean gameEnded = true;
            for (Treasure oneTreasure : gameState.getTreasureList()) {
                if (oneTreasure.getAssignedPlayerID() == null) {
                    gameEnded = false;
                }
            }
            if (gameEnded) {
                gameState.setRunningState(RunningState.GAME_ENDED);
                return new Reply.MoveReply(NONE, gameState, illegalMove);
            }


            Player player = null;
            for (Player onePlayer : gameState.getPlayerList()) {
                if (playerID.equals(onePlayer.getPlayerID())) {
                    player = onePlayer;
                    break;
                }
            }

            if (player == null) {
            } else {//player ID found
                //update gamestate;
                switch (direction) {
                    case 'N':
                        if (player.getCordy() + 1 >= gameState.getBoardSizeN()) {//check for illegal move
                            illegalMove = true;
                            break;
                        }
                        for (Player onePlayer : gameState.getPlayerList()) {
                            if (onePlayer.getCordx() == player.getCordx() &&
                                    onePlayer.getCordy() == player.getCordy() + 1) {
                                illegalMove = true;
                                break;
                            }
                        }
                        if (illegalMove == false) {
                            player.setCordy(player.getCordy() + 1);
                            getTreasures(player);
                        }
                        break;
                    case 'S':
                        if (player.getCordy() - 1 < 0) {//check for illegal move
                            illegalMove = true;
                            break;
                        }
                        for (Player onePlayer : gameState.getPlayerList()) {
                            if (onePlayer.getCordx() == player.getCordx() &&
                                    onePlayer.getCordy() == player.getCordy() - 1) {
                                illegalMove = true;
                                break;
                            }
                        }
                        if (illegalMove == false) {
                            player.setCordy(player.getCordy() - 1);
                            getTreasures(player);
                        }
                        break;
                    case 'E':
                        if (player.getCordx() + 1 >= gameState.getBoardSizeN()) {//check for illegal move
                            illegalMove = true;
                            break;
                        }
                        for (Player onePlayer : gameState.getPlayerList()) {
                            if (onePlayer.getCordx() == player.getCordx() + 1 &&
                                    onePlayer.getCordy() == player.getCordy()) {
                                illegalMove = true;
                                break;
                            }
                        }
                        if (illegalMove == false) {
                            player.setCordx(player.getCordx() + 1);
                            getTreasures(player);
                        }
                        break;
                    case 'W':
                        if (player.getCordx() - 1 < 0) {//check for illegal move
                            illegalMove = true;
                            break;
                        }
                        for (Player onePlayer : gameState.getPlayerList()) {
                            if (onePlayer.getCordx() == player.getCordx() - 1 &&
                                    onePlayer.getCordy() == player.getCordy()) {
                                illegalMove = true;
                                break;
                            }
                        }
                        if (illegalMove == false) {
                            player.setCordx(player.getCordx() - 1);
                            getTreasures(player);
                        }
                        break;
                    case 'O': //no move
                        break;
                    default:// illegal move
                        illegalMove = true;
                        break;
                }
            }
            //TODO update self?...

            try {
                backupServer.callBackupUpdate(gameState);
            } catch (RemoteException e) {
                // backup is down
                System.out.println("Backup is down!!");


                // check if peer is not the primary server!!
                Peer primaryServer = gameState.getServerConfig().getPrimaryServer();
                if (peer.hashCode() == primaryServer.hashCode()) {
                    System.out.println("This is the primary server moving!");
                    return new Reply.MoveReply(NONE, gameState, illegalMove);
                } else {
                    gameState.getServerConfig().setBackupServer(peer);
                    return new Reply.MoveReply(PROMOTED_TO_BACKUP, gameState, illegalMove);
                }

            }

            return new Reply.MoveReply(NONE, gameState, illegalMove);
        }

        public Reply ping() throws RemoteException {
            return new Reply.PingReply(this.gameState);
        }

        private String getNextPlayerId() {
            return "Player-" + nextIds.incrementAndGet();
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
    }

}
