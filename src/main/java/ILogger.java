/**
 * @author lpthanh
 */
public interface ILogger {

    /**
     * Log a message relevant to the game client component.
     */
    void clientLog(String message);

    /**
     * Log a message relevant to the game client component, with an error
     */
    void clientLogError(String message, Exception e);

    /**
     * Log a message relevant to the game server component.
     */
    void serverLog(String message);

    /**
     * Log a message relevant to the game server component.
     */
    void serverLogError(String message, Exception e);

}
