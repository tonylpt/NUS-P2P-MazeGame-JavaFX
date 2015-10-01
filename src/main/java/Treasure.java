import java.io.Serializable;


public class Treasure implements Serializable {

    private String treasureId;

    private int posX;

    private int posY;

    private String assignedPlayerId;

    public Treasure(String treasureId, int posX, int posY, String assignedPlayerId) {
        this.treasureId = treasureId;
        this.posX = posX;
        this.posY = posY;
        this.assignedPlayerId = assignedPlayerId;
    }

    public String getTreasureId() {
        return treasureId;
    }

    public void setTreasureId(String treasureId) {
        this.treasureId = treasureId;
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

    public String getAssignedPlayerId() {
        return assignedPlayerId;
    }

    public void setAssignedPlayerId(String assignedPlayerId) {
        this.assignedPlayerId = assignedPlayerId;
    }


}
