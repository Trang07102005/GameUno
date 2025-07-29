package com.example.gameuno;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class Game {
    @FXML private Button replayButton;
    @FXML private ImageView currentCardImage;
    @FXML private Label currentPlayerLabel;
    @FXML private Label gameStatusLabel;
    @FXML private Button drawCardButton;
    @FXML private Button unoButton;
    @FXML private StackPane currentCardPane;
    @FXML private HBox bottomPlayer, topPlayer;
    @FXML private VBox leftPlayerContainer, leftPlayer, rightPlayerContainer, rightPlayer;
    @FXML private Label topPlayerLabel, leftPlayerLabel, rightPlayerLabel, bottomPlayerLabel;

    private UnoDeck deck;
    private UnoCard currentCard;
    private List<UnoCard> playerHand;

    private List<UnoCard> ai2Hand = new ArrayList<>();
    private List<UnoCard> ai3Hand = new ArrayList<>();
    private List<UnoCard> ai4Hand = new ArrayList<>();
    private int numberOfPlayers;
    private int currentPlayer = 1;
    private int direction = 1;
    private boolean unoCalled = false;
    private boolean[] aiUno = new boolean[5];

    private PauseTransition turnTimer;

    private int penaltyStack = 0;
    private UnoCard.Value comboType = null;

    private List<String> playerNames;
    private String myName;
    @FXML private Button exitButton;

    @FXML
    public void initialize() {
        deck = new UnoDeck();
        playerHand = new ArrayList<>();

        double cardWidth = 60;
        double cardHeight = 100;

        String backImagePath = getClass().getResource("/cards/Back.png").toExternalForm();
        ImageView backImage = new ImageView(backImagePath);
        backImage.setFitWidth(cardWidth);
        backImage.setFitHeight(cardHeight);

        drawCardButton.setGraphic(backImage);
        drawCardButton.setPrefWidth(cardWidth);
        drawCardButton.setPrefHeight(cardHeight);

        for (int i = 0; i < 7; i++) {
            UnoCard card = deck.drawCard();
            playerHand.add(card);
            addCardToHand(card);
        }
        currentCard = deck.drawCard();
        updateCurrentCardView(currentCard);
        updateDeckCount();
    }

    public void initPlayers(int numberOfPlayers, List<String> playerNames) {
        this.numberOfPlayers = numberOfPlayers;
        this.playerNames = playerNames;
        this.myName = playerNames.get(0);

        unoCalled = false;

        GameLogger.startGame(playerNames); // Log game start

        leftPlayerContainer.setVisible(numberOfPlayers >= 3);
        rightPlayerContainer.setVisible(numberOfPlayers == 4);

        for (int i = 0; i < 7; i++) {
            ai2Hand.add(deck.drawCard());
            topPlayer.getChildren().add(createFaceDownCard());
            if (numberOfPlayers >= 3) {
                ai3Hand.add(deck.drawCard());
                leftPlayer.getChildren().add(createFaceDownCard());
            }
            if (numberOfPlayers == 4) {
                ai4Hand.add(deck.drawCard());
                rightPlayer.getChildren().add(createFaceDownCard());
            }
        }

        updatePlayerLabels();
    }

    @FXML
    private void drawCard() {
        if (currentPlayer != 1) {
            gameStatusLabel.setText("❌ Không phải lượt của bạn!");
            return;
        }
        if (turnTimer != null) turnTimer.stop();

        if (comboType != null && hasComboCard(playerHand)) {
            gameStatusLabel.setText("❌ Bạn phải đánh lá " + comboType + " để chồng bài hoặc rút phạt!");
            return;
        }

        int drawCount = (comboType != null) ? penaltyStack : 1;
        for (int i = 0; i < drawCount; i++) {
            UnoCard card = deck.drawCard();
            playerHand.add(card);
            addCardToHand(card);
        }
        updateDeckCount();

        GameLogger.logMove(myName, "đã bốc " + drawCount + " lá"); // Log draw action
        if (comboType != null) {
            gameStatusLabel.setText("💥 " + myName + " bốc " + penaltyStack + " lá do combo!");
            comboType = null;
            penaltyStack = 0;
        } else {
            gameStatusLabel.setText("🃏 " + myName + " đã rút 1 lá.");
        }

        unoCalled = false;
        nextTurn();
    }

    private boolean hasComboCard(List<UnoCard> hand) {
        for (UnoCard card : hand) {
            if (card.getValue() == comboType) return true;
        }
        return false;
    }

    @FXML
    private void callUno() {
        unoCalled = true;
        GameLogger.logMove(myName, "đã kêu UNO!"); // Log UNO call
        gameStatusLabel.setText("🗣️ " + myName + " đã kêu UNO!");
    }

    private void addCardToHand(UnoCard card) {
        String imagePath = getClass().getResource(card.getImagePath()).toExternalForm();
        ImageView imageView = new ImageView(imagePath);
        imageView.setFitWidth(60);
        imageView.setFitHeight(80);

        Button cardBtn = new Button();
        cardBtn.setGraphic(imageView);
        cardBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        cardBtn.setOnAction(e -> playCard(card, cardBtn));

        bottomPlayer.getChildren().add(cardBtn);
    }

    private void playCard(UnoCard card, Button cardBtn) {
        if (currentPlayer != 1) {
            gameStatusLabel.setText("❌ Không phải lượt của bạn!");
            return;
        }
        if (turnTimer != null) turnTimer.stop();

        if (card.getColor() == currentCard.getColor()
                || card.getValue() == currentCard.getValue()
                || card.getColor() == UnoCard.Color.Wild) {
            GameLogger.logMove(myName, "đã đánh lá " + card.getColor() + " " + card.getValue()); // Log play action
            if (card.getValue() == UnoCard.Value.Wild || card.getValue() == UnoCard.Value.WildDrawFour) {
                handleWild(card);
            }

            if (card.getValue() == UnoCard.Value.DrawTwo || card.getValue() == UnoCard.Value.WildDrawFour) {
                if (comboType == null) {
                    comboType = card.getValue();
                    penaltyStack = (card.getValue() == UnoCard.Value.DrawTwo) ? 2 : 4;
                } else {
                    penaltyStack += (card.getValue() == UnoCard.Value.DrawTwo) ? 2 : 4;
                }
                skipNext();
            } else if (card.getValue() == UnoCard.Value.Skip) {
                skipNext();
            } else if (card.getValue() == UnoCard.Value.Reverse) {
                direction *= -1;
                gameStatusLabel.setText("🔄 Đã đảo chiều!");
                GameLogger.logMove(myName, "đã đánh Reverse"); // Log reverse action
            }

            currentCard = card;
            updateCurrentCardView(card);
            playerHand.remove(card);
            bottomPlayer.getChildren().remove(cardBtn);
            updateDeckCount();

            if (playerHand.size() == 1 && !unoCalled) {
                for (int i = 0; i < 2; i++) {
                    UnoCard penalty = deck.drawCard();
                    playerHand.add(penalty);
                    addCardToHand(penalty);
                }
                GameLogger.logMove(myName, "quên kêu UNO! Bị phạt 2 lá"); // Log penalty
                gameStatusLabel.setText("⚠️ " + myName + " quên kêu UNO! Bị phạt 2 lá!");
            } else if (playerHand.isEmpty()) {
                endGame(myName);
                return;
            }

            unoCalled = false;
            nextTurn();
        } else {
            if (!hasValidCard()) {
                gameStatusLabel.setText("❌ Không có lá nào hợp lệ! Bạn phải bốc bài.");
                showNoPlayableCardNotification();
            } else {
                gameStatusLabel.setText("❌ Thẻ không hợp lệ!");
            }
        }
    }

    private void handleWild(UnoCard card) {
        List<String> options = List.of("Red", "Yellow", "Green", "Blue");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Red", options);
        dialog.setTitle("Chọn màu");
        dialog.setHeaderText(myName + " đã đánh Wild");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(color -> {
            card.setDynamicColor(UnoCard.Color.valueOf(color));
            gameStatusLabel.setText("🎨 Bạn chọn màu " + color.toUpperCase());
            GameLogger.logMove(myName, "đã chọn màu " + color + " cho Wild"); // Log wild color choice
        });
    }

    private void givePenalty(int playerIndex, int cards) {
        List<UnoCard> hand = getHand(playerIndex);
        for (int i = 0; i < cards; i++) {
            hand.add(deck.drawCard());
            addFaceDown(playerIndex);
        }
        GameLogger.logMove(playerNames.get(playerIndex - 1), "bị phạt " + cards + " lá"); // Log penalty
        updatePlayerLabels();
    }

    private List<UnoCard> getHand(int index) {
        return switch (index) {
            case 2 -> ai2Hand;
            case 3 -> ai3Hand;
            case 4 -> ai4Hand;
            default -> playerHand;
        };
    }

    private void skipNext() {
        currentPlayer = getNextPlayer();
        nextTurn();
    }

    private int getNextPlayer() {
        int next = currentPlayer + direction;
        if (next > numberOfPlayers) next = 1;
        if (next < 1) next = numberOfPlayers;
        return next;
    }

    private void nextTurn() {
        currentPlayer = getNextPlayer();

        if (currentPlayer == 1) {
            gameStatusLabel.setText("👉 Tới lượt " + myName);
            currentPlayerLabel.setText("Lượt: " + myName);

            if (!hasValidCard()) {
                gameStatusLabel.setText("❌ Bạn không có lá hợp lệ. Vui lòng rút bài!");
                javafx.application.Platform.runLater(this::showNoPlayableCardNotification);
            }

            startTurnTimer();
        } else {
            currentPlayerLabel.setText("Lượt: " + playerNames.get(currentPlayer - 1));
            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e -> aiTurn(currentPlayer));
            delay.play();
        }
    }

    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.stop();
        }

        turnTimer = new PauseTransition(Duration.seconds(10));
        turnTimer.setOnFinished(e -> {
            if (currentPlayer == 1) {
                gameStatusLabel.setText("⌛ Hết giờ! Bạn bị rút 1 lá!");
                UnoCard card = deck.drawCard();
                playerHand.add(card);
                addCardToHand(card);
                GameLogger.logMove(myName, "hết giờ, bị rút 1 lá"); // Log timeout
                updateDeckCount();
                nextTurn();
            }
        });
        turnTimer.play();
    }

    private void aiTurn(int index) {
        List<UnoCard> hand = getHand(index);
        UnoCard chosen = null;

        for (UnoCard card : hand) {
            if (card.getColor() == currentCard.getColor()
                    || card.getValue() == currentCard.getValue()
                    || card.getColor() == UnoCard.Color.Wild) {
                chosen = card;
                break;
            }
        }

        if (chosen != null) {
            hand.remove(chosen);
            removeFaceDown(index);
            GameLogger.logMove(playerNames.get(index - 1), "đã đánh lá " + chosen.getColor() + " " + chosen.getValue()); // Log AI play

            if (chosen.getValue() == UnoCard.Value.Wild || chosen.getValue() == UnoCard.Value.WildDrawFour) {
                UnoCard.Color[] colors = {UnoCard.Color.Red, UnoCard.Color.Yellow, UnoCard.Color.Green, UnoCard.Color.Blue};
                UnoCard.Color picked = colors[new Random().nextInt(colors.length)];
                chosen.setDynamicColor(picked);
                GameLogger.logMove(playerNames.get(index - 1), "đã chọn màu " + picked + " cho Wild"); // Log AI wild color
                gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " đánh Wild và chọn màu " + chosen.getColor());
            }

            currentCard = chosen;
            updateCurrentCardView(chosen);

            if (chosen.getValue() == UnoCard.Value.DrawTwo || chosen.getValue() == UnoCard.Value.WildDrawFour) {
                if (comboType == null) {
                    comboType = chosen.getValue();
                    penaltyStack = (chosen.getValue() == UnoCard.Value.DrawTwo) ? 2 : 4;
                } else {
                    penaltyStack += (chosen.getValue() == UnoCard.Value.DrawTwo) ? 2 : 4;
                }
                skipNext();
                return;
            }

            if (chosen.getValue() == UnoCard.Value.Skip) {
                GameLogger.logMove(playerNames.get(index - 1), "đã đánh Skip"); // Log AI skip
                gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " đánh Skip!");
                skipNext();
                return;
            }

            if (chosen.getValue() == UnoCard.Value.Reverse) {
                direction *= -1;
                GameLogger.logMove(playerNames.get(index - 1), "đã đánh Reverse"); // Log AI reverse
                gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " đánh Reverse! Đổi chiều.");
            } else {
                gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " đánh " + chosen);
            }

            if (hand.size() == 1 && !aiUno[index]) {
                aiUno[index] = true;
                GameLogger.logMove(playerNames.get(index - 1), "đã kêu UNO!"); // Log AI UNO
                gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " kêu UNO!");
                PauseTransition unoPause = new PauseTransition(Duration.seconds(1));
                unoPause.setOnFinished(e -> nextTurn());
                unoPause.play();
                return;
            }

            if (hand.isEmpty()) {
                endGame(playerNames.get(index - 1));
                return;
            }
        } else {
            int drawCount = (comboType != null) ? penaltyStack : 1;
            for (int i = 0; i < drawCount; i++) {
                hand.add(deck.drawCard());
                addFaceDown(index);
            }
            GameLogger.logMove(playerNames.get(index - 1), "đã bốc " + drawCount + " lá"); // Log AI draw
            if (comboType != null) {
                gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " bốc " + penaltyStack + " lá combo!");
                comboType = null;
                penaltyStack = 0;
            } else {
                gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " bốc 1 lá");
            }
        }

        updatePlayerLabels();
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> nextTurn());
        delay.play();
    }

    private void updatePlayerLabels() {
        bottomPlayerLabel.setText(myName + " (" + playerHand.size() + ")");
        topPlayerLabel.setText(playerNames.get(1) + " (" + ai2Hand.size() + ")");
        if (numberOfPlayers >= 3)
            leftPlayerLabel.setText(playerNames.get(2) + " (" + ai3Hand.size() + ")");
        if (numberOfPlayers == 4)
            rightPlayerLabel.setText(playerNames.get(3) + " (" + ai4Hand.size() + ")");
    }

    private void removeFaceDown(int index) {
        if (index == 2) topPlayer.getChildren().remove(0);
        if (index == 3) leftPlayer.getChildren().remove(0);
        if (index == 4) rightPlayer.getChildren().remove(0);
    }

    private void addFaceDown(int index) {
        if (index == 2) topPlayer.getChildren().add(createFaceDownCard());
        if (index == 3) leftPlayer.getChildren().add(createFaceDownCard());
        if (index == 4) rightPlayer.getChildren().add(createFaceDownCard());
    }

    private void updateDeckCount() {
        drawCardButton.setText("🎴 " + deck.remainingCards());
    }

    private void updateCurrentCardView(UnoCard card) {
        String imagePath = getClass().getResource(card.getImagePath()).toExternalForm();
        currentCardImage.setImage(new Image(imagePath));
    }

    private StackPane createFaceDownCard() {
        String imagePath = getClass().getResource("/cards/Back.png").toExternalForm();
        ImageView backImage = new ImageView(imagePath);
        backImage.setFitWidth(60);
        backImage.setFitHeight(80);
        return new StackPane(backImage);
    }

    private void endGame(String winnerName) {
        SoundManager.stopBGM();
        if (winnerName.equals(myName)) {
            SoundManager.playWin();
        } else {
            SoundManager.playError();
        }

        GameLogger.logResult(winnerName, numberOfPlayers, playerHand.size(), playerNames); // Log game result
        gameStatusLabel.setText("🏆 " + winnerName + " đã thắng!");

        replayButton.setVisible(true);
        replayButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("Game.fxml"));
                Scene gameScene = new Scene(loader.load());
                Stage stage = (Stage) currentCardPane.getScene().getWindow();
                stage.setScene(gameScene);
                stage.setTitle("UNO - Ván mới");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> gameStatusLabel.setText("👉 Nhấn 'Chơi lại' để bắt đầu ván mới."));
        delay.play();
    }

    public void resetGame() {
        deck = new UnoDeck();
        playerHand.clear();
        ai2Hand.clear();
        ai3Hand.clear();
        ai4Hand.clear();
        currentPlayer = 1;
        direction = 1;
        unoCalled = false;
        comboType = null;
        penaltyStack = 0;
        aiUno = new boolean[5];

        bottomPlayer.getChildren().clear();
        topPlayer.getChildren().clear();
        leftPlayer.getChildren().clear();
        rightPlayer.getChildren().clear();
        gameStatusLabel.setText("");
        replayButton.setVisible(false);

        updateDeckCount();
        for (int i = 0; i < 7; i++) {
            UnoCard card = deck.drawCard();
            playerHand.add(card);
            addCardToHand(card);
        }
        currentCard = deck.drawCard();
        updateCurrentCardView(currentCard);
    }

    @FXML
    private void handleExit() {
        Stage stage = (Stage) exitButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleReplay() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Game.fxml"));
            Scene gameScene = new Scene(loader.load());
            Game newGameController = loader.getController();
            newGameController.initPlayers(numberOfPlayers, playerNames);
            newGameController.resetGame();
            Stage stage = (Stage) currentCardPane.getScene().getWindow();
            stage.setScene(gameScene);
            stage.setTitle("UNO - Ván mới");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean hasValidCard() {
        for (UnoCard card : playerHand) {
            if (comboType != null) {
                if (card.getValue() == comboType) return true;
            } else {
                if (card.getColor() == currentCard.getColor()
                        || card.getValue() == currentCard.getValue()
                        || card.getColor() == UnoCard.Color.Wild) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showNoPlayableCardNotification() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Không có lá bài hợp lệ");
        alert.setHeaderText(null);
        alert.setContentText("⚠️ Bạn không có lá bài nào phù hợp.\nVui lòng bốc bài.");
        alert.showAndWait();
    }
}