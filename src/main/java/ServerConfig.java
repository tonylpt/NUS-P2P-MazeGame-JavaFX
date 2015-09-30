import java.io.Serializable;

/**
 * @author lpthanh
 */
public class ServerConfig implements Serializable {

    private IPeer primaryServer;

    private IPeer backupServer;

    public IPeer getPrimaryServer() {
        return primaryServer;
    }

    public void setPrimaryServer(IPeer primaryServer) {
        this.primaryServer = primaryServer;
    }

    public IPeer getBackupServer() {
        return backupServer;
    }

    public void setBackupServer(IPeer backupServer) {
        this.backupServer = backupServer;
    }

}
