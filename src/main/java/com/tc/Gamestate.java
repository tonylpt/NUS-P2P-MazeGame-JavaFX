package com.tc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author chenchi
 */
public class Gamestate implements Serializable{
    //singleton gamestate.
    
    
    private int N;
    private List<Player> playerList;
    private List<Treasure> treasureList; //
    private static Gamestate instance = null;

    public List<Player> getPlayerList() {
        return playerList;
    }

    public void setPlayerList(List<Player> playerList) {
        this.playerList = playerList;
    }

    public List<Treasure> getTreasureList() {
        return treasureList;
    }

    public void setTreasureList(List<Treasure> treasureList) {
        this.treasureList = treasureList;
    }
    
    public int getN() {
        return N;
    }

    public void setN(int N) {
        this.N = N;
    }
    
    public static synchronized Gamestate getInstance() {
      if(instance == null) {
         instance = new Gamestate();
      }
      return instance;
   }
    
    
    public synchronized void initialize(int N, int M){//start game
        instance.N = N;
        instance.playerList = new ArrayList<Player>();
        instance.treasureList = new ArrayList<Treasure>();
        
        
        Random rand = new Random();
        
        int i,x,y;
        //initialize M treasures and store in treasureList
        for(i=0; i<M; i++){
            x = rand.nextInt(N);
            y = rand.nextInt(N);
            Treasure newTreasure = new Treasure(String.valueOf(i), x, y, null);
            treasureList.add(newTreasure);
        }
    }
    
    public Player findPlayerByID(String playerID){
        for(Player onePlayer : this.playerList){
            if(onePlayer.getPlayerID().equals(playerID)){
                return onePlayer;
            }
        }
        return null;
    } 
    
    public synchronized void addPLayer(String playerID){
        int cordx;
        int cordy;
        int treasureCount=0;
        
        Random rand = new Random();
        
        cordx = rand.nextInt(N);
        cordy = rand.nextInt(N);
        Player newPlayer = new Player(playerID,cordx,cordy,treasureCount);
        
        //if the starting point has treasure, assign it;
        for (Treasure oneTreasure : this.treasureList){
            if(oneTreasure.getAssignedPlayerID()==null && 
                    oneTreasure.getCordx() == cordx &&
                    oneTreasure.getCordy() == cordy){
                //assign treasure
                oneTreasure.setAssignedPlayerID(playerID);
                newPlayer.setTreasureCount(newPlayer.getTreasureCount() + 1);
            }
        }

        
        this.playerList.add(newPlayer);
    }
}

class Player implements Serializable{
    
    public Player(String id, int cordx, int cordy, int treasureCount){
        this.playerID = id;
        this.cordx = cordx;
        this.cordy = cordy;
        this.treasureCount = treasureCount;
    }
    
    private String playerID;
    private int cordx;
    private int cordy;
    private int treasureCount;
    
    public boolean equals(Player anotherPlayer){
        if(this.playerID.equals(anotherPlayer.playerID)){
            return true;
        }
        else return false;
    }

    public String getPlayerID() {
        return playerID;
    }

    public void setPlayerID(String playerID) {
        this.playerID = playerID;
    }

    public int getCordx() {
        return cordx;
    }

    public void setCordx(int cordx) {
        this.cordx = cordx;
    }

    public int getCordy() {
        return cordy;
    }

    public void setCordy(int cordy) {
        this.cordy = cordy;
    }

    public int getTreasureCount() {
        return treasureCount;
    }

    public void setTreasureCount(int treasureCount) {
        this.treasureCount = treasureCount;
    }
    
    
}


class Treasure implements Serializable{
    private String treasureID;
    private int cordx;
    private int cordy;
    private String assignedPlayerID;

    public Treasure(String treasureID, int cordx, int cordy, String assignedPlayerID) {
        this.treasureID = treasureID;
        this.cordx = cordx;
        this.cordy = cordy;
        this.assignedPlayerID = assignedPlayerID;
    }
    
    public String getTreasureID() {
        return treasureID;
    }

    public void setTreasureID(String treasureID) {
        this.treasureID = treasureID;
    }

    public int getCordx() {
        return cordx;
    }

    public void setCordx(int cordx) {
        this.cordx = cordx;
    }

    public int getCordy() {
        return cordy;
    }

    public void setCordy(int cordy) {
        this.cordy = cordy;
    }

    public String getAssignedPlayerID() {
        return assignedPlayerID;
    }

    public void setAssignedPlayerID(String assignedPlayerID) {
        this.assignedPlayerID = assignedPlayerID;
    }

    
}
