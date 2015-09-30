import javafx.animation.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @author lpthanh
 */
public class GameUI {

    /**
     * Start the game as a primary server.
     */
    public static void start(Stage stage, GameParams.PrimaryParams params) throws Exception {
        GameUI game = new GameUI(params);
        game.startGame(stage);
    }

    /**
     * Start the game as a normal (non-primary) player.
     */
    public static void start(Stage stage, GameParams.NonPrimaryParams params) throws Exception {
        GameUI game = new GameUI(params);
        game.startGame(stage);
    }

    private final UIController uiController;

    private final P2PGame game;

    private final UILogger logger;

    private GameUI(GameParams params) throws RemoteException {
        this.logger = new UILogger();

        // trying to bootstrap the game class
        this.uiController = new UIController();
        this.game = new P2PGame(params, logger, uiController);
        this.logger.setExtraOutput(uiController);
    }

    /**
     * Shows the UI on the screen.
     */
    public void startGame(Stage stage) {
        Scene scene = new Scene(uiController.getRootUI(), 1000, 800);
        scene.setOnKeyTyped(keyEvent -> uiController.handleKeyTyped(keyEvent));
        stage.setScene(scene);
        stage.setTitle("CS5223 - Maze Game");
        stage.show();
    }

    /**
     * Log messages into both the console and the UI.
     */
    private static class UILogger implements ILogger {

        private ILogger extraOutput;

        public void setExtraOutput(ILogger logger) {
            this.extraOutput = logger;
        }

        @Override
        public void clientLog(String message) {
            System.out.println("CLIENT: " + message);
            extraOutput.clientLog(message);
        }

        @Override
        public void clientLogError(String message, Exception e) {
            System.out.println("CLIENT: " + message);
            e.printStackTrace();
            extraOutput.clientLogError(message, e);
        }

        @Override
        public void serverLog(String message) {
            System.out.println("SERVER: " + message);
            extraOutput.serverLog(message);
        }

        @Override
        public void serverLogError(String message, Exception e) {
            System.out.println("SERVER: " + message);
            extraOutput.serverLogError(message, e);
        }
    }

    /**
     * The main game controller that coordinates the UI and logic.
     */
    public static class UIController implements ILogger {

        private final GameView gameView;

        private final GameModel gameModel;

        private P2PGame game;

        public UIController() {
            this.gameView = new GameView(this);
            this.gameModel = new GameModel();
        }

        public void setGame(P2PGame game) {
            this.game = game;
        }

        public Parent getRootUI() {
            return this.gameView;
        }

        public GameModel getGameModel() {
            return gameModel;
        }

        public void handleKeyTyped(KeyEvent event) {
            KeyCode code = event.getCode();
            switch (code) {
                case A:
                    moveLeft();
                    break;
                case S:
                    moveUp();
                    break;
                case D:
                    moveDown();
                    break;
                case F:
                    moveRight();
                    break;
            }
        }

        private void moveUp() {
            gameView.highlightUp();
        }

        private void moveDown() {
            gameView.highlightDown();
        }

        private void moveLeft() {
            gameView.highlightLeft();
        }

        private void moveRight() {
            gameView.highlightRight();
        }

        @Override
        public void clientLog(String message) {
            gameModel.clientLog.add(new LogEntryModel(message, new Date()));
        }

        @Override
        public void clientLogError(String message, Exception e) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println(message);
            printWriter.println("=====stack=trace======");
            e.printStackTrace(printWriter);
            printWriter.println("======================");
            printWriter.close();
            String msg = stringWriter.toString();
            gameModel.clientLog.add(new LogEntryModel(msg, new Date()));
        }

        @Override
        public void serverLog(String message) {
            gameModel.serverLog.add(new LogEntryModel(message, new Date()));
        }

