package com.tc.p2p;

import java.io.Serializable;

/**
 * @author lpthanh
 */
public interface Reply extends Serializable {

    class JoinDeclined implements Reply {

        public final String reason;

        public JoinDeclined(String reason) {
            this.reason = reason;
        }

        public JoinDeclined() {
            this("");
        }
    }

    class JoinSucceeded implements Reply {

        private String playerId;

        public JoinSucceeded(String playerId) {
            this.playerId = playerId;
        }

        public String getPlayerId() {
            return playerId;
        }
    }

    class JoinAsBackup implements Reply {

        private GameState gameState;

        private String playerId;

        public JoinAsBackup(String playerId, GameState gameState) {
            this.playerId = playerId;
            this.gameState = gameState;
        }

        public GameState getGameState() {
            return gameState;
        }

        public String getPlayerId() {
            return playerId;
        }
    }

    class GameStartReply implements Reply {

    }

    class MoveReply implements Reply {

        /**
         * Whether the peer should become the new primary or backup
         */
        public enum PromotionStatus {

            NONE,

            PROMOTED_TO_PRIMARY,

            PROMOTED_TO_BACKUP

        }

        private PromotionStatus promotionStatus = PromotionStatus.NONE;

        private GameState gameState;

        public MoveReply(PromotionStatus promotionStatus, GameState gameState) {
            this.promotionStatus = promotionStatus;
            this.gameState = gameState;
        }

        public PromotionStatus getPromotionStatus() {
            return promotionStatus;
        }

        public GameState getGameState() {
            return gameState;
        }
    }

    class PingReply implements Reply {
        private GameState gameState;

        public PingReply(GameState gameState) {
            this.gameState = gameState;
        }

        public GameState getGameState() {
            return gameState;
        }
    }
}
