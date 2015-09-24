/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tc;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author chenchi
 */
public interface Server extends Remote{
   public void startGame(int N, int M) throws RemoteException ;
   public ReplyMsg joinGame() throws RemoteException;
   public ReplyMsg move(String playerID, char direction) throws RemoteException;
}