        @Override
        public void serverLogError(String message, Exception e) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println(message);
            printWriter.println("=====stack=trace======");
            e.printStackTrace(printWriter);
            printWriter.println("======================");
            printWriter.close();
            String msg = stringWriter.toString();
            gameModel.serverLog.add(new LogEntryModel(msg, new Date()));
        }

        public void onGameStarted(GameState gameState) {
            int boardSize = gameState.getBoardSize();
            List<Player> playerList = gameState.getPlayerList();

        }
    }

    /**
     * Represents the game model.
     * Encapsulates all the global variables needed for the game and UI.
     */
    private static class GameModel {

        public final ObservableList<LogEntryModel> clientLog = FXCollections.emptyObservableList();

        public final ObservableList<LogEntryModel> serverLog = FXCollections.emptyObservableList();

        public final ObservableList<PlayerModel> players = FXCollections.emptyObservableList();

        public final ObjectProperty<PlayerModel> currentPlayer = new SimpleObjectProperty<>();

        public final BooleanProperty gameStarted = new SimpleBooleanProperty(this, "gameStarted", false);

        public final IntegerProperty boardSize = new SimpleIntegerProperty(this, "boardSize", 0);

        public void initGameStarted(int newBoardSize) {
            boardSize.set(newBoardSize);
            gameStarted.set(true);
        }


    }

    /**
     * Represents the player model.
     */
    private static class PlayerModel {

        public enum Role {

            PRIMARY_SERVER("Primary"),

            BACKUP_SERVER("Backup"),

            NON_SERVER("Player");

            private String uiName;

            Role(String uiName) {
                this.uiName = uiName;
            }

            @Override
            public String toString() {
                return this.uiName;
            }
        }

        private BooleanProperty self = new SimpleBooleanProperty(this, "self", false);

        private BooleanProperty alive = new SimpleBooleanProperty(this, "alive", false);

        private BooleanProperty uiFocused = new SimpleBooleanProperty(this, "uiFocused", false);

        private ObjectProperty<Role> role = new SimpleObjectProperty<>(this, "role", Role.NON_SERVER);

        private StringProperty name = new SimpleStringProperty(this, "name", null);

        private IntegerProperty treasureObtained = new SimpleIntegerProperty(this, "treasureObtained", 0);

        private IntegerProperty xPos = new SimpleIntegerProperty(this, "xPos", 0);

        private IntegerProperty yPos = new SimpleIntegerProperty(this, "yPos", 0);

        public PlayerModel(String name,
                           boolean isSelf,
                           boolean isAlive,
                           boolean isUIFocused,
                           Role role,
                           int xPos,
                           int yPos,
                           int treasureObtained) {

            setName(name);
            setSelf(isSelf);
            setAlive(isAlive);
            setUIFocused(isUIFocused);
            setRole(role);
            setXPos(xPos);
            setYPos(yPos);
            setTreasureObtained(treasureObtained);
        }

        public PlayerModel(String name) {
            if (name == null) {
                name = generateDefaultRandomName();
            }

            this.setName(name);
        }

        public PlayerModel() {
            this(null);
        }

        public final String getName() {
            return nameProperty().get();
        }

        public final void setName(String name) {
            nameProperty().set(name);
        }

        public StringProperty nameProperty() {
            return name;
        }

        public final int getTreasureObtained() {
            return treasureObtainedProperty().get();
        }

        public final void setTreasureObtained(int value) {
            treasureObtainedProperty().set(value);
        }

        public IntegerProperty treasureObtainedProperty() {
            return treasureObtained;
        }

        public final int getXPos() {
            return xPosProperty().get();
        }

        public final void setXPos(int value) {
            xPosProperty().set(value);
        }

        public IntegerProperty xPosProperty() {
            return xPos;
        }

        public final int getYPos() {
            return yPosProperty().get();
        }

        public final void setYPos(int value) {
            yPosProperty().set(value);
        }

        public IntegerProperty yPosProperty() {
            return yPos;
        }

        public final boolean isSelf() {
            return selfProperty().get();
        }

        public final void setSelf(boolean isSelf) {
            selfProperty().set(isSelf);
        }

        public BooleanProperty selfProperty() {
            return self;
        }

        public final boolean isAlive() {
            return aliveProperty().get();
        }

        public final void setAlive(boolean isAlive) {
            aliveProperty().set(isAlive);
        }

        public BooleanProperty aliveProperty() {
            return alive;
        }

        public final boolean isUIFocused() {
            return uiFocusedProperty().get();
        }

        public final void setUIFocused(boolean isUIFocused) {
            uiFocusedProperty().set(isUIFocused);
        }

        public BooleanProperty uiFocusedProperty() {
            return uiFocused;
        }

        public final Role getRole() {
            return roleProperty().get();
        }

        public final void setRole(Role role) {
            roleProperty().set(role);
        }

        public ObjectProperty<Role> roleProperty() {
            return role;
        }

        private static String generateDefaultRandomName() {
            StringBuilder sb = new StringBuilder();
            sb.append("player-");
            UUID uuid = UUID.randomUUID();
            sb.append(uuid.toString().hashCode());
            return sb.toString();
        }

    }

    /**
     * Represents one entry in the log list.
     */
    private static class LogEntryModel {

        private StringProperty message = new SimpleStringProperty(this, "message", "");

        private ObjectProperty<Date> time = new SimpleObjectProperty<>(this, "date", new Date());

        public LogEntryModel(String message) {
            this(message, new Date());
        }

        public LogEntryModel(String message, Date time) {
            setMessage(message);
            setTime(time);
        }

        public StringProperty messageProperty() {
            return message;
        }

        public ObjectProperty<Date> timeProperty() {
            return time;
        }

        public final String getMessage() {
            return messageProperty().get();
        }

        public final void setMessage(String message) {
            messageProperty().set(message);
        }

        public final Date getTime() {
            return timeProperty().get();
        }

        public final void setTime(Date time) {
            if (time == null) {
                throw new IllegalArgumentException("Log time cannot be null");
            }

            timeProperty().set(time);
        }

    }

    /**
     * This class encapsulate the overall UI of the game window.
     */
    private static class GameView extends Parent {

        private TableView<PlayerModel> playerList = new TableView<>();

        private final ListView<LogEntryModel> playerLogList = new ListView<>();

        private final ListView<LogEntryModel> serverLogList = new ListView<>();

        private final Button clearPlayerLogList = new Button("Clear");

        private final Button clearServerLogList = new Button("Clear");

        private final DirectionButton leftButton = new DirectionButton("left [A]");

        private final DirectionButton rightButton = new DirectionButton("right [F]");

        private final DirectionButton upButton = new DirectionButton("up [S]");

        private final DirectionButton downButton = new DirectionButton("down [D]");

        private final MazeBoard mazeBoard;

        private final UIController uiController;

        public GameView(UIController uiController) {
            this.uiController = uiController;
            this.mazeBoard = new MazeBoard(this);
            this.initUI();
        }

        private void initUI() {
            GameModel gameModel = uiController.getGameModel();

            // Client Log Panel
            TitledPane playerLogPane = new TitledPane();
            playerLogPane.setText("Player Log");
            playerLogPane.setAnimated(false);
            playerLogPane.setCollapsible(false);
            playerLogPane.setExpanded(true);
            playerLogPane.setContent(createPlayerLogPanel(gameModel.clientLog));

            // Server Log Panel
            TitledPane serverLogPane = new TitledPane();
            serverLogPane.textProperty().bind(Bindings.concat("Server Log - "));
            serverLogPane.setAnimated(false);
            serverLogPane.setCollapsible(false);
            serverLogPane.setExpanded(true);
            serverLogPane.setContent(createServerLogPanel(gameModel.serverLog));

            SplitPane logPanels = new SplitPane(playerLogPane, serverLogPane);
            logPanels.setOrientation(Orientation.HORIZONTAL);
            logPanels.setDividerPositions(.5);

            // Player List Panel
            TitledPane playerListPane = new TitledPane();
            playerListPane.setText("Players");
            playerListPane.setContent(createPlayerList(gameModel.players));
            playerListPane.setCollapsible(false);
            playerListPane.setMaxHeight(Double.MAX_VALUE);

            // Game Panel
            TitledPane gamePane = new TitledPane();
            gamePane.textProperty().bind(Bindings.concat("Game: [", gameModel.boardSize, " x ", gameModel.boardSize, "]"));
            gamePane.setContent(createGamePanel());
            gamePane.setAnimated(false);
            gamePane.setCollapsible(false);
            gamePane.setExpanded(true);

            SplitPane splitter1 = new SplitPane(gamePane, playerListPane);
            splitter1.setOrientation(Orientation.HORIZONTAL);
            splitter1.setDividerPositions(.7);

            SplitPane splitter2 = new SplitPane(splitter1, logPanels);
            splitter2.setOrientation(Orientation.VERTICAL);
            splitter2.setDividerPositions(.7);
        }

        /**
         * Called when the server has signalled the client to start game
         */
        public void startGame() {
            GameModel gameModel = uiController.getGameModel();
            mazeBoard.init(gameModel.boardSize.get(), gameModel.players);
            setupPlayerSelectionHandler(gameModel.players);
        }

        /**
         * Whenever a player is clicked, select it in the list
         */
        private void setupPlayerSelectionHandler(List<PlayerModel> players) {
            players.forEach(player -> {
                player.uiFocusedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        playerList.getSelectionModel().select(player);
                    }
                });
            });
        }

        private Region createPlayerLogPanel(ObservableList<LogEntryModel> logEntries) {
            playerLogList.setItems(logEntries);
            playerLogList.setCellFactory(listView -> new LogEntryListCell());
            clearPlayerLogList.setOnAction(e -> logEntries.clear());
            VBox box = new VBox(playerLogList, clearPlayerLogList);
            box.setSpacing(0);
            box.setPadding(new Insets(0));
            VBox.setVgrow(playerLogList, Priority.ALWAYS);
            VBox.setVgrow(clearPlayerLogList, Priority.NEVER);
            clearPlayerLogList.setMaxWidth(Double.MAX_VALUE);
            return box;
        }

        private Region createServerLogPanel(ObservableList<LogEntryModel> logEntries) {
            serverLogList.setItems(logEntries);
            serverLogList.setCellFactory(listView -> new LogEntryListCell());
            clearServerLogList.setOnAction(e -> logEntries.clear());
            VBox box = new VBox(serverLogList, clearServerLogList);
            box.setSpacing(0);
            box.setPadding(new Insets(0));
            VBox.setVgrow(serverLogList, Priority.ALWAYS);
            VBox.setVgrow(clearServerLogList, Priority.NEVER);
            clearServerLogList.setMaxWidth(Double.MAX_VALUE);
            return box;
        }

        private Region createPlayerList(ObservableList<PlayerModel> players) {
            playerList.setItems(players);

            // declare the columns
            TableColumn<PlayerModel, String> nameCol = new TableColumn<>("ID");
            TableColumn<PlayerModel, String> aliveCol = new TableColumn<>("ALIVE");
            TableColumn<PlayerModel, String> roleCol = new TableColumn<>("ROLE");
            TableColumn<PlayerModel, String> treasureCol = new TableColumn<>("TREASURE");
            playerList.getColumns().addAll(nameCol, aliveCol, roleCol, treasureCol);
            playerList.setEditable(false);

            // bind the display value
            nameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
            aliveCol.setCellValueFactory(cell -> Bindings.when(cell.getValue().aliveProperty()).then("Yes").otherwise("No"));
            roleCol.setCellValueFactory(cell -> cell.getValue().roleProperty().asString());
            treasureCol.setCellValueFactory(cell -> cell.getValue().treasureObtainedProperty().asString());

            // selection handling
            playerList.getSelectionModel().selectedItemProperty().addListener((observable, old, newSelection) -> {
                players.forEach(player -> player.setUIFocused(newSelection == player));
            });

            return playerList;
        }

        private Region createGamePanel() {
            GridPane buttonPane = new GridPane();
            InnerShadow innerShadow = new InnerShadow(4, 0, 1, Color.BLACK);
            buttonPane.setEffect(innerShadow);
            buttonPane.setStyle("-fx-background-color: darkslategray");
            buttonPane.setPadding(new Insets(10, 20, 10, 20));
            buttonPane.setHgap(10);

            ColumnConstraints col1 = new ColumnConstraints();
            col1.setFillWidth(true);
            col1.setPercentWidth(25);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setFillWidth(true);
            col2.setPercentWidth(25);
            ColumnConstraints col3 = new ColumnConstraints();
            col3.setFillWidth(true);
            col3.setPercentWidth(25);
            ColumnConstraints col4 = new ColumnConstraints();
            col4.setFillWidth(true);
            col4.setPercentWidth(25);

            buttonPane.getColumnConstraints().addAll(
                    col1, col2, col3, col4
            );

            buttonPane.add(leftButton, 0, 0);
            buttonPane.add(upButton, 1, 0);
            buttonPane.add(downButton, 2, 0);
            buttonPane.add(rightButton, 3, 0);

            leftButton.setMaxWidth(Double.MAX_VALUE);
            upButton.setMaxWidth(Double.MAX_VALUE);
            downButton.setMaxWidth(Double.MAX_VALUE);
            rightButton.setMaxWidth(Double.MAX_VALUE);

            leftButton.setOnAction(e -> uiController.moveLeft());
            upButton.setOnAction(e -> uiController.moveUp());
            downButton.setOnAction(e -> uiController.moveDown());
            rightButton.setOnAction(e -> uiController.moveRight());

            VBox box = new VBox(mazeBoard, buttonPane);
            box.setSpacing(0);
            box.setPadding(new Insets(0));
            VBox.setVgrow(mazeBoard, Priority.ALWAYS);
            VBox.setVgrow(buttonPane, Priority.NEVER);

            return box;
        }

        public void highlightUp() {
            upButton.highlight();
        }

        public void highlightDown() {
            downButton.highlight();
        }

        public void highlightLeft() {
            leftButton.highlight();
        }

        public void highlightRight() {
            rightButton.highlight();
        }

        private void deselectPlayerList() {
            playerList.getSelectionModel().clearSelection();
        }
    }

    private static class MazeBoard extends GridPane {

        private final EventHandler<MouseEvent> deselector;

        private final PlayerPieceAnimator animator = new PlayerPieceAnimator();

        private final GameView parent;

        private MazeCell[][] cells;

        private List<PlayerModel> players;

        public MazeBoard(GameView parent) {
            this.parent = parent;
            this.deselector = e -> deselectAllPlayers();
            this.setMinSize(500, 500);
            this.setPadding(new Insets(2));
            this.setHgap(2);
            this.setVgap(2);
            this.setStyle("-fx-background-color: darkslategray");
        }

        private void deselectAllPlayers() {
            parent.deselectPlayerList();
            players.forEach(player -> player.setUIFocused(false));
        }

        public void init(int boardSize, ObservableList<PlayerModel> players) {
            this.cells = new MazeCell[boardSize][boardSize];
            this.players = players;

            for (int i = 0; i < boardSize; i++) {
                for (int j = 0; j < boardSize; j++) {
                    MazeCell cell = this.cells[i][j] = new MazeCell(i, j);
                    // deselect any player piece
                    cell.setOnMouseClicked(deselector);
                    add(cell, i, j);
                }
            }

            players.forEach(player -> {
                PlayerPiece playerPiece = new PlayerPiece(player);
                add(playerPiece, 0, 0);
                GridPane.setMargin(playerPiece, new Insets(15, 5, 2, 5));
                animator.autoTransition(playerPiece);
            });
        }
    }

    /**
     * Draws one line for each log entry in the log list.
     */
    private static class LogEntryListCell extends ListCell<LogEntryModel> {

        private DateFormat formatter = new SimpleDateFormat("kk:mm:ss.SSS - ");

        @Override
        public void updateItem(LogEntryModel entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                setText(formatter.format(entry.getTime()) + entry.getMessage());
                setGraphic(null);
            }
        }
    }

    /**
     * Draws the player piece on the UI.
     */
    private static class PlayerPiece extends StackPane {

        /**
         * The player object that this UI piece is assigned to. This is readonly.
         */
        private ReadOnlyObjectWrapper<PlayerModel> player = new ReadOnlyObjectWrapper<>(this, "player", null);

        public PlayerPiece(PlayerModel player) {
            if (player == null) {
                throw new IllegalArgumentException("Player cannot be null");
            }

            this.setPlayer(player);
            initStyles();
        }

        private void initStyles() {
            BooleanBinding isSelf = Bindings.selectBoolean(player, "self");
            BooleanBinding isUIFocused = Bindings.selectBoolean(player, "uiFocused");
            BooleanBinding isAlive = Bindings.selectBoolean(player, "alive");
            IntegerBinding treasureObtained = Bindings.selectInteger(player, "treasureObtained");

            // Declare the three UI layers
            Region background = new Region();
            Text treasureText = new Text();
            Region selectionHighlight = new Region();
            getChildren().addAll(background, treasureText, selectionHighlight);

            {
                // Background layer: styling for isSelf and uiFocused states
                StackPane.setAlignment(background, Pos.CENTER);
                StringExpression styleString =
                        Bindings.when(isSelf)
                                .then("-fx-background-color: radial-gradient(radius 100%, white .4, gray .9, darkgray 1);")
                                .otherwise("-fx-background-color: radial-gradient(radius 100%, white 0, black .6)")
                                .concat("; -fx-background-radius: 100em; -fx-background-insets: 3; -fx-border-radius: 100em; -fx-border-width: 3;")
                                .concat(
                                        // draw a greenish outline when the piece is being selected (clicked)
                                        Bindings.when(isUIFocused)
                                                .then("-fx-border-color: yellowgreen;")
                                                .otherwise("-fx-border-color: transparent;")
                                );

                background.styleProperty().bind(styleString);

                // setting effect
                DropShadow dropShadow = new DropShadow();
                dropShadow.setRadius(8);
                dropShadow.setSpread(.2);
                dropShadow.setOffsetY(1.0);
                dropShadow.setOffsetX(1.0);
                dropShadow.setColor(Color.BLACK);
                setEffect(dropShadow);
            }

            {
                // Treasure Text layer: only display number when there is a treasure
                StackPane.setAlignment(treasureText, Pos.CENTER);
                treasureText.textProperty().bind(
                        Bindings.when(treasureObtained.greaterThan(0))
                                .then(treasureObtained.asString())
                                .otherwise(""));

                // styling the treasure text: inner shadow and outer glow
                InnerShadow innerShadow = new InnerShadow(4, 1, 1, Color.SIENNA);
                innerShadow.colorProperty().bind(Bindings.when(isSelf).then(Color.BLACK).otherwise(Color.SIENNA));
                DropShadow glow = new DropShadow(2, 0, 0, Color.TOMATO);
                glow.setInput(innerShadow);
                treasureText.setEffect(glow);
                treasureText.setFill(Color.TOMATO);
                DoubleBinding fontSize = heightProperty().divide(1.5);
                treasureText.styleProperty().bind(Bindings.concat("-fx-font-weight: bold; -fx-font-size: ", fontSize.asString(), ";"));
            }

            {
                // Selection Highlighter layer: styling for selected state
                StackPane.setAlignment(selectionHighlight, Pos.CENTER);
                StringExpression styleString =
                        Bindings.when(isUIFocused)
                                .then("-fx-border-color: yellowgreen;")
                                .otherwise("-fx-border-color: transparent;")
                                .concat(" -fx-border-radius: 100em; -fx-border-width: 3;");

                selectionHighlight.styleProperty().bind(styleString);
            }

            // Dim background and treasure text for dead state
            DoubleExpression opacityBinding = Bindings.when(isAlive).then(1).otherwise(.3);
            background.opacityProperty().bind(opacityBinding);
            treasureText.opacityProperty().bind(opacityBinding);

            setPrefSize(180, 180);
            setOnMouseClicked(e -> {
                PlayerModel player = getPlayer();
                if (player != null) {
                    player.setUIFocused(true);
                }
            });
        }

        public ReadOnlyObjectProperty<PlayerModel> playerProperty() {
            return player.getReadOnlyProperty();
        }

        public PlayerModel getPlayer() {
            return playerProperty().get();
        }

        private void setPlayer(PlayerModel player) {
            this.player.set(player);
        }

    }

    /**
     * Draws the maze cell on the UI.
     */
    private static class MazeCell extends FlowPane {

        public static final int PREF_SIZE = 200;

        private IntegerProperty treasureCount = new SimpleIntegerProperty(this, "treasureCount", 0);

        private int xPos;

        private int yPos;

        /**
         * Only initiated by the Board class
         */
        public MazeCell(int xPos, int yPos) {
            this.xPos = xPos;
            this.yPos = yPos;

            initStyles();
            setPrefSize(PREF_SIZE, PREF_SIZE);
        }

        private void initStyles() {
            Light.Distant light = new Light.Distant();
            light.setElevation(50);
            light.setAzimuth(-135);
            setEffect(new Lighting(light));
            setStyle("-fx-background-color: lightsteelblue");

            setPadding(new Insets(2, 10, 10, 8));
            setHgap(2);

            Image image = new Image("/diamond.png", 16, 16, true, true);
            ImageView treasureImage = new ImageView(image);
            Text treasureText = new Text();
            getChildren().addAll(treasureImage, treasureText);

            // don't display the image when there is no treasure
            treasureImage.visibleProperty().bind(treasureCount.greaterThan(0));

            // styling the treasure text
            treasureText.textProperty().bind(
                    Bindings.when(treasureCount.greaterThan(0))
                            .then(treasureCount.asString())
                            .otherwise("")
            );

            InnerShadow innerShadow = new InnerShadow(4, 1, 1, Color.DARKSLATEGRAY);
            treasureText.setEffect(innerShadow);
            treasureText.setFill(Color.SLATEGRAY);
            DoubleBinding fontSize = heightProperty().divide(2.5);
            treasureText.styleProperty().bind(Bindings.concat("-fx-font-weight: bold; -fx-font-size: ", fontSize.asString(), ";"));
        }

        public IntegerProperty treasureCountProperty() {
            return treasureCount;
        }

        public final int getTreasureCount() {
            return treasureCountProperty().get();
        }

        public final void setTreasureCount(int count) {
            treasureCountProperty().set(count);
        }

        public int getXPos() {
            return xPos;
        }

        public int getYPos() {
            return yPos;
        }
    }

    /**
     * Extending a normal JavaFX button to support flashing animation.
     */
    private static class DirectionButton extends Button {

        public static final Color NORMAL = Color.LIGHTBLUE;

        public static final Color HIGHLIGHT = Color.SLATEGRAY;

        private ObjectProperty<Color> baseColor = new SimpleObjectProperty<>(NORMAL);

        private Timeline highlighter;

        public DirectionButton(String text) {
            super(text);

            StringBinding styleString = Bindings.createStringBinding(() -> {
                Color color = baseColor.get();
                return "-fx-background-radius: 20 20 20 20; -fx-base: rgba("
                        + ((int) (color.getRed() * 255)) + ","
                        + ((int) (color.getGreen() * 255)) + ","
                        + ((int) (color.getBlue() * 255)) + ","
                        + color.getOpacity() + ")";
            }, baseColor);
            styleProperty().bind(styleString);

            highlighter = new Timeline(
                    new KeyFrame(Duration.seconds(0), new KeyValue(baseColor, HIGHLIGHT, Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(0.5), new KeyValue(baseColor, NORMAL, Interpolator.EASE_OUT))
            );
            highlighter.setCycleCount(1);
            highlighter.setAutoReverse(false);
        }

        public void highlight() {
            highlighter.playFromStart();
        }
    }

    /**
     * This class handles the animation and interactivty of all the player pieces on the game board.
     */
    private static class PlayerPieceAnimator {

        private final HashMap<Node, TranslateTransition> horizonalTrans = new HashMap<>();

        private final HashMap<Node, TranslateTransition> verticalTrans = new HashMap<>();

        private final Animator animator = new Animator();

        /**
         * Automatically animate the player piece to a new position when changed.
         */
        public void autoTransition(PlayerPiece piece) {
            PlayerModel player = piece.getPlayer();
            if (player != null) {
                ChangeListener<Number> positionChange = (observable, oldPos, newPos) -> {
                    GridPane.setRowIndex(piece, player.getXPos());
                    GridPane.setColumnIndex(piece, player.getYPos());
                };

                player.xPosProperty().addListener(positionChange);
                player.yPosProperty().addListener(positionChange);
            }

            piece.layoutXProperty().addListener(animator);
            piece.layoutYProperty().addListener(animator);
        }

        /**
         * The change listener for animating the player pieces on the board
         */
        private class Animator implements ChangeListener<Number> {

            public static final long TRANSITION_DURATION = 500;

            @Override
            public void changed(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
                DoubleProperty doubleProperty = (DoubleProperty) property;
                double distance = newValue.doubleValue() - oldValue.doubleValue();
                PlayerPiece piece = (PlayerPiece) doubleProperty.getBean();

                switch (doubleProperty.getName()) {

                    case "layoutX": {
                        TranslateTransition trans = horizonalTrans.get(piece);
                        if (trans == null) {
                            trans = new TranslateTransition(Duration.millis(TRANSITION_DURATION), piece);
                            trans.setInterpolator(Interpolator.EASE_OUT);
                            horizonalTrans.put(piece, trans);
                            trans.setToX(0);
                        }

                        double from = piece.getTranslateX() - distance;
                        piece.setTranslateX(from);
                        trans.setFromX(from);
                        trans.playFromStart();
                        break;
                    }
                    case "layoutY": {
                        TranslateTransition trans = verticalTrans.get(piece);
                        if (trans == null) {
                            trans = new TranslateTransition(Duration.millis(TRANSITION_DURATION), piece);
                            trans.setInterpolator(Interpolator.EASE_OUT);
                            verticalTrans.put(piece, trans);
                            trans.setToY(0);
                        }

                        double from = piece.getTranslateY() - distance;
                        piece.setTranslateY(from);
                        trans.setFromY(from);
                        trans.playFromStart();
                        break;
                    }
                }
            }
        }
    }
}
