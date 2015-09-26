package com.tc.p2p;

import com.tc.ReplyCode;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author lpthanh
 */
public class P2PGame {

    public static final String NAME_PRIMARY = "UTOWN_PRIMARY_SERVER";

    public static final String NAME_BACKUP = "UTOWN_PRIMARY_SERVER";

    private RMIServer rmiServer;

    private GameState gameState;

    private final PeerImpl peerImpl;

    public P2PGame(boolean primary, String host, int port) {
        this.rmiServer = createServer(primary, port);

        if (primary) {
            // init the waiting / the game state
            initialStartPrimary(this.rmiServer);
        }

        try {
            this.peerImpl = new PeerImpl();
        } catch (RemoteException e) {
            throw new RuntimeException("Unable to create Peer object", e);
        }

        // connect to the primary
        initialConnectToPrimary(host, port);
    }

    private void initialStartPrimary(RMIServer server) {
        try {
            PrimaryServerImpl obj = new PrimaryServerImpl();
            PrimaryServer rmiObj = (PrimaryServer) UnicastRemoteObject.exportObject(obj, server.getListenPort());
            server.getRegistry().bind(NAME_PRIMARY, rmiObj);
        } catch (Exception ex) {
            System.err.println("Unable to start the Primary Server");
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void initialConnectToPrimary(String host, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            PrimaryServer server = (PrimaryServer) registry.lookup(NAME_PRIMARY);

            int status = server.join((Peer) peerImpl);

            System.out.println("Joining status " + status);

        } catch (RemoteException | NotBoundException e) {
            System.err.println("Unable to connect to the Primary Server");
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static RMIServer createServer(boolean primary, int port) {
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

        return new RMIServer(registry, listenPort);
    }

    private class PeerImpl extends UnicastRemoteObject implements Peer {

        protected PeerImpl() throws RemoteException {
            super();
        }

        @Override
        public void gameStarted() throws RemoteException {
            System.out.println("PLAY PLAY PLAY");
        }

        @Override
        public void gameEnded() throws RemoteException {

        }
    }

    private class PrimaryServerImpl implements PrimaryServer {

        private GameState gameState = new GameState();

        private final Object gameStateLock = new Object();

        public PrimaryServerImpl() {
            startTiming();
        }

        private void startTiming() {
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


        @Override
        public int join(Peer peer) throws RemoteException {
            synchronized (gameStateLock) {
                if (gameState.getRunningState() == RunningState.ACCEPTING_PLAYERS) {
                    System.out.println("Adding player!!!");
                    gameState.getPeerList().add(peer);
                    return ReplyCode.CODE_SUCCESS;
                } else {
                    return ReplyCode.CODE_JOIN_FAILED;
                }
            }
        }

        @Override
        public void move() throws RemoteException {
            // later
        }

    }

    /**
     * Every peer acts as an RMI Server. This class is to contain the port that the peer listens on,
     * and the registry created on that port.
     */
    private static class RMIServer {

        private Registry registry;

        private int listenPort;

        public RMIServer(Registry registry, int listenPort) {
            this.registry = registry;
            this.listenPort = listenPort;
        }

        public Registry getRegistry() {
            return registry;
        }

        public void setRegistry(Registry registry) {
            this.registry = registry;
        }

        public int getListenPort() {
            return listenPort;
        }

        public void setListenPort(int listenPort) {
            this.listenPort = listenPort;
        }
    }

    /**
     * The game can be started with one of the following commands:
     * java P2PGame -primary=1234
     * java P2PGame -primary=localhost:1234
     * java P2PGame -connect=1234
     * // java P2PGame -connect=localhost:1234
     * // java P2PGame -connect=173.333.333.333:1234
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Use one of the following options:");
            System.out.println("-primary=PORT");
            System.out.println("-connect=HOST:PORT");
            System.exit(0);
            return;
        }

        boolean isPrimary = false;
        String hostName;
        int port;

        String[] argParts = args[0].trim().split("=");
        if (argParts.length != 2) {
            System.out.println("Invalid option: " + args[0]);
            System.exit(0);
            return;
        }

        switch (argParts[0].toLowerCase()) {
            case "-primary":
                isPrimary = true;
                break;
            case "-connect":
                isPrimary = false;
                break;
            default:
                System.out.println("Invalid option: " + argParts[0]);
                System.exit(0);
                return;
        }

        String[] hostParts = argParts[1].split(":");
        String portStr;

        if (hostParts.length == 1) {
            hostName = null;
            portStr = hostParts[0];
        } else if (hostParts.length == 2) {
            hostName = hostParts[0];
            portStr = hostParts[1];
        } else {
            System.out.println("Invalid host format: " + argParts[1]);
            System.exit(0);
            return;
        }

        // Parsing the port from string to int
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            System.out.println("Port needs to be an integer: " + argParts[1]);
            System.exit(0);
            return;
        }


        new P2PGame(isPrimary, hostName, port);
    }

}
