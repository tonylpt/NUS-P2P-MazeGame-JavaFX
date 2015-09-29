package com.tc.p2p;

import java.rmi.RemoteException;

import static com.tc.p2p.Reply.MoveReply.PromotionStatus.*;

/**
 * @author lpthanh
 */
public class BackupServer {

    private final Peer owner;

    private GameState gameState;

    private final Object gameStateLock = new Object();

    public BackupServer(Peer owner, GameState gameState) {
        this.owner = owner;
        this.gameState = gameState;
    }


    public Reply update(GameState gameState) {
        // deep - copy from game state
        System.out.println("backup gamestate updated!");
        this.gameState = gameState;
        return new Reply.UpdateReply(this.gameState);
    }

    public Reply primaryDied(Peer peer) throws RemoteException {

        Peer backupServer = gameState.getServerConfig().getBackupServer();
        Peer primaryServer = gameState.getServerConfig().getPrimaryServer();
        //ping primary to see if really died.
        try{
            primaryServer.ping();
        }catch(RemoteException e){
            //really died
            if(peer.hashCode() == backupServer.hashCode()){
                System.out.println("This is the backup server moving, no primary server to process...");
                //TODO if back server moving, how? no move??
                return new Reply.MoveReply(IS_BACKUP, gameState, false);
            }
            else {
                this.gameState.getServerConfig().setPrimaryServer(peer);
                return new Reply.MoveReply(PROMOTED_TO_PRIMARY, gameState, false);
            }
        }
        //never die
        return new Reply.MoveReply(NONE, gameState, false);
    }

    public Reply ping() throws RemoteException {
        return new Reply.PingReply(this.gameState);
    }
}
