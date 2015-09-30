import com.tc.p2p.GameState;
import com.tc.p2p.Reply;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author lpthanh
 */
public interface IPeer {

    interface IClient extends IPeer, Remote {

        /**
         * Called by the Primary server to the peer to signal that the game is
         * started (after 20 seconds from the first join)
         */
        IReply callClientGameStarted(String playerID, GameState gameState) throws RemoteException;

    }

    interface IPrimaryServer extends IPeer, Remote {

        /**
         * Called by the peers to the Primary server to register as a player
         */
        IReply callPrimaryJoin(IClient peer) throws RemoteException;

        /**
         * Called by the peers to the Primary server to make a game move
         */
        IReply callPrimaryMove(IClient peer, char direction, String playerId) throws RemoteException;

        /**
         * ping primary server
         */
        IReply callPrimaryPing() throws RemoteException;

    }

    interface IBackupServer extends IPeer, Remote {

        /**
         * Called by Primary to Backup server to update the game state
         */
        IReply callBackupUpdate(GameState gameState) throws RemoteException;

        /**
         * ping backup server
         */
        IReply callBackupPing() throws RemoteException;


        /**
         * called by player to inform backup that primary server died.
         */
        IReply primaryDied(IClient peer) throws RemoteException;

    }

    interface IReply extends Serializable {

        class JoinDeclined implements IReply {

            public final String reason;

            public JoinDeclined(String reason) {
                this.reason = reason;
            }

            public JoinDeclined() {
                this("");
            }
        }

        class JoinSucceeded implements IReply {

            private String playerId;

            public JoinSucceeded(String playerId) {
                this.playerId = playerId;
            }

            public String getPlayerId() {
                return playerId;
            }
        }

        class JoinAsBackup implements IReply {

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

        class GameStartReply implements IReply {

        }

        class MoveReply implements IReply {

            /**
             * Whether the peer should become the new primary or backup
             */
            public enum PromotionStatus {

                NONE,

                IS_BACKUP,

                PROMOTED_TO_PRIMARY,

                PROMOTED_TO_BACKUP

            }

            private PromotionStatus promotionStatus = PromotionStatus.NONE;

            private GameState gameState;

            private boolean illegalMove;

            public MoveReply(PromotionStatus promotionStatus, GameState gameState, boolean illegalMove) {
                this.promotionStatus = promotionStatus;
                this.gameState = gameState;
                this.illegalMove = illegalMove;
            }

            public boolean isIllegalMove() {
                return illegalMove;
            }

            public PromotionStatus getPromotionStatus() {
                return promotionStatus;
            }

            public GameState getGameState() {
                return gameState;
            }
        }

        class PingReply implements IReply {
            private GameState gameState;

            public PingReply(GameState gameState) {
                this.gameState = gameState;
            }

            public GameState getGameState() {
                return gameState;
            }
        }

        class UpdateReply implements IReply {
            private GameState gameState;

            public UpdateReply(GameState gameState) {
                this.gameState = gameState;
            }

            public GameState getGameState() {
                return gameState;
            }
        }
    }
}
