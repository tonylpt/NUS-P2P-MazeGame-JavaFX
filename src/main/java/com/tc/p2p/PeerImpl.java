package com.tc.p2p;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import javax.management.remote.rmi.RMIServer;
import javax.swing.*;
import java.awt.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static com.tc.p2p.Reply.*;
import static com.tc.p2p.Reply.MoveReply.PromotionStatus.*;

/**
 * @author lpthanh
 */
public class PeerImpl extends UnicastRemoteObject implements Peer {



    private final RMIServer rmiServer;

    private GameState gameState;


    /**
     * Will be null if this peer is not the primary server
     */
    private PrimaryServer primaryServer;

    /**
     * Will be null if this peer is not the backup server
     */
    private BackupServer backupServer;

    public PeerImpl(boolean primary, String host, int port, int N, int M) throws RemoteException {
        this.rmiServer = createServer(this, primary, port);

        if (this.rmiServer == null) {
            throw new RuntimeException("Unable to create Peer object");
        }

        if (primary) {
            // init the waiting / the game state
            initialStartPrimary(N,M);
        }

        // connect to the primary
        initialConnectToPrimary(host, port);
    }

    private void checkPrimary() {
        if (primaryServer == null) {
            throw new Error("Primary Server is unavailable.");
        }
    }

    private void checkBackup() {
        if (backupServer == null) {
            throw new Error("Backup Server is unavailable.");
        }
    }

    /**
     * Performed by Primary server
     */
    @Override
    public Reply callPrimaryJoin(Peer peer) throws RemoteException {
        checkPrimary();
        return primaryServer.join(peer);
    }

    @Override
    public Reply callPrimaryMove(Peer peer, char direction, String playerID) throws RemoteException {
        checkPrimary();
        return primaryServer.move(peer, direction, playerID);
    }

    @Override
    public Reply callPrimaryPing() throws RemoteException {
        checkPrimary();
        return primaryServer.ping();
    }

    @Override
    public Reply callBackupPing() throws RemoteException {
        checkBackup();
        return backupServer.ping();
    }

    @Override
    public PingReply ping() throws RemoteException {
        return null;
    }

    @Override
    public Reply callBackupUpdate(GameState gameState) throws RemoteException {
        checkBackup();
        return backupServer.update(gameState);
    }


    public void startPollingTimer() {
        final java.util.Timer timer = new java.util.Timer();
        final TimerTask task = new TimerTask() {

            @Override
            public void run() {
                System.out.println("poll server and update gamestate ");
                //ping primary, if fails, ping backup. get latest gamestate/serverConfig

                PingReply reply;
                try {
                    reply = (PingReply) PeerImpl.this.gameState.getServerConfig().getPrimaryServer().callPrimaryPing();
                    gameState = reply.getGameState();
                } catch (RemoteException e) {
                    try {
                        reply = (PingReply) PeerImpl.this.gameState.getServerConfig().getBackupServer().callBackupPing();
                        gameState = reply.getGameState();
                    } catch (RemoteException e1) {
                        //both primary & backup down, never happens..
                        e1.printStackTrace();
                    }
                }

            }
        };


        timer.schedule(task, 0, 30000);
    }

