/**
 * @author lpthanh
 */
public enum PeerRole {

    PRIMARY_SERVER("Primary"),

    BACKUP_SERVER("Backup"),

    NON_SERVER("Player");

    private String uiName;

    PeerRole(String uiName) {
        this.uiName = uiName;
    }

    @Override
    public String toString() {
        return this.uiName;
    }

}