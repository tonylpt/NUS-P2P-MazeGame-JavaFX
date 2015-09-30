import javafx.application.Application;
import javafx.stage.Stage;

import java.util.Map;


/**
 * @author lpthanh
 */
public class P2PGameBootstrap extends Application {

    /**
     * The game can be started with one of the following commands:
     * java P2PGame -primary=1234,10,10
     * java P2PGame -primary=localhost:1234,10,10
     * java P2PGame -connect=1234
     * // java P2PGame -connect=localhost:1234
     * // java P2PGame -connect=173.333.333.333:1234
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Process the command-line parameters
        Map<String, String> params = getParameters().getNamed();
        String primaryParam = params.get("primary");
        String connectParam = params.get("connect");

        if (primaryParam != null && connectParam != null) {
            System.out.println("Please specify either one of --primary or --connect");
            System.exit(0);
            return;
        }

        if (primaryParam != null) {
            // Start as primary server
            // Process the primary params

            GameParams.PrimaryParams param = GameParams.PrimaryParams.parse(primaryParam);
            GameUI.start(primaryStage, param);
        }

        if (connectParam != null) {
            // Start as a normal (non-server) peer
            // Process the normal params
            GameParams.NonPrimaryParams param = GameParams.NonPrimaryParams.parse(connectParam);
            GameUI.start(primaryStage, param);
        }
    }
}
