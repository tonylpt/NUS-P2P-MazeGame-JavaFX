package com.tc.p2p;

import java.rmi.RemoteException;

/**
 * @author lpthanh
 */
public class P2PGame {

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


        try {
            // start the game
            new com.tc.p2p.PeerImpl(isPrimary, hostName, port);
        } catch (RemoteException e) {
            System.err.println("Unable to start the game");
            e.printStackTrace();
        }
    }

}
