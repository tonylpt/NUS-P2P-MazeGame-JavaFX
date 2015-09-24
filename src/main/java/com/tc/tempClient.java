/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tc;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class tempClient {

    private tempClient() {}

    public static void main(String[] args) {

	String host = (args.length < 1) ? null : args[0];
	try {
	    Registry registry = LocateRegistry.getRegistry(host);
	    Server stub = (Server) registry.lookup("MazeServer");
	    stub.startGame(2, 2);
            ReplyMsg replyMsg1 = stub.joinGame();
            System.out.println("join game return id: " + replyMsg1.getPlayerID());
            ReplyMsg replyMsg2 = stub.move("0", 'N');
            System.out.println("join game return state: " + replyMsg2.getReplyCode());            
	    
	} catch (Exception e) {
	    System.err.println("Client exception: " + e.toString());
	    e.printStackTrace();
	}
    }
}