package com.tc.p2p;

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

    public PrimaryServer(Peer owner) {
        this.owner = owner;
        initGameState();
    }

    private void initGameState() {
        gameState.getServerConfig().setPrimaryServer(owner);
        gameState.setRunningState(RunningState.ACCEPTING_PLAYERS);
        gameState.getPeerList().clear();
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

            // initiate the game board + treasures + assign IDs
            // add those to the game state
            // call gameStarted() on all the peers with the game state
        }
    }


    public Reply join(Peer peer) throws RemoteException {
        synchronized (gameStateLock) {
            if (gameState.getRunningState() == RunningState.ACCEPTING_PLAYERS) {
                System.out.println("Adding player!!!");
                gameState.getPeerList().add(peer);
                boolean becomeBackup = gameState.getPeerList().size() == 2;
                if (becomeBackup) {
                    gameState.getServerConfig().setBackupServer(peer);
                    return new Reply.JoinAsBackup(getNextPlayerId(), gameState);
                } else {
                    return new Reply.JoinSucceeded(getNextPlayerId());
                }
            } else {
                return new Reply.JoinDeclined("No longer accepting player");
            }
        }
    }

    public Reply move(Peer peer) throws RemoteException {
        Peer backupServer = gameState.getServerConfig().getBackupServer();
        if (backupServer == null) {
            throw new Error("Backup Server is unavailable");
        }

        try {
            backupServer.callBackupUpdate(gameState);
        } catch (RemoteException e) {
            // backup is down

            // make the peer the new backup server
            gameState.getServerConfig().setBackupServer(peer);
            return new Reply.MoveReply(PROMOTED_TO_BACKUP, gameState);
        }

        return new Reply.MoveReply(NONE, gameState);
    }

    public void ping() throws RemoteException {
    }

    private String getNextPlayerId() {
        return "Player-" + nextIds.incrementAndGet();
    }
}
