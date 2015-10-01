import java.io.Serializable;


public class ServerConfig implements Serializable {

    private IPeer primaryServer;

    private IPeer backupServer;

    private String primaryPlayerId;

    private String backupPlayerId;

    public IPeer getPrimaryServer() {
        return primaryServer;
    }

    private void setPrimaryServer(IPeer primaryServer) {
        this.primaryServer = primaryServer;
    }

    public IPeer getBackupServer() {
        return backupServer;
    }

    private void setBackupServer(IPeer backupServer) {
        this.backupServer = backupServer;
    }

    public String getPrimaryPlayerId() {
        return primaryPlayerId;
    }

    private void setPrimaryPlayerId(String primaryPlayerId) {
        this.primaryPlayerId = primaryPlayerId;
    }

    public String getBackupPlayerId() {
        return backupPlayerId;
    }

    private void setBackupPlayerId(String backupPlayerId) {
        this.backupPlayerId = backupPlayerId;
    }

    public void setPrimary(String serverId, IPeer peer) {
        setPrimaryPlayerId(serverId);
        setPrimaryServer(peer);
    }

    public void setBackup(String serverId, IPeer peer) {
        setBackupPlayerId(serverId);
        setBackupServer(peer);
    }
}
