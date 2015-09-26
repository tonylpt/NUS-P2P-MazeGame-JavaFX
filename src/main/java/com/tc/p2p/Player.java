package com.tc.p2p;

import java.io.Serializable;

public class Player implements Serializable {

    private String playerID;

    private int cordx;

    private int cordy;

    private int treasureCount;

    private Peer peer;

    private boolean alive;

    public Player(String id, int cordx, int cordy, int treasureCount, Peer peer) {
        this.playerID = id;
        this.cordx = cordx;
        this.cordy = cordy;
        this.treasureCount = treasureCount;
        this.peer = peer;
    }


    public boolean equals(Player anotherPlayer) {
        if (this.playerID.equals(anotherPlayer.playerID)) {
            return true;
        } else return false;
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

    public Peer getPeer() {
        return peer;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
