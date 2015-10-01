
public enum RunningState {

    /**
     * Waiting for new joiners.
     */
    ACCEPTING_PLAYERS,

    /**
     * No longer accepting new joins.
     * Game has started.
     */
    GAME_STARTED,

    /**
     * Go home.
     */
    GAME_ENDED

}
