package com.tc.model;

public class ServerConfig {

    private String primaryPlayerId;

    private String primaryHost;

    private int primaryPort;

    private String backupPlayerId;

    private String backupHost;

    private int backupPort;

    public ServerConfig() {

    }

    public String getPrimaryPlayerId() {
        return primaryPlayerId;
    }

    public void setPrimaryPlayerId(String primaryPlayerId) {
        this.primaryPlayerId = primaryPlayerId;
    }

    public String getPrimaryHost() {
        return primaryHost;
    }

    public void setPrimaryHost(String primaryHost) {
        this.primaryHost = primaryHost;
    }

    public int getPrimaryPort() {
        return primaryPort;
    }

    public void setPrimaryPort(int primaryPort) {
        this.primaryPort = primaryPort;
    }

    public String getBackupPlayerId() {
        return backupPlayerId;
    }

    public void setBackupPlayerId(String backupPlayerId) {
        this.backupPlayerId = backupPlayerId;
    }

    public String getBackupHost() {
        return backupHost;
    }

    public void setBackupHost(String backupHost) {
        this.backupHost = backupHost;
    }

    public int getBackupPort() {
        return backupPort;
    }

    public void setBackupPort(int backupPort) {
        this.backupPort = backupPort;
    }
}
