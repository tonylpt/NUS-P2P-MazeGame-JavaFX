import java.io.Serializable;

/**
 * @author lpthanh
 */
public class Move implements Serializable {

    public enum Direction {

        N,

        S,

        E,

        W,

        NIL

    }

    private final Direction direction;

    private final String playerId;

    public Move(Direction direction, String playerId) {
        this.direction = direction;
        this.playerId = playerId;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getPlayerId() {
        return playerId;
    }

}
