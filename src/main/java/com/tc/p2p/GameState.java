package com.tc.p2p;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lpthanh
 */
public class GameState implements Serializable {

    private final ServerConfig serverConfig = new ServerConfig();

    private final List<Player> playerList = new ArrayList<>();

    private RunningState runningState = RunningState.ACCEPTING_PLAYERS;

    public GameState() {

    }

    public RunningState getRunningState() {
        return runningState;
    }

    public void setRunningState(RunningState runningState) {
        this.runningState = runningState;
    }

    public List<Player> getPlayerList() {
        return playerList;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }
}
