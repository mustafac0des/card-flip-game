package com.cfg.cardflipgame;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class CardFlipGame extends Application {
    
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
    
    private static class GameConfig {
        private Difficulty difficulty;
        private int gridRows, gridCols, totalPairs, totalCards;
        private String difficultyName, description;
        
        public GameConfig(Difficulty diff, int rows, int cols, String name, String desc) {
            this.difficulty = diff;
            this.gridRows = rows;
            this.gridCols = cols;
            this.totalCards = rows * cols;
            this.totalPairs = totalCards / 2;
            this.difficultyName = name;
            this.description = desc;
        }
        
        public static GameConfig createEasy() {
            return new GameConfig(Difficulty.EASY, 4, 4, "Easy", "4×4 Grid • 8 Unique Pairs");
        }
        
        public static GameConfig createMedium() {
            return new GameConfig(Difficulty.MEDIUM, 6, 6, "Medium", "6×6 Grid • 18 Unique Pairs");
        }
        
        public static GameConfig createHard() {
            return new GameConfig(Difficulty.HARD, 8, 8, "Hard", "8×8 Grid • 32 Unique Pairs");
        }
        
        public Difficulty getDifficulty() { return difficulty; }
        public int getGridRows() { return gridRows; }
        public int getGridCols() { return gridCols; }
        public int getTotalPairs() { return totalPairs; }
        public int getTotalCards() { return totalCards; }
        public String getDifficultyName() { return difficultyName; }
        public String getDescription() { return description; }
    }
    
    private GameConfig config;
    private int[] cards;
    private boolean[] solved;
    private boolean[] revealed;
    private int[] flipped = new int[2];
    private int flippedCount = 0;
    private boolean waitingForDelay = false;
    private int matchedPairs = 0;
    private boolean gameWon = false;
    private int moves = 0;
    private Random random = new Random();
    
    private Stage primaryStage;
    private BorderPane root;
    private Label titleLabel;
    private Label descriptionLabel;
    private Label statsLabel;
    private GridPane gameGrid;
    private Button[][] cardButtons;
    private HBox difficultyButtons;
    private Button easyBtn, mediumBtn, hardBtn;
    
    private Map<Integer, String> cardSymbols = new HashMap<>();
    private Map<Integer, Color> cardColors = new HashMap<>();
    private Deque<String> matchStack = new ArrayDeque<>();
    private VBox matchStackBox;
    
    private static final String[] SYMBOLS = {
        "🎮", "🎯", "🎲", "🎪", "🎨", "🎭", "🎬", "🎤", "🎸", "🎹", "🎺", "🎻",
        "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏓", "🏸", "🥊", "🏆", "🥇", "🥈",
        "🌟", "⭐", "✨", "💫", "🌙", "☀️", "🌈", "⚡", "🔥", "💎", "🎊", "🎉",
        "🦄", "🐉", "🦋", "🌸", "🌺", "🌻", "🌷", "🌹", "🍀", "🌿", "🍃", "🌱",
        "🎁", "🎈", "🎀", "💝", "💖", "💕", "💗", "💓", "💘", "💞", "💌", "💐"
    };
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.config = GameConfig.createEasy();
        
        initializeGame();
        setupUI();
        
        primaryStage.setTitle("Memory Game");
        primaryStage.setScene(new Scene(root, 1000, 800));
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    
    private void initializeGame() {
        cards = new int[config.getTotalCards()];
        solved = new boolean[config.getTotalCards()];
        revealed = new boolean[config.getTotalCards()];
        
        for (int i = 0; i < config.getTotalPairs(); i++) {
            cards[i * 2] = i;
            cards[i * 2 + 1] = i;
        }
        
        Arrays.fill(solved, false);
        Arrays.fill(revealed, false);
        flipped[0] = flipped[1] = -1;
        flippedCount = 0;
        waitingForDelay = false;
        matchedPairs = 0;
        gameWon = false;
        moves = 0;
        
        fisherYatesShuffle();
        initializeSymbolsAndColors();
    }
    
    private void fisherYatesShuffle() {
        for (int i = config.getTotalCards() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = cards[i];
            cards[i] = cards[j];
            cards[j] = temp;
        }
    }
    
    private void initializeSymbolsAndColors() {
        cardSymbols.clear();
        cardColors.clear();
        
        for (int i = 0; i < config.getTotalPairs(); i++) {
            cardSymbols.put(i, SYMBOLS[i % SYMBOLS.length]);
            
            double hue = (double) i / config.getTotalPairs() * 360;
            cardColors.put(i, Color.hsb(hue, 0.8, 0.9));
        }
    }
    
private void setupUI() {
    root = new BorderPane();
    root.setStyle("-fx-background: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");
    
    matchStackBox = new VBox(15);
    matchStackBox.setPadding(new Insets(20));
    matchStackBox.setMaxWidth(250);
    matchStackBox.setMaxHeight(300);
    matchStackBox.setStyle("""
        -fx-background-color: rgba(248, 249, 250, 0.9);
        -fx-background-radius: 20;
        -fx-border-radius: 20;
        -fx-border-color: rgba(255,255,255,0.2);
        -fx-border-width: 1.5;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 4);
        """);
    VBox.setMargin(matchStackBox, new Insets(15));
    Label hdr = new Label("Latest Matches:");
    hdr.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
    hdr.setTextFill(Color.web("#333"));
    matchStackBox.getChildren().add(hdr);
    StackPane leftWrapper = new StackPane(matchStackBox);
    leftWrapper.setPadding(new Insets(15)); // adds margin from all sides
    root.setLeft(leftWrapper);
    
    

    root.setTop(createHeader());
    createGameGrid();
    root.setCenter(gameGrid);
    root.setBottom(createFooter());

    updateStatsLabel();
    updateStackUI();
}

private void handleMatch(int idx1, int idx2) {
    String sym = cardSymbols.get(cards[idx1]);
    matchStack.offerFirst(sym);
    if (matchStack.size() > 3) matchStack.removeLast();
    updateStackUI();
}

private void updateStackUI() {
    if (matchStackBox.getChildren().size() > 1) {
        matchStackBox.getChildren().subList(1, matchStackBox.getChildren().size()).clear();
    }
    
    matchStack.stream().limit(3).forEach(s -> {
        Label entry = new Label(s);
        entry.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        entry.setTextFill(Color.web("#333"));
        entry.setAlignment(Pos.CENTER);
        entry.setPadding(new Insets(12));
        entry.setPrefWidth(120);
        entry.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 16;
            -fx-border-radius: 16;
            -fx-border-color: rgba(0,0,0,0.1);
            -fx-border-width: 1;
            """);
        matchStackBox.getChildren().add(entry);
    });
}

    
    private VBox createHeader() {
        VBox header = new VBox(20);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(30, 20, 20, 20));
        
        titleLabel = new Label("Memory Game");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setEffect(new DropShadow(10, Color.BLACK));
        
        // Description
        descriptionLabel = new Label(config.getDescription());
        descriptionLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        descriptionLabel.setTextFill(Color.WHITE);
        descriptionLabel.setOpacity(0.9);
        
        // Stats
        statsLabel = new Label();
        statsLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        statsLabel.setTextFill(Color.WHITE);
        
        // Difficulty buttons
        difficultyButtons = new HBox(15);
        difficultyButtons.setAlignment(Pos.CENTER);
        
        easyBtn = createDifficultyButton("EASY", GameConfig.createEasy());
        mediumBtn = createDifficultyButton("MEDIUM", GameConfig.createMedium());
        hardBtn = createDifficultyButton("HARD", GameConfig.createHard());
        
        difficultyButtons.getChildren().addAll(easyBtn, mediumBtn, hardBtn);
        
        header.getChildren().addAll(titleLabel, descriptionLabel, statsLabel, difficultyButtons);
        return header;
    }
    
    private void createGameGrid() {
        gameGrid = new GridPane();
        gameGrid.setAlignment(Pos.CENTER);
        gameGrid.setHgap(8);
        gameGrid.setVgap(8);
        gameGrid.setPadding(new Insets(20));
        
        cardButtons = new Button[config.getGridRows()][config.getGridCols()];
        
        for (int i = 0; i < config.getTotalCards(); i++) {
            int row = i / config.getGridCols();
            int col = i % config.getGridCols();
            
            Button card = createCardButton(i);
            cardButtons[row][col] = card;
            gameGrid.add(card, col, row);
        }
        
        updateCardDisplay();
    }
    
    private Button createCardButton(int cardIndex) {
        Button button = new Button();
        
        double size = switch (config.getDifficulty()) {
            case EASY -> 80;
            case MEDIUM -> 60;
            case HARD -> 45;
        };
        
        button.setPrefSize(size, size);
        button.setMinSize(size, size);
        button.setMaxSize(size, size);
        
        button.setStyle("""
            -fx-background-color: linear-gradient(to bottom, #f8f9fa, #e9ecef);
            -fx-background-radius: 12;
            -fx-border-radius: 12;
            -fx-border-color: rgba(255, 255, 255, 0.3);
            -fx-border-width: 2;
            -fx-font-size: %dpx;
            -fx-font-weight: bold;
            -fx-cursor: hand;
            """.formatted((int)(size * 0.4)));
        
        button.setOnMouseEntered(e -> {
            if (!solved[cardIndex] && !revealed[cardIndex] && !waitingForDelay) {
                button.setStyle(button.getStyle() + 
                    "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa);");
                
                ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
                scale.setToX(1.05);
                scale.setToY(1.05);
                scale.play();
            }
        });
        
        button.setOnMouseExited(e -> {
            if (!solved[cardIndex] && !revealed[cardIndex]) {
                button.setStyle(button.getStyle().replace(
                    "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa);", ""));
                
                ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
                scale.setToX(1.0);
                scale.setToY(1.0);
                scale.play();
            }
        });
        
        button.setOnAction(e -> handleCardClick(cardIndex));
        
        button.setEffect(new DropShadow(5, Color.rgb(0, 0, 0, 0.2)));
        return button;
    }
    
    private void styleMainButton(Button button) {
        button.setStyle("""
            -fx-background-color: linear-gradient(to bottom, #ff6b6b, #ee5a52);
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-background-radius: 25;
            -fx-border-radius: 25;
            -fx-padding: 12 24 12 24;
            -fx-cursor: hand;
            """);
        
        button.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.3)));
        
        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + 
                "-fx-background-color: linear-gradient(to bottom, #ff5252, #e53935);");
            
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(button.getStyle().replace(
                "-fx-background-color: linear-gradient(to bottom, #ff5252, #e53935);", ""));
            
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });
    }
    
    private Button createDifficultyButton(String text, GameConfig newConfig) {
        Button button = new Button(text);
        
        boolean isSelected = newConfig.getDifficulty() == config.getDifficulty();
        
        if (isSelected) {
            button.setStyle("""
                -fx-background-color: linear-gradient(to bottom, #4ecdc4, #44a08d);
                -fx-text-fill: white;
                -fx-font-size: 12px;
                -fx-font-weight: bold;
                -fx-background-radius: 20;
                -fx-border-radius: 20;
                -fx-padding: 8 16 8 16;
                -fx-cursor: hand;
                """);
        } else {
            button.setStyle("""
                -fx-background-color: rgba(255, 255, 255, 0.2);
                -fx-text-fill: white;
                -fx-font-size: 12px;
                -fx-font-weight: bold;
                -fx-background-radius: 20;
                -fx-border-radius: 20;
                -fx-border-color: rgba(255, 255, 255, 0.3);
                -fx-border-width: 1;
                -fx-padding: 8 16 8 16;
                -fx-cursor: hand;
                """);
        }
        
        button.setOnAction(e -> switchDifficulty(newConfig));
        
        // Add hover effect
        button.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.play();
        });
        
        button.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });
        
        return button;
    }
    
    private VBox createFooter() {
        VBox footer = new VBox(15);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(20, 20, 30, 20));
        
        Button newGameBtn = new Button("New Game");
        styleMainButton(newGameBtn);
        newGameBtn.setOnAction(e -> resetGame());
        
        footer.getChildren().addAll(newGameBtn);
        return footer;
    }
    
    private void handleCardClick(int cardIndex) {
        if (waitingForDelay || gameWon || solved[cardIndex] || revealed[cardIndex]) {
            return;
        }
        
        flipCard(cardIndex, true);
        
        revealed[cardIndex] = true;
        flipped[flippedCount] = cardIndex;
        flippedCount++;
        
        if (flippedCount == 2) {
            moves++;
            updateStatsLabel();
            
            int card1 = flipped[0];
            int card2 = flipped[1];
            
            if (cards[card1] == cards[card2]) {
                Timeline delay = new Timeline(new KeyFrame(Duration.millis(500), e -> {
                    solved[card1] = true;
                    solved[card2] = true;
                    matchedPairs++;
                    
                    handleMatch(card1, card2);
                    animateMatchFound(card1, card2);
                    
                    flippedCount = 0;
                    flipped[0] = flipped[1] = -1;
                    
                    updateStatsLabel();
                    
                    if (matchedPairs == config.getTotalPairs()) {
                        gameWon = true;
                        showWinScreen();
                    }

                }));
                delay.play();
            } else {
                waitingForDelay = true;
                Timeline delay = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
                    flipCard(card1, false);
                    flipCard(card2, false);
                    
                    revealed[card1] = false;
                    revealed[card2] = false;
                    
                    waitingForDelay = false;
                    flippedCount = 0;
                    flipped[0] = flipped[1] = -1;
                }));
                delay.play();
            }
        }
    }
    
    private void showWinScreen() {
        VBox winPane = new VBox(20);
        winPane.setAlignment(Pos.CENTER);
        winPane.setPadding(new Insets(50));

        Label winLabel = new Label("🎉 YOU WIN! 🎉");
        winLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 48));
        winLabel.setTextFill(Color.WHITE);

        Button playAgain = new Button("Play Again");
        styleMainButton(playAgain);
        playAgain.setOnAction(e -> {
            // restore main grid
            initializeGame();
            createGameGrid();          
            updateCardDisplay();
            updateStatsLabel();
            root.setCenter(gameGrid);   
            gameWon = false;
        });

        winPane.getChildren().addAll(winLabel, playAgain);
        root.setCenter(winPane);
    }
    
    private void flipCard(int cardIndex, boolean reveal) {
        int row = cardIndex / config.getGridCols();
        int col = cardIndex % config.getGridCols();
        Button button = cardButtons[row][col];
        
        RotateTransition rotate = new RotateTransition(Duration.millis(200), button);
        rotate.setAxis(new javafx.geometry.Point3D(0, 1, 0));
        rotate.setFromAngle(0);
        rotate.setToAngle(90);
        
        rotate.setOnFinished(e -> {
            if (reveal) {
                Color cardColor = cardColors.get(cards[cardIndex]);
                String symbol = cardSymbols.get(cards[cardIndex]);
                
                button.setText(symbol);
                button.setStyle(button.getStyle() + String.format(
                    "-fx-background-color: %s;", toHexString(cardColor)));
            } else {
                button.setText("");
                button.setStyle(button.getStyle().replaceAll(
                    "-fx-background-color: #[0-9a-fA-F]{6};", ""));
            }
            
            RotateTransition rotateBack = new RotateTransition(Duration.millis(200), button);
            rotateBack.setAxis(new javafx.geometry.Point3D(0, 1, 0));
            rotateBack.setFromAngle(90);
            rotateBack.setToAngle(0);
            rotateBack.play();
        });
        
        rotate.play();
    }
    
    private void animateMatchFound(int card1, int card2) {
        int row1 = card1 / config.getGridCols();
        int col1 = card1 % config.getGridCols();
        int row2 = card2 / config.getGridCols();
        int col2 = card2 % config.getGridCols();
        
        Button button1 = cardButtons[row1][col1];
        Button button2 = cardButtons[row2][col2];
        
        DropShadow glow = new DropShadow();
        glow.setColor(Color.GOLD);
        glow.setRadius(20);
        glow.setSpread(0.5);
        
        button1.setEffect(glow);
        button2.setEffect(glow);
        
        ScaleTransition pulse1 = new ScaleTransition(Duration.millis(300), button1);
        pulse1.setFromX(1.0);
        pulse1.setFromY(1.0);
        pulse1.setToX(1.2);
        pulse1.setToY(1.2);
        pulse1.setAutoReverse(true);
        pulse1.setCycleCount(2);
        
        ScaleTransition pulse2 = new ScaleTransition(Duration.millis(300), button2);
        pulse2.setFromX(1.0);
        pulse2.setFromY(1.0);
        pulse2.setToX(1.2);
        pulse2.setToY(1.2);
        pulse2.setAutoReverse(true);
        pulse2.setCycleCount(2);
        
        pulse1.play();
        pulse2.play();
    }
    
    private void updateCardDisplay() {
        if (cardButtons == null) return;
        
        for (int i = 0; i < config.getTotalCards(); i++) {
            int row = i / config.getGridCols();
            int col = i % config.getGridCols();
            
            if (row < cardButtons.length && col < cardButtons[row].length) {
                Button button = cardButtons[row][col];
                
                if (solved[i] || revealed[i]) {
                    Color cardColor = cardColors.get(cards[i]);
                    String symbol = cardSymbols.get(cards[i]);
                    
                    button.setText(symbol);
                    button.setStyle(button.getStyle() + String.format(
                        "-fx-background-color: %s;", toHexString(cardColor)));
                } else {
                    button.setText("");
                    button.setStyle(button.getStyle().replaceAll(
                        "-fx-background-color: #[0-9a-fA-F]{6};", ""));
                }
            }
        }
    }
    
    private void updateStatsLabel() {
        statsLabel.setText(String.format("Moves: %d  •  Pairs: %d/%d  •  Cards: %d",
            moves, matchedPairs, config.getTotalPairs(), config.getTotalCards()));
    }
    
    private void switchDifficulty(GameConfig newConfig) {
        this.config = newConfig;
        
        descriptionLabel.setText(config.getDescription());
        
        updateDifficultyButtons();
        initializeGame();
        
        root.setCenter(null);
        createGameGrid();
        root.setCenter(gameGrid);
        
        updateStatsLabel();
    }
    
    private void updateDifficultyButtons() {
        easyBtn.setStyle(config.getDifficulty() == Difficulty.EASY ? 
            getSelectedButtonStyle() : getUnselectedButtonStyle());
        mediumBtn.setStyle(config.getDifficulty() == Difficulty.MEDIUM ? 
            getSelectedButtonStyle() : getUnselectedButtonStyle());
        hardBtn.setStyle(config.getDifficulty() == Difficulty.HARD ? 
            getSelectedButtonStyle() : getUnselectedButtonStyle());
    }
    
    private String getSelectedButtonStyle() {
        return """
            -fx-background-color: linear-gradient(to bottom, #4ecdc4, #44a08d);
            -fx-text-fill: white;
            -fx-font-size: 12px;
            -fx-font-weight: bold;
            -fx-background-radius: 20;
            -fx-border-radius: 20;
            -fx-padding: 8 16 8 16;
            -fx-cursor: hand;
            """;
    }
    
    private String getUnselectedButtonStyle() {
        return """
            -fx-background-color: rgba(255, 255, 255, 0.2);
            -fx-text-fill: white;
            -fx-font-size: 12px;
            -fx-font-weight: bold;
            -fx-background-radius: 20;
            -fx-border-radius: 20;
            -fx-border-color: rgba(255, 255, 255, 0.3);
            -fx-border-width: 1;
            -fx-padding: 8 16 8 16;
            -fx-cursor: hand;
            """;
    }
    
    private void resetGame() {
        initializeGame();
        updateCardDisplay();
        updateStatsLabel();
    }
    
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}