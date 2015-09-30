package com.tc.p2p;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author lpthanh
 */
public interface Peer extends Remote {



    /**
     * Called by a peer to primary server.
     */
    Reply ping() throws RemoteException;












}
