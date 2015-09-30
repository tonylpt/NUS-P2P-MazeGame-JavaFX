import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class contains the extra security information that is passed between primary and backup server,
 * as well as passed to a peer who is becoming a new server.
 * <p>
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

    /**
     * Authenticate the player ID and auth code with the peer session.
     */
    public boolean auth(IPeer peer, String playerId, String authCode) {
        // verify auth codes
        String correctAuthCode = getAuthCodes().get(playerId);
        if (correctAuthCode == null || !correctAuthCode.equals(authCode)) {
            // incorrect code
            return false;
        }

        // verify remote object
        IPeer savedPeer = getPeers().get(playerId);
        if (savedPeer == null || peer == null || savedPeer.hashCode() != peer.hashCode()) {
            return false;
        }

        return true;
    }


}
