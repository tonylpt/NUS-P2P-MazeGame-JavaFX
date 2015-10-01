import java.io.Serializable;

/**
 * @author lpthanh
 */
public class Player implements Serializable {

    private String id;

    private int posX;

    private int posY;

    private int treasureCount;

    private boolean alive;

    private PeerRole role;

    public Player(String id,
                  int posX,
                  int posY,
                  int treasureCount,
                  boolean alive,
                  PeerRole role) {

        this.id = id;
        this.posX = posX;
        this.posY = posY;
        this.treasureCount = treasureCount;
        this.alive = alive;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public int getTreasureCount() {
        return treasureCount;
    }

    public void setTreasureCount(int treasureCount) {
        this.treasureCount = treasureCount;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public PeerRole getRole() {
        return role;
    }

    public void setRole(PeerRole role) {
        this.role = role;
    }
}