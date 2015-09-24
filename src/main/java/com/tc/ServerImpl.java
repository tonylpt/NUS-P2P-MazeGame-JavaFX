/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tc;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author chenchi
 */
public class ServerImpl implements Server{

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_JOIN_FAILED = 1;
    public static final int CODE_PLAYER_NOT_FOUND = 2;
    public static final int CODE_ILLEGAL_MOVE = 2;


    
    private Gamestate gamestate;
    private static int currentPlayerID;
    private static boolean canJoinGame;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        try {
            System.out.println("======================Server Started=======================");
            LocateRegistry.createRegistry(1234);

            ServerImpl obj = new ServerImpl();
            Server MazeServer = (Server) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry(1234);
            registry.bind("MazeServer", MazeServer);
        } catch (Exception ex) {
            System.err.println("Server exception: " + ex.toString());
            ex.printStackTrace();
        }
    }

//    @Override
//    public void run() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
    
    public void printGameState(Gamestate gamestate){
        
        System.out.println("======================print game state========================");
        System.out.println("Size of Grid: "+gamestate.getN());
        System.out.println("No. of players: "+gamestate.getPlayerList().size());
        System.out.println("No. of treasures: "+gamestate.getTreasureList().size());
        for(Player player : gamestate.getPlayerList()){
            System.out.print("Player ID: "+player.getPlayerID());            
            System.out.print("  cord X: "+player.getCordx());
            System.out.print("  cord Y: "+player.getCordy());
            System.out.println("    Player treasure Count: " + player.getTreasureCount());
        }
        for(Treasure treasure : gamestate.getTreasureList()){
            System.out.print("treasure ID: "+treasure.getTreasureID());            
            System.out.print("  cord X: "+treasure.getCordx());
            System.out.print("  cord Y: "+treasure.getCordy());
            System.out.println("    assignedPlayerID: "+treasure.getAssignedPlayerID());
            
        }
        System.out.println("==========================================================");
    }
    
    private void printPlayer(){
        
        System.out.println("=====================print player++======================");
        System.out.println("No. of players: "+gamestate.getPlayerList().size());
        for(Player player : gamestate.getPlayerList()){
            System.out.print("Player ID: "+player.getPlayerID());            
            System.out.print("  cord X: "+player.getCordx());
            System.out.print("  cord Y: "+player.getCordy());
            System.out.println("    Player treasure Count: " + player.getTreasureCount());
            
        }
        System.out.println("==========================================================");
    }
    
    
    //initialize variables, reset gamestate.
    public void startGame(int N, int M){
        currentPlayerID = 0;
        
        //create gamestate.
        gamestate = Gamestate.getInstance();
        gamestate.initialize(N, M);
        this.canJoinGame = true;
        
        //print
        printGameState(this.gamestate);
        
        this.stopJoinGame();
    }
    
    private void stopJoinGame(){
        final Timer timer = new Timer();
        
        final TimerTask task = new TimerTask() {

            @Override
            public void run() {
                canJoinGame = false;
                System.out.println("20secs passed, Game START ");
                //print
                timer.cancel();
                timer.purge();
                printGameState(gamestate);
            }
        };
        
        
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, 2);
        timer.schedule(task, c.getTime());
    }
    
    
    
    private static synchronized String createID(){
            return String.valueOf(currentPlayerID++);
    }
    
    public synchronized ReplyMsg joinGame(){
        String playerID = null;
        ReplyMsg replyMsg = new ReplyMsg();
        
        if(this.canJoinGame){
            playerID = this.createID();
            this.gamestate.addPLayer(playerID);
            replyMsg.setGamestate(gamestate);
            replyMsg.setReplyCode(CODE_SUCCESS);
            replyMsg.setPlayerID(playerID);

        }
        else{
            System.out.println("Join Game failed");
            System.out.println("Game has started. No more new players");
            replyMsg.setReplyCode(CODE_JOIN_FAILED);
        }
        
        //print
        this.printPlayer();
        
        return replyMsg;
    }
    
    public synchronized ReplyMsg move(String playerID, char direction){
        int errorCode = 0; // 1 - player not found,  2- illegal move
        ReplyMsg replyMsg = new ReplyMsg();
        
        Player player = gamestate.findPlayerByID(playerID);
        if(player == null){//if playerID not found, reply error
            errorCode = 1;
        }
        else{//player ID found
            //update gamestate;
            switch(direction){
                case 'N':
                    if(player.getCordy()+1 >= gamestate.getN()){//check for illegal move
                        errorCode = 2;
                    }
                    else{
                        player.setCordy(player.getCordy()+1);
                    }
                    break;
                case 'S':
                    if(player.getCordy()-1 < 0){//check for illegal move
                        errorCode = 2;
                    }
                    else{
                        player.setCordy(player.getCordy()-1);
                    }
                    break;
                case 'E':
                    if(player.getCordx()+1 >= gamestate.getN()){//check for illegal move
                        errorCode = 2;
                    }
                    else{
                        player.setCordx(player.getCordx()+1);
                    }
                    break;
                case 'W':
                    if(player.getCordx()-1 < 0){//check for illegal move
                        errorCode = 2;
                    }
                    else{
                        player.setCordx(player.getCordx()-1);
                    }
                    break;
                case 'O': //no move
                    break;
                default :// illegal move
                    errorCode = 2;
                    break;
            }
            
        }
        if(errorCode == 1){
            //set some message in gamestate?
            System.out.println("Error 1: Player not found");
            replyMsg.setReplyCode(CODE_PLAYER_NOT_FOUND);
            
        }
            
        else if(errorCode == 2){
            System.out.println("Error 2: Illegal move");
            replyMsg.setReplyCode(CODE_ILLEGAL_MOVE);
            replyMsg.setPlayerID(playerID);
        }
        else{
            replyMsg.setReplyCode(CODE_SUCCESS);
            replyMsg.setPlayerID(playerID);
            replyMsg.setGamestate(gamestate);
        }
        return replyMsg;
    }
}
