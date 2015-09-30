import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class contains the extra security information that is passed between primary and backup server,
 * as well as passed to a peer who is becoming a new server.
 *
 * Objects of this class should not be passed to non-server peers.
 *
 * @author lpthanh
 */
public class ServerSecrets implements Serializable {

    private final Map<String, String> authCodes = new HashMap<>();

    private final Map<String, IPeer> peers = new HashMap<>();

    public Map<String, String> getAuthCodes() {
        return authCodes;
    }

    public Map<String, IPeer> getPeers() {
        return peers;
    }

}
