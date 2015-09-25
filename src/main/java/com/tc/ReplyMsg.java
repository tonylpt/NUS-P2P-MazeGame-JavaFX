
package com.tc;

import com.tc.model.ServerConfig;

import java.io.Serializable;

public class ReplyMsg implements Serializable {

    private String playerID;

    private GameState gameState;

    private int replyCode;

    private ServerConfig primaryServer;

    private ServerConfig backupServer;

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public int getReplyCode() {
        return replyCode;
    }

    public void setReplyCode(int replyCode) {
        this.replyCode = replyCode;
    }

    public String getPlayerID() {
        return playerID;
    }

    public void setPlayerID(String playerID) {
        this.playerID = playerID;
    }

    public ServerConfig getPrimaryServer() {
        return primaryServer;
    }

    public void setPrimaryServer(ServerConfig primaryServer) {
        this.primaryServer = primaryServer;
    }

    public ServerConfig getBackupServer() {
        return backupServer;
    }

    public void setBackupServer(ServerConfig backupServer) {
        this.backupServer = backupServer;
    }
}