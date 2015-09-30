package com.tc.p2p;

import com.tc.RunningState;
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

    private List<Player> playerList = new ArrayList<>();

    private List<Treasure> treasureList = new ArrayList<>();

    private RunningState runningState = RunningState.ACCEPTING_PLAYERS;

    public GameState() {
    }

    public int getBoardSizeN() {
        return boardSizeN;
    }

    public void setBoardSizeN(int boardSizeN) {
        this.boardSizeN = boardSizeN;
    }

    public int getTreasureCountM() {
        return treasureCountM;
    }

    public void setTreasureCountM(int treasureCountM) {
        this.treasureCountM = treasureCountM;
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

    public List<Treasure> getTreasureList() {
        return treasureList;
    }

    public synchronized void initialize(){//start game
//        boardSizeN = N;
//        treasureCountM = M;
        this.treasureList = new ArrayList<Treasure>();

        List<Coordinate> blackList = new ArrayList<Coordinate>();

        int i,x,y;
        Coordinate coord;
        //initialize M treasures and store in treasureList
        for(i=0; i<treasureCountM; i++){
            coord = generateCord(boardSizeN);
            x = coord.getX();
            y = coord.getY();
            blackList.add(coord);
            Treasure newTreasure = new Treasure(String.valueOf(i), x, y, null);
            treasureList.add(newTreasure);
        }

        //init player's position:
        for(Player onePlayer: this.playerList){
            coord = generateCord(boardSizeN);
            for(int j=0; j<blackList.size(); j++){
                if(coord.getX() == blackList.get(j).getX() &&
                        coord.getY() == blackList.get(j).getY()){
                    //regen and restart if coord already exists
                    j=0;
                    coord = generateCord(boardSizeN);
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

    public void printGamestate(){
        System.out.println("======================print game state========================");
        System.out.println("No. of players: "+ this.getPlayerList().size());
        System.out.println("Primary Server: " + this.getServerConfig().getPrimaryServer());
        System.out.println("No. of treasures: "+ this.getTreasureList().size());
        for(Player player : this.getPlayerList()){
            System.out.print("Player ID: "+player.getPlayerID());
            System.out.print("  cord X: "+player.getCordx());
            System.out.print("  cord Y: "+player.getCordy());
            System.out.println("    Player treasure Count: " + player.getTreasureCount());
        }
        for(Treasure treasure : this.getTreasureList()){
            System.out.print("treasure ID: "+treasure.getTreasureID());
            System.out.print("  cord X: "+treasure.getCordx());
            System.out.print("  cord Y: "+treasure.getCordy());
            System.out.println("    assignedPlayerID: "+treasure.getAssignedPlayerID());

        }
        System.out.println("==========================================================");
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
