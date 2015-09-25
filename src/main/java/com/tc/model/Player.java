package com.tc.model;

import java.io.Serializable;

public class Player implements Serializable {

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
