package com.tc.p2p;

import java.io.Serializable;

public class ServerConfig implements Serializable {

    private Peer primaryServer;

    private Peer backupServer;

    public Peer getPrimaryServer() {
        return primaryServer;
    }

    public void setPrimaryServer(Peer primaryServer) {
        this.primaryServer = primaryServer;
    }

    public Peer getBackupServer() {
        return backupServer;
    }

    public void setBackupServer(Peer backupServer) {
        this.backupServer = backupServer;
    }

}
