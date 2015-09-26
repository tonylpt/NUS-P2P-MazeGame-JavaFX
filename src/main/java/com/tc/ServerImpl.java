/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tc;

import com.tc.p2p.Player;
import com.tc.model.Treasure;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class ServerImpl implements Server{

    private Gamestate gameState;
    private int currentPlayerID;
    private static boolean canJoinGame;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        try {
            System.out.println("======================Server Started=======================");
            LocateRegistry.createRegistry(8080);

            ServerImpl obj = new ServerImpl();
            Server MazeServer = (Server) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry(8080);
            registry.bind("MazeServer", MazeServer);
        } catch (Exception ex) {
            System.err.println("Server exception: " + ex.toString());
            ex.printStackTrace();
        }
    }
    
    //initialize variables, reset gamestate.
    public synchronized void startGame(int N, int M){
        //create gamestate if no game state (1st player).
        if(this.gameState == null) {
            currentPlayerID = 0;
            gameState = Gamestate.getInstance();
            gameState.initialize(N, M);
            this.canJoinGame = true;
            this.currentPlayerID = 0;
//            this.stopJoinGame();
        }
        
        //print
        this.gameState.printGamestate();
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
                gameState.printGamestate();
            }
        };
        
        
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, 2);
        timer.schedule(task, c.getTime());
    }
    
    
    
    private synchronized String createID(){
            return String.valueOf(currentPlayerID++);
    }
    
    public synchronized ReplyMsg joinGame(){
        String playerID = null;
        ReplyMsg replyMsg = new ReplyMsg();
        
        if(this.canJoinGame){
            playerID = this.createID();
            this.gameState.addPLayer(playerID);
            replyMsg.setGameState(gameState);
            replyMsg.setReplyCode(ReplyCode.CODE_SUCCESS);
            replyMsg.setPlayerID(playerID);
//            this.canJoinGame = false;

        }
        else{
            System.out.println("Join Game failed");
            System.out.println("Game has started. No more new players");
            replyMsg.setReplyCode(ReplyCode.CODE_JOIN_FAILED);
        }
        
        //print
        this.printPlayer();
        
        return replyMsg;
    }

    private void getTreasures(Player player){
        List<Treasure> treasureList = gameState.getTreasureList();
        for(Treasure oneTreasure : treasureList){
            if(oneTreasure.getAssignedPlayerID() == null &&
                    oneTreasure.getCordx() == player.getCordx() &&
                    oneTreasure.getCordy() == player.getCordy()){
                oneTreasure.setAssignedPlayerID(player.getPlayerID());
                player.setTreasureCount(player.getTreasureCount()+1);
            }
        }
    }


    public synchronized ReplyMsg move(String playerID, char direction){
        int errorCode = 0; // 1 - player not found,  2- illegal move
        ReplyMsg replyMsg = new ReplyMsg();
        
        Player player = gameState.findPlayerByID(playerID);
        if(player == null){//if playerID not found, reply error
            errorCode = 1;
        }
        else{//player ID found
            //update gamestate;
            switch(direction){
                case 'N':
                    if(player.getCordy()+1 >= gameState.getN()){//check for illegal move
                        errorCode = 2;
                    }
                    else{
                        player.setCordy(player.getCordy()+1);
                        getTreasures(player);
                    }
                    break;
                case 'S':
                    if(player.getCordy()-1 < 0){//check for illegal move
                        errorCode = 2;
                    }
                    else{
                        player.setCordy(player.getCordy()-1);
                        getTreasures(player);
                    }
                    break;
                case 'E':
                    if(player.getCordx()+1 >= gameState.getN()){//check for illegal move
                        errorCode = 2;
                    }
                    else{
                        player.setCordx(player.getCordx()+1);
                        getTreasures(player);
                    }
                    break;
                case 'W':
                    if(player.getCordx()-1 < 0){//check for illegal move
                        errorCode = 2;
                    }
                    else{
                        player.setCordx(player.getCordx()-1);
                        getTreasures(player);

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
            replyMsg.setReplyCode(ReplyCode.CODE_PLAYER_NOT_FOUND);
            
        }
            
        else if(errorCode == 2){
            System.out.println("Error 2: Illegal move");
            replyMsg.setReplyCode(ReplyCode.CODE_ILLEGAL_MOVE);
            replyMsg.setPlayerID(playerID);
        }
        else{
            replyMsg.setReplyCode(ReplyCode.CODE_SUCCESS);
            replyMsg.setPlayerID(playerID);
            replyMsg.setGameState(gameState);

        }
        this.gameState.printGamestate();
        return replyMsg;
    }

    //new game if gamestate is null - game ends, set gamestate to null?...
    public synchronized boolean isFirstPlayer(){
        boolean isFirstPlayer = true;

        if(this.gameState!=null){
            isFirstPlayer = false;
        }

        return isFirstPlayer;
    }

    private void printPlayer(){

        System.out.println("=====================print player++======================");
        System.out.println("No. of players: "+ gameState.getPlayerList().size());
        for(Player player : gameState.getPlayerList()){
            System.out.print("Player ID: "+player.getPlayerID());
            System.out.print("  cord X: "+player.getCordx());
            System.out.print("  cord Y: "+player.getCordy());
            System.out.println("    Player treasure Count: " + player.getTreasureCount());

        }
        System.out.println("==========================================================");
    }

}
