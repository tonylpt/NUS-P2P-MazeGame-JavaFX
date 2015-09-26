/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tc;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class tempClient {

    private tempClient() {}

    public static void main(String[] args) {

	String host = (args.length < 1) ? null : args[0];
	try {

	    Registry registry = LocateRegistry.getRegistry(host, 8080);
	    Server stub = (Server) registry.lookup("MazeServer");
		String playerID;

		stub.startGame(10, 10);
		ReplyMsg replyMsg1 = stub.joinGame();
		if(replyMsg1.getReplyCode() == 0) {
			playerID = replyMsg1.getPlayerID();
			ReplyMsg movReply;
			System.out.println("join game return id: " + replyMsg1.getPlayerID());

			Scanner sc = new Scanner(System.in);
			while (sc.hasNext()) {
				String input = sc.nextLine();
				char dir = input.charAt(0);
				movReply = stub.move(playerID, dir);
				movReply.getGameState().printGamestate();
				System.out.println("move reply code: " + movReply.getReplyCode());
			}
		}
		else{
			System.out.println("Failed to join Game...");
		}
	    
	} catch (Exception e) {
	    System.err.println("Client exception: " + e.toString());
	    e.printStackTrace();
	}
    }
}