package com.tc.p2p;

import com.tc.model.ServerConfig;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author lpthanh
 */
public interface PrimaryServer extends Remote {

    int join(Peer peer) throws RemoteException;

    void move() throws RemoteException;

    void ping() throws RemoteException;

}
