import java.io.Serializable;

/**
 * @author lpthanh
 */
interface IReply extends Serializable {

    class JoinReply implements IReply {

        private final boolean accepted;

        private final String playerId;

        private final String authCode;

        private final boolean becomeBackup;

        private JoinReply(boolean accepted, String playerId, String authCode, boolean becomeBackup) {
            this.accepted = accepted;
            this.playerId = playerId;
            this.authCode = authCode;
            this.becomeBackup = becomeBackup;
        }

        public static JoinReply createDeclineReply() {
            return new JoinReply(false, null, null, false);
        }

        public static JoinReply createApproveAsBackupReply(String playerId, String authCode) {
            return new JoinReply(true, playerId, authCode, true);
        }

        public static JoinReply createApproveAsNormalReply(String playerId, String authCode) {
            return new JoinReply(true, playerId, authCode, false);
        }

        public boolean isAccepted() {
            return accepted;
        }

        public String getPlayerId() {
            return playerId;
        }

        public String getAuthCode() {
            return authCode;
        }

        public boolean shouldBecomeBackup() {
            return becomeBackup;
        }
    }

    class PingReply implements IReply {

        /**
         * Whether the peer should become the new primary or backup
         */
        public enum PromotionStatus {

            NONE,

            IS_BACKUP,

            PROMOTED_TO_PRIMARY,

            PROMOTED_TO_BACKUP

        }

        private final PromotionStatus promotionStatus;

        private final GameState gameState;

        private final ServerSecrets serverSecrets;

        /**
         * Server secret should be null if promotionStatus is neither
         * PROMOTED_TO_PRIMARY or PROMOTED_TO_BACKUP.
         */
        public PingReply(PromotionStatus promotionStatus,
                         GameState gameState,
                         ServerSecrets serverSecrets) {

            this.promotionStatus = promotionStatus;
            this.gameState = gameState;
            this.serverSecrets = serverSecrets;
        }

        public PromotionStatus getPromotionStatus() {
            return promotionStatus;
        }

        public GameState getGameState() {
            return gameState;
        }

        public ServerSecrets getServerSecrets() {
            return serverSecrets;
        }

        public static PingReply createUpdate(GameState gameState) {
            return new PingReply(PromotionStatus.NONE, gameState, null);
        }

        public static PingReply createPromoteToBackup(GameState gameState, ServerSecrets serverSecrets) {
            return new PingReply(PromotionStatus.PROMOTED_TO_BACKUP, gameState, serverSecrets);
        }
    }

    class MoveReply extends IReply.PingReply {

        private final boolean illegalMove;

        public MoveReply(PromotionStatus promotionStatus,
                         GameState gameState,
                         ServerSecrets serverSecrets,
                         boolean illegalMove) {

            super(promotionStatus, gameState, serverSecrets);
            this.illegalMove = illegalMove;
        }

        public boolean isIllegalMove() {
            return illegalMove;
        }

        public static MoveReply createIllegal(GameState gameState) {
            return createReply(gameState, true);
        }

        public static MoveReply createReply(GameState gameState, boolean illegalMove) {
            return new MoveReply(PromotionStatus.NONE, gameState, null, illegalMove);
        }

        public static MoveReply createPromoteToBackup(GameState gameState, ServerSecrets serverSecrets, boolean illegalMove) {
            return new MoveReply(PromotionStatus.PROMOTED_TO_BACKUP, gameState, serverSecrets, illegalMove);
        }
    }

}
