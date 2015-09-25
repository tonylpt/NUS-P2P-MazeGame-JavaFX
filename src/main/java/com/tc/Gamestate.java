package com.tc;

import com.tc.model.Player;
import com.tc.model.Treasure;

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
public class GameState implements Serializable{
    //singleton gamestate.
    
    
    private int N;
    private List<Player> playerList;
    private List<Treasure> treasureList; //
    private static GameState instance = null;

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
    
    public static synchronized GameState getInstance() {
      if(instance == null) {
         instance = new GameState();
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