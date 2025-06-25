package com.example.gameuno;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class GameOnline {
    @FXML private Label currentPlayerLabel, gameStatusLabel, bottomPlayerLabel, topPlayerLabel, leftPlayerLabel, rightPlayerLabel;
    @FXML private Button drawCardButton, unoButton, exitButton;
    @FXML private ImageView currentCardImage;
    @FXML private HBox bottomPlayer, topPlayer;
    @FXML private VBox leftPlayerContainer, leftPlayer, rightPlayerContainer, rightPlayer;

    private UnoClientConnection client;
    private UnoDeck localDeck = new UnoDeck();
    private UnoCard currentCard;

    private final List<UnoCard> myHand = new ArrayList<>();
    private final List<UnoCard>[] opponentHands = new List[4];

    private List<String> playerNames;
    private String myName;
    private int myIndex;
    private int numberOfPlayers;
    private int currentPlayer;

    private UnoCard pendingCard;
    private Button pendingButton;

    private int drawStack = 0;
    private UnoCard.Value drawStackType = null;

    private boolean gameOver = false;

    @FXML
    public void initialize() {
        for (int i = 0; i < 4; i++) opponentHands[i] = new ArrayList<>();

        Image img = new Image(getClass().getResource("/cards/Back.png").toExternalForm());
        ImageView imgView = new ImageView(img);
        imgView.setFitWidth(60);
        imgView.setFitHeight(80);
        drawCardButton.setGraphic(imgView);
    }

    public void initPlayers(int numberOfPlayers, List<String> playerNames, UnoClientConnection client, String myName, List<UnoCard> myInitialHand, UnoCard firstCard) {
        this.numberOfPlayers = numberOfPlayers;
        this.playerNames = playerNames;
        this.client = client;
        this.myName = myName;
        this.myIndex = playerNames.indexOf(myName);

        myHand.addAll(myInitialHand);
        myInitialHand.forEach(this::addCardToHand);

        currentCard = firstCard;
        updateCurrentCardView(firstCard);

        leftPlayerContainer.setVisible(numberOfPlayers >= 3);
        rightPlayerContainer.setVisible(numberOfPlayers == 4);

        for (int i = 0; i < numberOfPlayers; i++) {
            if (i != myIndex) {
                for (int j = 0; j < 7; j++) {
                    opponentHands[i].add(null);
                    addFaceDown(i);
                }
            }
        }

        updatePlayerLabels();
        listenToServer();
    }

    private void listenToServer() {
        new Thread(() -> {
            try {
                String line;
                while ((line = client.getIn().readLine()) != null) {
                    String msg = line;
                    Platform.runLater(() -> handleServer(msg));
                }
            } catch (IOException e) {
                Platform.runLater(() -> gameStatusLabel.setText("❌ Mất kết nối: " + e.getMessage()));
            }
        }).start();
    }

    private void handleServer(String msg) {
        if (gameOver) return;

        if (msg.startsWith("CURRENT_PLAYER:")) {
            int serverPlayer = Integer.parseInt(msg.split(":" )[1]);
            currentPlayer = serverPlayer - 1;
            currentPlayerLabel.setText("Lượt: " + playerNames.get(currentPlayer));
            if (currentPlayer == myIndex) {
                gameStatusLabel.setText("👉 Tới lượt bạn!");
                if (myHand.size() > 15) {
                    gameStatusLabel.setText("❌ Bạn có quá 15 lá → Bị xử thua!");
                    client.send("GAME_OVER:" + playerNames.get((myIndex + 1) % numberOfPlayers));
                    gameOver = true;
                } else if (drawStack > 0 && !hasStackableCard()) {
                    gameStatusLabel.setText("💥 Bạn bị cộng " + drawStack + " lá!");
                    client.send("DRAW_CARD:" + myName);
                }
            } else {
                gameStatusLabel.setText("👉 Tới lượt " + playerNames.get(currentPlayer));
            }
        } else if (msg.startsWith("CURRENT_CARD:")) {
            String[] p = msg.split(":" )[1].split(",");
            UnoCard card = new UnoCard(UnoCard.Color.valueOf(p[0]), UnoCard.Value.valueOf(p[1]));
            updateCurrentCardView(card);
        } else if (msg.startsWith("PLAY_CARD:")) {
            try {
                String[] parts = msg.split(":", 2);
                String[] payload = parts[1].split(" ", 2);
                if (payload.length != 2) return;

                String name = payload[0];
                String[] cardData = payload[1].split(",");
                if (cardData.length != 2) return;

                UnoCard card = new UnoCard(UnoCard.Color.valueOf(cardData[0]), UnoCard.Value.valueOf(cardData[1]));

                if (card.getValue() == UnoCard.Value.DrawTwo || card.getValue() == UnoCard.Value.WildDrawFour) {
                    drawStackType = card.getValue();
                    drawStack += (card.getValue() == UnoCard.Value.DrawTwo) ? 2 : 4;
                } else {
                    drawStack = 0;
                    drawStackType = null;
                }

                int idx = playerNames.indexOf(name);
                if (idx == myIndex) {
                    if (pendingCard != null && pendingButton != null) {
                        myHand.remove(pendingCard);
                        bottomPlayer.getChildren().remove(pendingButton);
                        pendingCard = null;
                        pendingButton = null;

                        if (myHand.isEmpty()) {
                            gameStatusLabel.setText("🏆 Bạn đã thắng ván này!");
                            client.send("GAME_OVER:" + myName);
                            gameOver = true;
                        }
                    }
                } else {
                    if (!opponentHands[idx].isEmpty()) {
                        opponentHands[idx].remove(0);
                        removeFaceDown(idx);
                    }
                }

                updateCurrentCardView(card);
                updatePlayerLabels();
            } catch (Exception e) {
                gameStatusLabel.setText("❗ Lỗi xử lý PLAY_CARD");
                e.printStackTrace();
            }
        } else if (msg.startsWith("DRAW_CARD:")) {
            String name = msg.split(":" )[1];
            int idx = playerNames.indexOf(name);
            int drawCount = (drawStack > 0) ? drawStack : 1;
            if (idx == myIndex) {
                for (int i = 0; i < drawCount; i++) {
                    UnoCard drawn = localDeck.drawCard();
                    myHand.add(drawn);
                    addCardToHand(drawn);
                }
                drawStack = 0;
                drawStackType = null;

                if (myHand.size() > 15) {
                    gameStatusLabel.setText("❌ Bạn có quá 15 lá → Bị xử thua!");
                    client.send("GAME_OVER:" + playerNames.get((myIndex + 1) % numberOfPlayers));
                    gameOver = true;
                }
            } else {
                for (int i = 0; i < drawCount; i++) {
                    opponentHands[idx].add(null);
                    addFaceDown(idx);
                }
            }
            updatePlayerLabels();
        } else if (msg.startsWith("CALL_UNO:")) {
            String name = msg.split(":" )[1];
            gameStatusLabel.setText("🗣️ " + name + " đã kêu UNO!");
        } else if (msg.startsWith("GAME_OVER:")) {
            String winner = msg.split(":" )[1];
            gameStatusLabel.setText("🏆 " + winner + " đã thắng!");
            gameOver = true;
        }
    }

    private boolean hasStackableCard() {
        for (UnoCard c : myHand) {
            if (c.getValue() == drawStackType) return true;
        }
        return false;
    }

    @FXML
    private void drawCard() {
        if (gameOver) return;
        if (currentPlayer != myIndex) {
            gameStatusLabel.setText("❌ Không phải lượt của bạn!");
            return;
        }
        client.send("DRAW_CARD:" + myName);
    }

    @FXML
    private void callUno() {
        if (gameOver) return;
        client.send("CALL_UNO:" + myName);
    }

    private void addCardToHand(UnoCard card) {
        ImageView img = new ImageView(getClass().getResource(card.getImagePath()).toExternalForm());
        img.setFitWidth(60);
        img.setFitHeight(80);
        Button btn = new Button();
        btn.setGraphic(img);
        btn.setStyle("-fx-background-color: transparent;");
        btn.setOnAction(e -> playCard(card, btn));
        bottomPlayer.getChildren().add(btn);
    }

    private void playCard(UnoCard card, Button btn) {
        if (gameOver) return;
        if (currentPlayer != myIndex) {
            gameStatusLabel.setText("❌ Không phải lượt của bạn!");
            return;
        }

        boolean valid = card.getColor() == currentCard.getColor()
                || card.getValue() == currentCard.getValue()
                || card.getColor() == UnoCard.Color.Wild
                || (drawStack > 0 && card.getValue() == drawStackType);

        if (!valid) {
            gameStatusLabel.setText("❌ Thẻ không hợp lệ!");
            return;
        }

        if (card.getValue() == UnoCard.Value.Wild || card.getValue() == UnoCard.Value.WildDrawFour) {
            List<String> options = List.of("Red", "Yellow", "Green", "Blue");
            ChoiceDialog<String> dialog = new ChoiceDialog<>("Red", options);
            dialog.setHeaderText("Chọn màu Wild");
            dialog.showAndWait().ifPresent(color -> {
                card.setDynamicColor(UnoCard.Color.valueOf(color));
                pendingCard = card;
                pendingButton = btn;
                client.send("PLAY_CARD:" + myName + " " + card.getColor() + "," + card.getValue());
            });
        } else {
            pendingCard = card;
            pendingButton = btn;
            client.send("PLAY_CARD:" + myName + " " + card.getColor() + "," + card.getValue());
        }
    }

    private void addFaceDown(int idx) {
        int relative = (idx - myIndex + numberOfPlayers) % numberOfPlayers;
        StackPane card = createFaceDown();
        if (relative == 1) topPlayer.getChildren().add(card);
        else if (relative == 2) leftPlayer.getChildren().add(card);
        else if (relative == 3) rightPlayer.getChildren().add(card);
    }

    private void removeFaceDown(int idx) {
        int relative = (idx - myIndex + numberOfPlayers) % numberOfPlayers;
        if (relative == 1 && !topPlayer.getChildren().isEmpty()) topPlayer.getChildren().remove(0);
        else if (relative == 2 && !leftPlayer.getChildren().isEmpty()) leftPlayer.getChildren().remove(0);
        else if (relative == 3 && !rightPlayer.getChildren().isEmpty()) rightPlayer.getChildren().remove(0);
    }

    private StackPane createFaceDown() {
        ImageView img = new ImageView(getClass().getResource("/cards/Back.png").toExternalForm());
        img.setFitWidth(60);
        img.setFitHeight(80);
        return new StackPane(img);
    }

    private void updatePlayerLabels() {
        bottomPlayerLabel.setText(myName + " (" + myHand.size() + ")");
        topPlayerLabel.setText(playerNames.get((myIndex + 1) % numberOfPlayers) + " (" + opponentHands[(myIndex + 1) % numberOfPlayers].size() + ")");
        if (numberOfPlayers >= 3)
            leftPlayerLabel.setText(playerNames.get((myIndex + 2) % numberOfPlayers) + " (" + opponentHands[(myIndex + 2) % numberOfPlayers].size() + ")");
        if (numberOfPlayers == 4)
            rightPlayerLabel.setText(playerNames.get((myIndex + 3) % numberOfPlayers) + " (" + opponentHands[(myIndex + 3) % numberOfPlayers].size() + ")");
    }

    private void updateCurrentCardView(UnoCard card) {
        if (card.getValue() != UnoCard.Value.Wild && card.getValue() != UnoCard.Value.WildDrawFour) {
            card.resetColor();
        }
        currentCard = card;
        currentCardImage.setImage(new Image(getClass().getResource(card.getImagePath()).toExternalForm()));
    }

    @FXML
    private void handleExit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gameuno/Menu.fxml"));
            Stage stage = (Stage) exitButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("UNO - Menu");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}