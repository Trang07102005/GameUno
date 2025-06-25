package com.example.gameuno;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

public class EnterNames {

    @FXML
    private VBox namesBox;
    @FXML
    private Label statusLabel;

    private int numberOfPlayers;
    private boolean isOnline;
    private UnoClientConnection client;

    private static boolean hostSentPlayerCount = false;
    private String myName;

    public void initNumberOfPlayers(int num, boolean isOnline, UnoClientConnection client) {
        this.numberOfPlayers = num;
        this.isOnline = isOnline;
        this.client = client;

        TextField tf = new TextField();
        tf.setPromptText("Tên của bạn");
        tf.setPrefWidth(300);
        tf.setStyle("-fx-font-size: 16; -fx-background-radius: 10; -fx-padding: 10;");
        namesBox.getChildren().add(tf);

        if (isOnline && statusLabel != null) {
            statusLabel.setText("✍️ Nhập tên & bấm XÁC NHẬN để tham gia phòng...");
        }
    }

    @FXML
    private void startGame(ActionEvent event) {
        try {
            TextField tf = (TextField) namesBox.getChildren().get(0);
            String inputName = tf.getText().trim();
            if (inputName.isEmpty()) {
                inputName = "Player" + System.currentTimeMillis();
            }
            this.myName = inputName;

            if (isOnline) {
                if (!hostSentPlayerCount) {
                    client.send("PLAYER_COUNT:" + numberOfPlayers);
                    hostSentPlayerCount = true;
                }

                client.send("PLAYER_NAME:" + myName);

                if (statusLabel != null) {
                    statusLabel.setText("⏳ Đã gửi! Chờ những người khác...");
                }

                new Thread(() -> {
                    try {
                        List<String> playerNames = new ArrayList<>();
                        UnoCard firstCard = null;
                        List<UnoCard> myInitialHand = new ArrayList<>();
                        String response;

                        while ((response = client.getIn().readLine()) != null) {
                            System.out.println("📥 Server: " + response);

                            if (response.startsWith("GAME_START:")) {
                                String[] parts = response.split(":")[1].split(",");
                                playerNames.addAll(List.of(parts));
                            } else if (response.startsWith("CURRENT_CARD:")) {
                                String[] p = response.split(":")[1].split(",");
                                firstCard = new UnoCard(
                                        UnoCard.Color.valueOf(p[0]),
                                        UnoCard.Value.valueOf(p[1])
                                );
                            } else if (response.startsWith("INITIAL_HAND:")) {
                                String[] cards = response.split(":")[1].split(";");
                                for (String card : cards) {
                                    if (!card.isEmpty()) {
                                        String[] p = card.split(",");
                                        myInitialHand.add(new UnoCard(
                                                UnoCard.Color.valueOf(p[0]),
                                                UnoCard.Value.valueOf(p[1])
                                        ));
                                    }
                                }
                            }

                            if (!playerNames.isEmpty() && firstCard != null && !myInitialHand.isEmpty()) {
                                UnoCard finalFirstCard = firstCard;
                                Platform.runLater(() ->
                                        loadOnlineGame(event, playerNames, myInitialHand, finalFirstCard)
                                );
                                break;
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            if (statusLabel != null) {
                                statusLabel.setText("❌ Mất kết nối server!");
                            }
                        });
                    }
                }).start();

            } else {
                List<String> playerNames = new ArrayList<>();
                playerNames.add(myName);
                for (int i = 2; i <= numberOfPlayers; i++) {
                    playerNames.add("AI Bot " + i);
                }
                loadOfflineGame(event, playerNames);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (statusLabel != null) {
                statusLabel.setText("❌ Lỗi: " + e.getMessage());
            }
        }
    }

    private void loadOnlineGame(ActionEvent event, List<String> playerNames, List<UnoCard> myInitialHand, UnoCard firstCard) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gameuno/game_online.fxml"));
            Parent root = loader.load();
            GameOnline gameOnline = loader.getController();
            gameOnline.initPlayers(numberOfPlayers, playerNames, client, myName, myInitialHand, firstCard);
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("UNO ONLINE");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadOfflineGame(ActionEvent event, List<String> playerNames) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gameuno/game.fxml"));
            Parent root = loader.load();
            Game game = loader.getController();
            game.initPlayers(numberOfPlayers, playerNames);
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("UNO OFFLINE");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
