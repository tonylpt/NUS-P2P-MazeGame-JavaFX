import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author lpthanh
 */
public class GameState implements Serializable {

    private final ServerConfig serverConfig = new ServerConfig();

    private final List<Player> playerList = new ArrayList<>();

    private final List<Treasure> treasureList = new ArrayList<>();

    private final int boardSize;

    private RunningState runningState = RunningState.ACCEPTING_PLAYERS;

    public GameState(int boardSize) {
        this.boardSize = boardSize;
    }

    public int getBoardSize() {
        return boardSize;
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

    public RunningState getRunningState() {
        return runningState;
    }

    public void setRunningState(RunningState runningState) {
        this.runningState = runningState;
    }

    /**
     * Initialize the game board by creating treasures and placing the players randomly on the
     * game board.
     *
     * @param treasureCount the total number of treasures that should be created.
     */
    public void initRandom(int treasureCount) {
        List<Coordinate> blackList = new ArrayList<>();

        int x;
        int y;
        Coordinate coordinate;

        //initialize M treasures and store in treasureList
        for (int i = 0; i < treasureCount; i++) {
            coordinate = Coordinate.generateRandom(boardSize);
            x = coordinate.getX();
            y = coordinate.getY();
            blackList.add(coordinate);
            Treasure newTreasure = new Treasure("treasure-" + i, x, y, null);
            treasureList.add(newTreasure);
        }

        //init player's position:
        for (Player player : this.playerList) {
            coordinate = Coordinate.generateRandom(boardSize);
            for (int j = 0; j < blackList.size(); j++) {
                if (coordinate.getX() == blackList.get(j).getX() &&
                        coordinate.getY() == blackList.get(j).getY()) {
                    //regen and restart if coord already exists
                    j = 0;
                    coordinate = Coordinate.generateRandom(boardSize);
                }
            }
            player.setPosX(coordinate.getX());
            player.setPosY(coordinate.getY());
            blackList.add(coordinate);
        }
    }

    private static class Coordinate {

        private int x;

        private int y;

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

        public static Coordinate generateRandom(int limit) {
            Coordinate cord = new Coordinate();
            Random rand = new Random();
            cord.setX(rand.nextInt(limit));
            cord.setY(rand.nextInt(limit));
            return cord;
        }
    }

}
