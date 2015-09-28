package com.tc.p2p;

import com.tc.Gamestate;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tc.p2p.Reply.MoveReply.PromotionStatus.NONE;
import static com.tc.p2p.Reply.MoveReply.PromotionStatus.PROMOTED_TO_BACKUP;

/**
 * @author lpthanh
 */
public class PrimaryServer {

    private final Peer owner;

    private final GameState gameState = new GameState();

    private final Object gameStateLock = new Object();

    private final AtomicInteger nextIds = new AtomicInteger(0);

    private final Timer timer = new Timer("primary-server");

    private int N;

    private int M;

    public PrimaryServer(Peer owner, int N, int M) {
        this.owner = owner;
        this.N = N;
        this.M = M;
        initGameState(); //just set server here, gamestate not initialized.
    }

    //for change of primary server.
    public PrimaryServer(Peer owner, GameState gamestate) {
        this.owner = owner;
        //TODO deep update gamestate to latest from backup;
        this.gameState.getServerConfig().setPrimaryServer(gamestate.getServerConfig().getPrimaryServer());
        this.gameState.getServerConfig().setBackupServer(gamestate.getServerConfig().getBackupServer());
//        this.gameState = gameState;
    }

    private void initGameState() {
        gameState.getServerConfig().setPrimaryServer(owner);
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

            gameState.initialize(N,M);

            for (Player player : gameState.getPlayerList()) {
                if (!player.isAlive()) {
                    try {
                        player.getPeer().callClientGameStarted(player.getPlayerID(),gameState);
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

    public Reply move(Peer peer, char direction) throws RemoteException {
        Peer backupServer = gameState.getServerConfig().getBackupServer();
        if (backupServer == null) {
            throw new Error("Backup Server is unavailable");
        }

        System.out.println("Player is making a move!");

        try {
            backupServer.callBackupUpdate(gameState);
        } catch (RemoteException e) {
            // backup is down
            System.out.println("Backup is down!!");

            // make the peer the new backup server


            /// TODO check if peer is not the primary server!!
            Peer primaryServer = gameState.getServerConfig().getPrimaryServer();
            if(peer.hashCode() == primaryServer.hashCode()){
                System.out.println("This is the primary server moving!");
                return new Reply.MoveReply(NONE, gameState);
            }
            else{
                gameState.getServerConfig().setBackupServer(peer);
                return new Reply.MoveReply(PROMOTED_TO_BACKUP, gameState);
            }

        }

        return new Reply.MoveReply(NONE, gameState);
    }

//    public void ping() throws RemoteException {
//        System.out.println("ping comes to server!");
//    }

    private String getNextPlayerId() {
        return "Player-" + nextIds.incrementAndGet();
    }
}
