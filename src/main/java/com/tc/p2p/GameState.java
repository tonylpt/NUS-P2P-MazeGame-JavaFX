package com.tc.p2p;

import com.tc.model.Treasure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author lpthanh
 */
public class GameState implements Serializable {

    private int boardSizeN;

    private int treasureCountM;

    private final ServerConfig serverConfig = new ServerConfig();

    private List<Player> playerList;

    private List<Treasure> treasureList;

    private RunningState runningState = RunningState.ACCEPTING_PLAYERS;

    public GameState() {
        this.playerList = new ArrayList<>();
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

    public synchronized void initialize(int N, int M){//start game
        boardSizeN = N;
        treasureCountM = M;
        this.treasureList = new ArrayList<Treasure>();

        List<Coordinate> blackList = new ArrayList<Coordinate>();

        int i,x,y;
        Coordinate coord;
        //initialize M treasures and store in treasureList
        for(i=0; i<M; i++){
            coord = generateCord(N);
            x = coord.getX();
            y = coord.getY();
            blackList.add(coord);
            Treasure newTreasure = new Treasure(String.valueOf(i), x, y, null);
            treasureList.add(newTreasure);
        }

        //init player's position:
        for(Player onePlayer: this.playerList){
            coord = generateCord(N);
            for(int j=0; j<blackList.size(); j++){
                if(coord.getX() == blackList.get(j).getX() &&
                        coord.getY() == blackList.get(j).getY()){
                    //regen and restart if coord already exists
                    j=0;
                    coord = generateCord(N);
                }
            }
            onePlayer.setCordx(coord.getX());
            onePlayer.setCordy(coord.getY());
            blackList.add(coord);
        }
    }

    private Coordinate generateCord(int N){
        Coordinate cord = new Coordinate();
        Random rand = new Random();

        cord.setX(rand.nextInt(N));
        cord.setY(rand.nextInt(N));

        return cord;
    }
}

class Coordinate{
    int x;
    int y;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
