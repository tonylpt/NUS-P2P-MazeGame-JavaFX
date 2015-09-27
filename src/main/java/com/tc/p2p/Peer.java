package com.tc.p2p;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author lpthanh
 */
public interface Peer extends Remote {

    /**
     * Called by the peers to the Primary server to register as a player
     */
    Reply callPrimaryJoin(Peer peer) throws RemoteException;

    /**
     * Called by the peers to the Primary server to make a game move
     */
    Reply callPrimaryMove(Peer peer) throws RemoteException;

    /**
     * Called by a peer to primary server.
     */
    Reply ping() throws RemoteException;


    /**
     * Called by Primary to Backup server to update the game state
     * @param gameState
     */
    Reply callBackupUpdate(GameState gameState) throws RemoteException;

    /**
     * Called by the Primary server to the peer to signal that the game is
     * started (after 20 seconds from the first join)
     */
    Reply callClientGameStarted(String playerID, GameState gameState) throws RemoteException;

    /**
     * Called by the Primary server to each peer to signal that the game
     * has ended
     */
    Reply callClientGameEnded() throws RemoteException;


    /**
     * called by player to inform backup that primary server died.
     *
     */
    Reply primaryDied(Peer peer) throws RemoteException;


}
