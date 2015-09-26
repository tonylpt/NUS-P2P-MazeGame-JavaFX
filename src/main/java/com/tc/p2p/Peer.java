package com.tc.p2p;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author lpthanh
 */
public interface Peer extends Remote {

    /**
     * Called by the Primary server to the peer to signal that the game is
     * started (after 20 seconds from the first join)
     */
    void gameStarted() throws RemoteException;

    /**
     * Called by the Primary server to each peer to signal that the game
     * has ended
     */
    void gameEnded() throws RemoteException;

}
