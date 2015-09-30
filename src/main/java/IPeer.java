import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author lpthanh
 */
public interface IPeer extends Remote {

    /*============== CLIENT METHODS =================*/

    /**
     * Called by the Primary server to the peer to signal that the game is
     * started (after 20 seconds from the first join)
     */
    void callClientGameStarted(GameState gameState) throws RemoteException;


    /*============ PRIMARY SERVER METHODS ===========*/

    /**
     * Called by the peers to the Primary server to register as a player
     */
    IReply.JoinReply callPrimaryJoin(IPeer peer) throws RemoteException;

    /**
     * Called by the peers to the Primary server to make a game move
     */
    IReply.MoveReply callPrimaryMove(IPeer peer, char direction, String playerId, String authCode) throws RemoteException;

    /**
     * ping primary server
     */
    IReply.PingReply callPrimaryPing(IPeer peer, String playerId, String authCode) throws RemoteException;


    /*============ BACKUP SERVER METHODS =============*/

    /**
     * Called by Primary to Backup server to update the game state
     */
    void callBackupUpdate(GameState gameState, ServerSecrets serverSecrets) throws RemoteException;

    /**
     * ping backup server
     */
    IReply.PingReply callBackupPing() throws RemoteException;


    /**
     * called by player to inform backup that primary server died.
     */
    IReply callBackUpOnPrimaryDied(IPeer peer, String playerId, String authCode) throws RemoteException;


}