    @Override
    public Reply callClientGameStarted(String playerID, GameState gameState) throws RemoteException {
        System.out.println("Game is started");

        this.gameState = gameState;
        Player me = null;
        for(Player onePlayer : gameState.getPlayerList()){
            if(onePlayer.getPlayerID().equals(playerID)){
                me = onePlayer;
                break;
            }
        }
        System.out.println("My playerID: " + playerID);
        System.out.println("My Coordinate " + me.getCordx() + ":" + me.getCordy());
        startPollingTimer();

        SwingUtilities.invokeLater(new Runnable() {

            class Step {
                public void step(char ch) {
                    MoveReply moveReply;
                    System.out.println("moving");
                    try {
                        moveReply = (MoveReply) PeerImpl.this.gameState.getServerConfig().getPrimaryServer().callPrimaryMove(PeerImpl.this, ch, playerID);

                        //if game ended, print gamestate, return.
                        if (moveReply.getGameState().getRunningState().equals(RunningState.GAME_ENDED)) {
                            System.out.println("Game has ended!!");
                            moveReply.getGameState().printGamestate();
                            return;
                        }

                        if (moveReply.getPromotionStatus() == MoveReply.PromotionStatus.PROMOTED_TO_BACKUP) {
                            becomeBackup(moveReply.getGameState());
                        }
                        PeerImpl.this.gameState = moveReply.getGameState();

                        if (moveReply.isIllegalMove()) {
                            System.out.println("Move is illegal");
                        } else {
                            PeerImpl.this.gameState.printGamestate();
                        }

                    } catch (RemoteException e1) {
                        //primary down,
                        System.out.println("Primary is down");
                        try {
                            moveReply = (MoveReply) PeerImpl.this.gameState.getServerConfig().getBackupServer().primaryDied(PeerImpl.this);
                            if (moveReply.getPromotionStatus() == MoveReply.PromotionStatus.PROMOTED_TO_PRIMARY) {
                                becomePrimary(moveReply.getGameState());
                                step(ch);
                            } else if (moveReply.getPromotionStatus() == IS_BACKUP) {
                                System.out.println("I am Backup, no primary, no move");
                                //no rerun step
                            } else {
                                // update new primary
                                PeerImpl.this.gameState = moveReply.getGameState();
                                step(ch);
                            }
                        } catch (RemoteException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void run() {
                JFrame jFrame = new JFrame();
                JButton upButton = new JButton("UP");
                JButton downButton = new JButton("DOWN");
                JButton leftButton = new JButton("LEFT");
                JButton rightButton = new JButton("RIGHT");
                final Step step = new Step();
                upButton.addActionListener(e -> step.step('N'));
                downButton.addActionListener(e -> step.step('S'));
                leftButton.addActionListener(e -> step.step('W'));
                rightButton.addActionListener(e -> step.step('E'));

                jFrame.getContentPane().add(upButton, BorderLayout.NORTH);
                jFrame.getContentPane().add(downButton, BorderLayout.SOUTH);
                jFrame.getContentPane().add(leftButton, BorderLayout.WEST);
                jFrame.getContentPane().add(rightButton, BorderLayout.EAST);
                jFrame.setTitle("game");
                jFrame.setSize(500, 300);
                jFrame.setVisible(true);
            }
        });

        return new GameStartReply();
    }


    @Override
    public Reply primaryDied(Peer peer) throws RemoteException {

        return backupServer.primaryDied(peer);
    }

    private void initialStartPrimary(int N, int M) {
        this.primaryServer = new PrimaryServer(this, N, M);
        this.primaryServer.startInitialTimer();
    }

    private void initialConnectToPrimary(String host, int port) {
        try {
            System.out.println("Connecting to Primary Server at " + host + ":" + port);
            Registry registry = LocateRegistry.getRegistry(host, port);
            Peer primaryServer = (Peer) registry.lookup(NAME_PEER);
            Reply reply = primaryServer.callPrimaryJoin(this);

            if (reply instanceof JoinDeclined) {
                JoinDeclined joinReply = (JoinDeclined) reply;
                System.out.println("Join Failed. Reason: " + joinReply.reason);
            } else if (reply instanceof JoinSucceeded) {
                JoinSucceeded joinReply = (JoinSucceeded) reply;
                System.out.println("Join success. Player ID = " + joinReply.getPlayerId());
            } else if (reply instanceof JoinAsBackup) {
                JoinAsBackup joinReply = (JoinAsBackup) reply;
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

    private void joinAsBackup(GameState gameState) {
        System.out.println("Joining as backup server");
        gameState.getServerConfig().setBackupServer(this);
        this.gameState = gameState;
        this.backupServer = new BackupServer(this, gameState);
    }

    private void becomeBackup(GameState gameState) {
        System.out.println("Becoming backup server");
//        this.gameState.getServerConfig().setBackupServer(this);
        this.gameState = gameState;
        this.backupServer = new BackupServer(this, gameState);
    }

    private void becomePrimary(GameState gameState) {
        System.out.println("Becoming primary server");
        this.primaryServer = new PrimaryServer(this, gameState);
        this.gameState = gameState;
        this.gameState.getServerConfig().setPrimaryServer(this);
        //update backup after becoming primary
        try {
            this.gameState.getServerConfig().getBackupServer().callBackupUpdate(this.gameState);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }




}
