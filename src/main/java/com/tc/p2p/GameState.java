package com.tc.p2p;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lpthanh
 */
public class GameState {

    private RunningState runningState = RunningState.ACCEPTING_PLAYERS;

    private List<Peer> peerList = new ArrayList<>();

    public GameState() {

    }

    public RunningState getRunningState() {
        return runningState;
    }

    public void setRunningState(RunningState runningState) {
        this.runningState = runningState;
    }

    public List<Peer> getPeerList() {
        return peerList;
    }

}
