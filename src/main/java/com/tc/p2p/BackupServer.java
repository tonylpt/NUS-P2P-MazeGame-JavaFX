package com.tc.p2p;

import com.tc.Gamestate;
import com.tc.model.ServerConfig;

/**
 * @author lpthanh
 */
public interface BackupServer {

    /**
     * Called by the primary server to the backup server to update the latest game state
     */
    void updateGameState(Gamestate gameState);

    ServerConfig primaryDown();

}
