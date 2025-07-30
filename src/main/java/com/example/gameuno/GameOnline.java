package com.example.gameuno;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private int activePlayers;

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

    private void showEndGameScreen(String winnerName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("EndGameScreen.fxml"));
            StackPane root = loader.load();
            EndGameController controller = loader.getController();
            controller.setWinner(winnerName);

            Stage stage = new Stage();
            stage.setTitle("Kết thúc game");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initPlayers(int numberOfPlayers, List<String> playerNames, UnoClientConnection client, String myName, List<UnoCard> myInitialHand, UnoCard firstCard) {
        this.numberOfPlayers = numberOfPlayers;
        this.playerNames = new ArrayList<>(playerNames);
        this.activePlayers = playerNames.size();
        this.client = client;
        this.myName = myName;
        this.myIndex = playerNames.indexOf(myName);

        GameLogger.startGame(playerNames);

        myHand.addAll(myInitialHand);
        myInitialHand.forEach(this::addCardToHand);

        currentCard = firstCard;
        updateCurrentCardView(firstCard);

        updatePlayerVisibility();
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

    private void updatePlayerVisibility() {
        leftPlayerContainer.setVisible(numberOfPlayers >= 3);
        rightPlayerContainer.setVisible(numberOfPlayers == 4);
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
                Platform.runLater(() -> showGameDialog("❌ Mất kết nối: " + e.getMessage(), false, myName));
            }
        }).start();
    }

    private void showGameDialog(String message, boolean isWin, String winnerName) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(exitButton.getScene().getWindow());
        dialog.setTitle(isWin ? "Chiến thắng!" : "Thua cuộc");

        StackPane dialogPane = new StackPane();
        dialogPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");
        Label messageLabel = new Label();
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setWrapText(true);
        messageLabel.setText(message);
        messageLabel.setStyle(isWin ? "-fx-font-size: 24px; -fx-text-fill: gold;" : "-fx-font-size: 22px; -fx-text-fill: red;");

        Button okButton = new Button("Quay lại menu");
        okButton.setStyle("-fx-font-size: 16px; -fx-padding: 10px 20px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5px;");
        okButton.setOnAction(e -> {
            dialog.close();
            handleExit();
        });

        VBox vbox = new VBox(20, messageLabel, okButton);
        vbox.setAlignment(Pos.CENTER);
        dialogPane.getChildren().add(vbox);
        Scene dialogScene = new Scene(dialogPane, 400, 250);
        dialog.setScene(dialogScene);
        dialog.setResizable(false);
        dialog.show();
    }

    private void showEndGameDialog(String winnerName) {
        boolean isWin = winnerName.equals(myName);
        String message = isWin ? "🏆 Bạn đã chiến thắng!" : "😢 Bạn đã thua.";
        GameLogger.logResult(winnerName, numberOfPlayers, myHand.size(), playerNames);
        showGameDialog(message, isWin, winnerName);
    }

    private void showNoCardsNotification() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(exitButton.getScene().getWindow());
        dialog.setTitle("Chiến thắng!");

        StackPane dialogPane = new StackPane();
        dialogPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");
        Label messageLabel = new Label();
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setWrapText(true);
        messageLabel.setText("🏆 Bạn đã thắng!");
        messageLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: gold;");

        Button backButton = new Button("Quay về");
        backButton.setStyle("-fx-font-size: 16px; -fx-padding: 10px 20px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5px;");
        backButton.setOnAction(e -> {
            dialog.close();
            handleExit();
        });

        VBox vbox = new VBox(20, messageLabel, backButton);
        vbox.setAlignment(Pos.CENTER);
        dialogPane.getChildren().add(vbox);
        Scene dialogScene = new Scene(dialogPane, 400, 250);
        dialog.setScene(dialogScene);
        dialog.setResizable(false);
        dialog.show();

        checkGameEndCondition();
    }

    private void checkGameEndCondition() {
        if (playerNames.size() == 1 && myHand.isEmpty()) {
            showEndGameDialog(myName);
            gameOver = true;
        }
    }

    private void handleServer(String msg) {
        if (gameOver) return;

        if (msg.startsWith("PLAY_CARD:")) {
            try {
                String[] parts = msg.split(":", 2);
                String[] payload = parts[1].split(" ", 2);
                if (payload.length != 2) return;

                String name = payload[0];
                String[] cardData = payload[1].split(",");
                if (cardData.length != 2) return;

                UnoCard card = new UnoCard(UnoCard.Color.valueOf(cardData[0]), UnoCard.Value.valueOf(cardData[1]));
                GameLogger.logMove(name, "đã đánh lá " + card.getColor() + " " + card.getValue());

                int idx = playerNames.indexOf(name);
                if (idx == myIndex) {
                    if (pendingCard != null && pendingButton != null) {
                        myHand.remove(pendingCard);
                        bottomPlayer.getChildren().remove(pendingButton);
                        pendingCard = null;
                        pendingButton = null;
                        System.out.println("📊 Cập nhật myHand: " + myHand.size() + " lá");

                        if (myHand.isEmpty()) {
                            showNoCardsNotification();
                            client.send("GAME_OVER:" + myName);
                            gameOver = true;
                        }
                    }
                } else {
                    if (!opponentHands[idx].isEmpty()) {
                        opponentHands[idx].remove(0);
                        removeFaceDown(idx);
                        System.out.println("📊 Cập nhật opponentHands[" + idx + "]: " + opponentHands[idx].size() + " lá");
                    }

                    if (card.getValue() == UnoCard.Value.DrawTwo || card.getValue() == UnoCard.Value.WildDrawFour) {
                        drawStackType = card.getValue();
                        drawStack += (card.getValue() == UnoCard.Value.DrawTwo) ? 2 : 4;
                    } else if (card.getValue() == UnoCard.Value.Skip) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Thông báo");
                            alert.setHeaderText(null);
                            alert.setContentText("🚫 " + name + " đã bỏ lượt 1 người chơi!");
                            alert.showAndWait();
                        });
                    } else if (card.getValue() == UnoCard.Value.Reverse) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Thông báo");
                            alert.setHeaderText(null);
                            alert.setContentText("🔄 " + name + " đã đảo ngược hướng chơi!");
                            alert.showAndWait();
                        });
                    } else {
                        drawStack = 0;
                        drawStackType = null;
                    }
                }

                updateCurrentCardView(card);
                updatePlayerLabels();
            } catch (Exception e) {
                showGameDialog("❗ Lỗi xử lý PLAY_CARD", false, myName);
            }
        } else if (msg.startsWith("DRAW_CARD:")) {
            String[] parts = msg.split(":");
            if (parts.length < 4) {
                System.out.println("❗ Lỗi định dạng DRAW_CARD: " + msg);
                return;
            }
            String name = parts[1];
            int drawCount = Integer.parseInt(parts[2]);
            String[] cardData = parts[3].split(";");
            GameLogger.logMove(name, "đã bốc " + drawCount + " lá");

            int idx = playerNames.indexOf(name);
            if (idx == myIndex) {
                for (String cardStr : cardData) {
                    String[] cardParts = cardStr.split(",");
                    UnoCard drawn = new UnoCard(UnoCard.Color.valueOf(cardParts[0]), UnoCard.Value.valueOf(cardParts[1]));
                    myHand.add(drawn);
                    addCardToHand(drawn);
                    System.out.println("📊 Cập nhật myHand: " + myHand.size() + " lá");
                }
                drawStack = 0;
                drawStackType = null;

                if (myHand.size() > 15) {
                    String winner = playerNames.get((myIndex + 1) % numberOfPlayers);
                    showEndGameDialog(winner);
                    client.send("GAME_OVER:" + winner);
                    gameOver = true;
                }
            } else {
                for (int i = 0; i < drawCount; i++) {
                    opponentHands[idx].add(null);
                    addFaceDown(idx);
                    System.out.println("📊 Cập nhật opponentHands[" + idx + "]: " + opponentHands[idx].size() + " lá");
                }
            }
            updatePlayerLabels();
        } else if (msg.startsWith("PLAYER_CARD_COUNT:")) {
            String[] parts = msg.split(":");
            if (parts.length != 3) {
                System.out.println("❗ Lỗi định dạng PLAYER_CARD_COUNT: " + msg);
                return;
            }
            String name = parts[1];
            int cardCount = Integer.parseInt(parts[2]);
            int idx = playerNames.indexOf(name);
            if (idx == myIndex) {
                if (myHand.size() != cardCount) {
                    System.out.println("⚠️ Số lá bài không đồng bộ với server: myHand=" + myHand.size() + ", server=" + cardCount);
                }
            } else if (idx != -1) {
                while (opponentHands[idx].size() < cardCount) {
                    opponentHands[idx].add(null);
                    addFaceDown(idx);
                    System.out.println("📊 Cập nhật opponentHands[" + idx + "]: " + opponentHands[idx].size() + " lá");
                }
                while (opponentHands[idx].size() > cardCount) {
                    opponentHands[idx].remove(0);
                    removeFaceDown(idx);
                    System.out.println("📊 Cập nhật opponentHands[" + idx + "]: " + opponentHands[idx].size() + " lá");
                }
            }
            updatePlayerLabels();
        } else if (msg.startsWith("CALL_UNO:")) {
            String name = msg.split(":")[1];
            GameLogger.logMove(name, "đã kêu UNO!");
        } else if (msg.startsWith("GAME_OVER:")) {
            String winner = msg.split(":")[1];
            GameLogger.logResult(winner, numberOfPlayers, myHand.size(), playerNames);
            showEndGameDialog(winner);
            gameOver = true;
        } else if (msg.startsWith("CURRENT_PLAYER:")) {
            int serverPlayer = Integer.parseInt(msg.split(":")[1]);
            currentPlayer = serverPlayer - 1;
            boolean isMyTurn = (currentPlayer == myIndex);

            currentPlayerLabel.setText("Lượt: " + playerNames.get(currentPlayer));
            gameStatusLabel.setText(isMyTurn ? "👉 Tới lượt bạn!" : "👉 Tới lượt " + playerNames.get(currentPlayer));
            gameStatusLabel.setStyle("");

            if (isMyTurn) {
                if (myHand.size() > 15) {
                    String winner = playerNames.get((myIndex + 1) % numberOfPlayers);
                    showEndGameDialog(winner);
                    client.send("GAME_OVER:" + winner);
                    gameOver = true;
                } else if (drawStack > 0 && !hasStackableCard()) {
                    gameStatusLabel.setText("💥 Bạn bị cộng " + drawStack + " lá!");
                    client.send("DRAW_CARD:" + myName);
                } else if (drawStack == 0 && !hasPlayableCard()) {
                    gameStatusLabel.setText("⚠️ Bạn không có lá bài hợp lệ!");
                    showNoPlayableCardNotification();
                }
            }
        } else if (msg.startsWith("CURRENT_CARD:")) {
            String[] p = msg.split(":")[1].split(",");
            UnoCard card = new UnoCard(UnoCard.Color.valueOf(p[0]), UnoCard.Value.valueOf(p[1]));
            updateCurrentCardView(card);
        } else if (msg.startsWith("NO_CARDS:")) {
            String name = msg.split(":")[1];
            if (name.equals(myName)) {
                showNoCardsNotification();
            }
        } else if (msg.startsWith("PLAYER_LEFT:")) {
            String leftPlayer = msg.split(":")[1];
            int idx = playerNames.indexOf(leftPlayer);
            if (idx != -1) {
                playerNames.remove(leftPlayer);
                activePlayers--;
                numberOfPlayers--;
                opponentHands[idx].clear();
                removeFaceDownAll(idx);
                updatePlayerVisibility();
                updatePlayerLabels();
                System.out.println("📊 Player left: " + leftPlayer + ", Active players: " + activePlayers);
                checkGameEndCondition();
            }
        }
    }

    private void removeFaceDownAll(int idx) {
        int relative = (idx - myIndex + numberOfPlayers) % numberOfPlayers;
        if (relative == 1) topPlayer.getChildren().clear();
        else if (relative == 2) leftPlayer.getChildren().clear();
        else if (relative == 3) rightPlayer.getChildren().clear();
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
            gameStatusLabel.setStyle("");
            return;
        }
        GameLogger.logMove(myName, "đã bốc bài");
        client.send("DRAW_CARD:" + myName);
    }

    @FXML
    private void callUno() {
        if (gameOver) return;
        GameLogger.logMove(myName, "đã kêu UNO!");
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
            gameStatusLabel.setStyle("");
            return;
        }

        boolean valid = card.getColor() == currentCard.getColor()
                || card.getValue() == currentCard.getValue()
                || card.getColor() == UnoCard.Color.Wild
                || (drawStack > 0 && card.getValue() == drawStackType);

        if (!valid) {
            showInvalidCardNotification();
            return;
        }

        GameLogger.logMove(myName, "đã đánh lá " + card.getColor() + " " + card.getValue());
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
        topPlayerLabel.setText(playerNames.get((myIndex + 1) % playerNames.size()) + " (" + opponentHands[(myIndex + 1) % playerNames.size()].size() + ")");
        if (numberOfPlayers >= 3)
            leftPlayerLabel.setText(playerNames.get((myIndex + 2) % playerNames.size()) + " (" + opponentHands[(myIndex + 2) % playerNames.size()].size() + ")");
        else
            leftPlayerLabel.setText("");
        if (numberOfPlayers == 4)
            rightPlayerLabel.setText(playerNames.get((myIndex + 3) % playerNames.size()) + " (" + opponentHands[(myIndex + 3) % playerNames.size()].size() + ")");
        else
            rightPlayerLabel.setText("");
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

    private void showInvalidCardNotification() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("CẢNH BÁO");
        alert.setHeaderText("KHÔNG CÓ LÁ BÀI HỢP LỆ");
        alert.setContentText("Bạn không có lá bài nào phù hợp với lá bài hiện tại.\nVui lòng bốc thêm bài hoặc nhấn UNO nếu chỉ còn 1 lá!");
        alert.showAndWait();
    }

    private boolean hasPlayableCard() {
        for (UnoCard card : myHand) {
            boolean valid = card.getColor() == currentCard.getColor()
                    || card.getValue() == currentCard.getValue()
                    || card.getColor() == UnoCard.Color.Wild
                    || (drawStack > 0 && card.getValue() == drawStackType);
            if (valid) return true;
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