package com.tc.p2p;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

import static com.tc.p2p.Reply.*;

/**
 * @author lpthanh
 */
public class PeerImpl extends UnicastRemoteObject implements Peer {

    public static final String NAME_PEER = "YUMMY_PEAR";

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

    public PeerImpl(boolean primary, String host, int port) throws RemoteException {
        this.rmiServer = createServer(this, primary, port);

        if (this.rmiServer == null) {
            throw new RuntimeException("Unable to create Peer object");
        }

        if (primary) {
            // init the waiting / the game state
            initialStartPrimary();
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
    public Reply callPrimaryMove(Peer peer) throws RemoteException {
        checkPrimary();
        return primaryServer.move(peer);
    }

    @Override
    public Reply ping() throws RemoteException {
        return new PingReply();
    }

    @Override
    public Reply callBackupUpdate(GameState gameState) throws RemoteException {
        checkBackup();
        return backupServer.update(gameState);
    }

    @Override
    public Reply callClientGameStarted() throws RemoteException {
        return null;
    }

    @Override
    public Reply callClientGameEnded() throws RemoteException {
        return null;
    }

    private void initialStartPrimary() {
        this.primaryServer = new PrimaryServer(this);
        this.primaryServer.startInitialTimer();
    }

    private void initialConnectToPrimary(String host, int port) {
        try {
            System.out.printf("Connecting to Primary Server at " + host + ":" + port);
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
                becomeBackup(joinReply.getGameState());
            } else {
                throw new RuntimeException("Unrecognized response: " + reply.getClass());
            }

        } catch (RemoteException | NotBoundException e) {
            System.err.println("Unable to connect to the Primary Server");
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void becomeBackup(GameState gameState) {
        System.out.println("Becoming backup server");
        this.backupServer = new BackupServer(this, gameState);

    }

    /**
     * @return null if error
     */
    private static RMIServer createServer(Peer peer, boolean primary, int port) {
        Registry registry = null;
        int listenPort = 0;

        if (primary) {
            // try to create registry on the port

            try {
                listenPort = port;
                registry = LocateRegistry.createRegistry(listenPort);
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
                    registry = LocateRegistry.createRegistry(listenPort);
                } catch (RemoteException e) {
                    // the exception may have been caused by the port's unavailability
                    retry = true;
                }
            } while (retry);
        }

        try {
            registry.bind(NAME_PEER, peer);
        } catch (RemoteException | AlreadyBoundException e) {
            System.err.println("Unable to bind the peer");
            e.printStackTrace();
            return null;
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
}
