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
import javafx.stage.Modality;
import javafx.geometry.Pos;

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
        // Khởi tạo danh sách bài của đối thủ
        for (int i = 0; i < 4; i++) opponentHands[i] = new ArrayList<>();

        // Thiết lập hình ảnh cho nút bốc bài
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

        // Khởi tạo bài cho đối thủ
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
        showGameDialog(message, isWin, winnerName);
    }

    private void showNoCardsNotification() {
        // Hiển thị thông báo khi không còn lá bài nào
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText("🎉 Bạn không còn lá bài! Chờ kết quả ván đấu...");
        alert.showAndWait();
    }

    private void handleServer(String msg) {
        if (gameOver) return;

        // --------- XỬ LÝ LƯỢT CHƠI HIỆN TẠI ---------
        if (msg.startsWith("CURRENT_PLAYER:")) {
            int serverPlayer = Integer.parseInt(msg.split(":")[1]);
            currentPlayer = serverPlayer - 1;
            boolean isMyTurn = (currentPlayer == myIndex);

            currentPlayerLabel.setText("Lượt: " + playerNames.get(currentPlayer));
            gameStatusLabel.setText(isMyTurn ? "👉 Tới lượt bạn!" : "👉 Tới lượt " + playerNames.get(currentPlayer));
            gameStatusLabel.setStyle("");

            if (isMyTurn) {
                // Kiểm tra thua vì quá số lá
                if (myHand.size() > 15) {
                    String winner = playerNames.get((myIndex + 1) % numberOfPlayers);
                    showEndGameDialog(winner);
                    client.send("GAME_OVER:" + winner);
                    gameOver = true;
                }

                // Nếu bị cộng dồn mà không có bài chồng
                else if (drawStack > 0 && !hasStackableCard()) {
                    gameStatusLabel.setText("💥 Bạn bị cộng " + drawStack + " lá!");
                    client.send("DRAW_CARD:" + myName);
                }

                // ✅ Nếu không bị cộng dồn mà không có bài hợp lệ
                else if (drawStack == 0 && !hasPlayableCard()) {
                    gameStatusLabel.setText("⚠️ Bạn không có lá bài hợp lệ!");
                    showNoPlayableCardNotification();
                }
            }
        }

        // --------- CẬP NHẬT BÀI HIỆN TẠI ---------
        else if (msg.startsWith("CURRENT_CARD:")) {
            String[] p = msg.split(":")[1].split(",");
            UnoCard card = new UnoCard(UnoCard.Color.valueOf(p[0]), UnoCard.Value.valueOf(p[1]));
            updateCurrentCardView(card);
        }

        // --------- NGƯỜI CHƠI ĐÁNH BÀI ---------
        else if (msg.startsWith("PLAY_CARD:")) {
            try {
                String[] parts = msg.split(":", 2);
                String[] payload = parts[1].split(" ", 2);
                if (payload.length != 2) return;

                String name = payload[0];
                String[] cardData = payload[1].split(",");
                if (cardData.length != 2) return;

                UnoCard card = new UnoCard(UnoCard.Color.valueOf(cardData[0]), UnoCard.Value.valueOf(cardData[1]));

                int idx = playerNames.indexOf(name);
                if (idx == myIndex) {
                    // Người chơi là mình
                    if (pendingCard != null && pendingButton != null) {
                        myHand.remove(pendingCard);
                        bottomPlayer.getChildren().remove(pendingButton);
                        pendingCard = null;
                        pendingButton = null;

                        if (myHand.isEmpty()) {
                            showNoCardsNotification();
                            client.send("GAME_OVER:" + myName);
                        }
                    }
                } else {
                    // Người chơi là đối thủ
                    if (!opponentHands[idx].isEmpty()) {
                        opponentHands[idx].remove(0);
                        removeFaceDown(idx);
                    }

                    // ✅ Chỉ cộng dồn khi người đánh không phải mình
                    if (card.getValue() == UnoCard.Value.DrawTwo || card.getValue() == UnoCard.Value.WildDrawFour) {
                        drawStackType = card.getValue();
                        drawStack += (card.getValue() == UnoCard.Value.DrawTwo) ? 2 : 4;
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
        }


        // --------- NGƯỜI CHƠI BỐC BÀI ---------
        else if (msg.startsWith("DRAW_CARD:")) {
            String name = msg.split(":")[1];
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
                    String winner = playerNames.get((myIndex + 1) % numberOfPlayers);
                    showEndGameDialog(winner);
                    client.send("GAME_OVER:" + winner);
                    gameOver = true;
                }
            } else {
                for (int i = 0; i < drawCount; i++) {
                    opponentHands[idx].add(null);
                    addFaceDown(idx);
                }
            }

            updatePlayerLabels();
        }

        // --------- NGƯỜI CHƠI KÊU UNO ---------
        else if (msg.startsWith("CALL_UNO:")) {
            String name = msg.split(":")[1];
            gameStatusLabel.setText("🗣️ " + name + " đã kêu UNO!");
            gameStatusLabel.setStyle("");
        }

        // --------- GAME KẾT THÚC ---------
        else if (msg.startsWith("GAME_OVER:")) {
            String winner = msg.split(":")[1];
            showEndGameDialog(winner); // ✅ dùng giao diện mới
            gameOver = true;
        }

        // --------- THÔNG BÁO HẾT BÀI ---------
        else if (msg.startsWith("NO_CARDS:")) {
            String name = msg.split(":")[1];
            if (name.equals(myName)) {
                showNoCardsNotification();
            }
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
            gameStatusLabel.setStyle(""); // Reset style
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
            gameStatusLabel.setStyle(""); // Reset style
            return;
        }

        boolean valid = card.getColor() == currentCard.getColor()
                || card.getValue() == currentCard.getValue()
                || card.getColor() == UnoCard.Color.Wild
                || (drawStack > 0 && card.getValue() == drawStackType);

        if (!valid) {
            // Hiển thị thông báo khi thẻ không hợp lệ
            showInvalidCardNotification();
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

    // Phương thức hiển thị thông báo khi không có lá bài hợp lệ
    private void showInvalidCardNotification() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("CẢNH BÁO");
        alert.setHeaderText("KHÔNG CÓ LÁ BÀI HỢP LỆ");
        alert.setContentText("Bạn không có lá bài nào phù hợp với lá bài hiện tại.\nVui lòng bốc thêm bài hoặc nhấn UNO nếu chỉ còn 1 lá!");
        alert.showAndWait();
    }

    // Kiểm tra xem có lá bài nào có thể đánh không
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

    // Hiển thị cảnh báo khi không có lá bài nào hợp lệ
    private void showNoPlayableCardNotification() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Không có lá bài hợp lệ");
        alert.setHeaderText(null);
        alert.setContentText("⚠️ Bạn không có lá bài nào phù hợp.\nVui lòng bốc bài.");
        alert.showAndWait();
    }
}
