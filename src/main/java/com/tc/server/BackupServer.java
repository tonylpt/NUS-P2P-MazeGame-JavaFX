package com.tc.server;

import com.tc.GameState;
import com.tc.model.ServerConfig;

/**
 * @author lpthanh
 */
public interface BackupServer {

    void updateGameState(GameState gameState);

    ServerConfig bigGuyDied();

}
