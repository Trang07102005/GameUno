package com.example.gameuno;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Game {

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

    private List<String> playerNames; // Danh sách tên (bao gồm tên thật + tên bot)
    private String myName;             // Tên người chơi thật

    @FXML
    public void initialize() {
        deck = new UnoDeck();
        playerHand = new ArrayList<>();

        // 👉 Đảm bảo kích thước đồng nhất với thẻ hiện tại
        double cardWidth = 60;   // Giống currentCardImage
        double cardHeight = 100; // Giống currentCardImage

        // Gắn hình mặt sau cho nút bộ bài
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


    /** Khởi tạo game với tên & số người chơi **/
    public void initPlayers(int numberOfPlayers, List<String> playerNames) {
        this.numberOfPlayers = numberOfPlayers;
        this.playerNames = playerNames;
        this.myName = playerNames.get(0); // Tên thật là người đầu tiên

        unoCalled = false;

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

    /** Nút bốc bài **/
    @FXML
    private void drawCard() {
        if (currentPlayer != 1) {
            gameStatusLabel.setText("❌ Không phải lượt của bạn!");
            return;
        }
        UnoCard card = deck.drawCard();
        playerHand.add(card);
        addCardToHand(card);
        updateDeckCount();
        unoCalled = false;
        gameStatusLabel.setText("🃏 " + myName + " đã rút 1 lá.");
        nextTurn();
    }

    /** Nút gọi UNO **/
    @FXML
    private void callUno() {
        unoCalled = true;
        gameStatusLabel.setText("🗣️ " + myName + " đã kêu UNO!");
    }

    /** Thêm bài vào tay người chơi **/
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

    /** Đánh bài **/
    private void playCard(UnoCard card, Button cardBtn) {
        if (currentPlayer != 1) {
            gameStatusLabel.setText("❌ Không phải lượt của bạn!");
            return;
        }

        if (card.getColor() == currentCard.getColor()
                || card.getValue() == currentCard.getValue()
                || card.getColor() == UnoCard.Color.Wild) {

            if (card.getValue() == UnoCard.Value.Wild || card.getValue() == UnoCard.Value.WildDrawFour) {
                handleWild(card);
            }

            if (card.getValue() == UnoCard.Value.DrawTwo) {
                givePenalty(getNextPlayer(), 2);
                skipNext();
            } else if (card.getValue() == UnoCard.Value.Skip) {
                skipNext();
            } else if (card.getValue() == UnoCard.Value.Reverse) {
                direction *= -1;
                gameStatusLabel.setText("🔄 Đã đảo chiều!");
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
                gameStatusLabel.setText("⚠️ " + myName + " quên kêu UNO! Bị phạt 2 lá!");
            } else if (playerHand.isEmpty()) {
                gameStatusLabel.setText("🏆 " + myName + " đã thắng!");
                return;
            }

            unoCalled = false;
            nextTurn();

        } else {
            gameStatusLabel.setText("❌ Thẻ không hợp lệ!");
        }
    }

    /** Xử lý Wild và WildDrawFour **/
    private void handleWild(UnoCard card) {
        List<String> options = List.of("Red", "Yellow", "Green", "Blue");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Red", options);
        dialog.setTitle("Chọn màu");
        dialog.setHeaderText(myName + " đã đánh Wild");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(color -> {
            switch (color) {
                case "Red" -> card.setDynamicColor(UnoCard.Color.Red);
                case "Yellow" -> card.setDynamicColor(UnoCard.Color.Yellow);
                case "Green" -> card.setDynamicColor(UnoCard.Color.Green);
                case "Blue" -> card.setDynamicColor(UnoCard.Color.Blue);
            }
            gameStatusLabel.setText("🎨 Đã đổi màu thành " + color);
        });

        if (card.getValue() == UnoCard.Value.WildDrawFour) {
            givePenalty(getNextPlayer(), 4);
            skipNext();
        }
    }

    /** Phạt bốc bài **/
    private void givePenalty(int playerIndex, int cards) {
        List<UnoCard> hand = getHand(playerIndex);
        for (int i = 0; i < cards; i++) {
            hand.add(deck.drawCard());
            addFaceDown(playerIndex);
        }
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

    /** Chuyển lượt **/
    private void nextTurn() {
        currentPlayer = getNextPlayer();
        if (currentPlayer == 1) {
            gameStatusLabel.setText("👉 Tới lượt " + myName);
            currentPlayerLabel.setText("Lượt: " + myName);
        } else {
            currentPlayerLabel.setText("Lượt: " + playerNames.get(currentPlayer - 1));
            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e -> aiTurn(currentPlayer));
            delay.play();
        }
    }

    /** Lượt AI **/
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
            if (chosen.getValue() == UnoCard.Value.Wild || chosen.getValue() == UnoCard.Value.WildDrawFour) {
                UnoCard.Color[] colors = UnoCard.Color.values();
                UnoCard.Color picked = colors[(int) (Math.random() * 4)];
                chosen.setDynamicColor(picked);
                gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " đổi màu thành " + picked);
            }
            currentCard = chosen;
            updateCurrentCardView(chosen);
            gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " đánh " + chosen);

            if (hand.size() == 1 && !aiUno[index]) {
                aiUno[index] = true;
                gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " kêu UNO!");
                PauseTransition unoPause = new PauseTransition(Duration.seconds(1));
                unoPause.setOnFinished(e -> nextTurn());
                unoPause.play();
                return;
            }
            if (hand.isEmpty()) {
                gameStatusLabel.setText("🏆 " + playerNames.get(index - 1) + " đã thắng!");
                return;
            }

        } else {
            UnoCard drawn = deck.drawCard();
            hand.add(drawn);
            addFaceDown(index);
            gameStatusLabel.setText("🤖 " + playerNames.get(index - 1) + " bốc 1 lá");
        }

        updatePlayerLabels();
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> nextTurn());
        delay.play();
    }

    /** Cập nhật tên + số bài của mọi người **/
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
}
