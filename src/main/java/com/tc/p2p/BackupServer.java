package com.tc.p2p;

import com.tc.Gamestate;
import com.tc.model.ServerConfig;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author lpthanh
 */
public interface BackupServer extends Remote {

    /**
     * Called by the primary server to the backup server to update the latest game state
     */
    void updateGameState(Gamestate gameState) throws RemoteException;

    void ping() throws RemoteException;

    ServerConfig primaryDown() throws RemoteException;

}
