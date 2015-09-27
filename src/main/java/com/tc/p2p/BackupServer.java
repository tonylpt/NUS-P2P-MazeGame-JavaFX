package com.tc.p2p;

import static com.tc.p2p.Reply.MoveReply.PromotionStatus.*;

/**
 * @author lpthanh
 */
public class BackupServer {

    private final Peer owner;

    private final GameState gameState;

    private final Object gameStateLock = new Object();

    public BackupServer(Peer owner, GameState gameState) {
        this.owner = owner;
        this.gameState = gameState;
    }


    public Reply update(GameState gameState) {
        // deep - copy from game state
        return null;
    }


}
