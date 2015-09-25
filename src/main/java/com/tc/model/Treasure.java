package com.tc.model;

import java.io.Serializable;

public class Treasure implements Serializable {
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
