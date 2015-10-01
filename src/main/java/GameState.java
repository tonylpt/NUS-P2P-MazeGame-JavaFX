import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


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

    /**
     * Process the move and manipulate the game board.
     *
     * @return false if the move is illegal
     */
    public boolean processMove(Move move, Player player) {
        if (player == null) {
            String playerId = move.getPlayerId();
            for (Player p : getPlayerList()) {
                if (p.getId().equals(playerId)) {
                    player = p;
                }
            }

            if (player == null) {
                // player ID not found
                return false;
            }
        }

        boolean illegalMove = false;
        switch (move.getDirection()) {
            case S: {
                if (player.getPosY() + 1 >= getBoardSize()) {
                    // check for illegal move
                    illegalMove = true;
                    break;
                }

                for (Player onePlayer : getPlayerList()) {
                    if (onePlayer.getPosX() == player.getPosX() &&
                            onePlayer.getPosY() == player.getPosY() + 1) {
                        // crashing
                        illegalMove = true;
                        break;
                    }
                }

                if (!illegalMove) {
                    player.setPosY(player.getPosY() + 1);
                    obtainTreasures(player);
                }

                break;
            }

            case N: {
                if (player.getPosY() - 1 < 0) {
                    // check for illegal move
                    illegalMove = true;
                    break;
                }

                for (Player onePlayer : getPlayerList()) {
                    if (onePlayer.getPosX() == player.getPosX() &&
                            onePlayer.getPosY() == player.getPosY() - 1) {
                        // crashing
                        illegalMove = true;
                        break;
                    }
                }

                if (!illegalMove) {
                    player.setPosY(player.getPosY() - 1);
                    obtainTreasures(player);
                }
                break;
            }

            case E: {
                if (player.getPosX() + 1 >= getBoardSize()) {
                    // check for illegal move
                    illegalMove = true;
                    break;
                }

                for (Player onePlayer : getPlayerList()) {
                    if (onePlayer.getPosX() == player.getPosX() + 1 &&
                            onePlayer.getPosY() == player.getPosY()) {
                        // crashing
                        illegalMove = true;
                        break;
                    }
                }

                if (!illegalMove) {
                    player.setPosX(player.getPosX() + 1);
                    obtainTreasures(player);
                }

                break;
            }

            case W: {
                if (player.getPosX() - 1 < 0) {//check for illegal move
                    illegalMove = true;
                    break;
                }
                for (Player onePlayer : getPlayerList()) {
                    if (onePlayer.getPosX() == player.getPosX() - 1 &&
                            onePlayer.getPosY() == player.getPosY()) {
                        illegalMove = true;
                        break;
                    }
                }
                if (!illegalMove) {
                    player.setPosX(player.getPosX() - 1);
                    obtainTreasures(player);
                }
                break;
            }
        }

        return illegalMove;
    }

    private void obtainTreasures(Player player) {
        List<Treasure> treasureList = getTreasureList();

        // search for the treasure
        for (Treasure oneTreasure : treasureList) {
            if (oneTreasure.getAssignedPlayerId() == null &&
                    oneTreasure.getPosX() == player.getPosX() &&
                    oneTreasure.getPosY() == player.getPosY()) {
                oneTreasure.setAssignedPlayerId(player.getId());
                player.setTreasureCount(player.getTreasureCount() + 1);
                break;
            }
        }
    }

    /**
     * Search for a player from ID
     */
    public Player searchById(String playerId) {
        for (Player onePlayer : getPlayerList()) {
            if (playerId.equals(onePlayer.getId())) {
                return onePlayer;
            }
        }

        return null;
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
