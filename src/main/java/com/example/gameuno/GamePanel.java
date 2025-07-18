package com.example.gameuno;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.animation.PauseTransition;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GamePanel {
    @FXML
    private StackPane rootPane;

    @FXML
    private Label notificationLabel;

    public void initialize() {
        notificationLabel.setVisible(false);
    }

    public void showNotification(String message) {
        notificationLabel.setText(message);
        notificationLabel.setVisible(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> notificationLabel.setVisible(false));
        pause.play();
    }

    public StackPane getRootPane() {
        return rootPane;
    }
    public void startGame() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gameuno/game.fxml"));
        Parent root = loader.load();
        GamePanel gamePanel = loader.getController();
        GameMode.setGamePanel(gamePanel); // Gán gamePanel cho GameMode
        Stage stage = new Stage();
        stage.setScene(new Scene(root, 800, 600));
        stage.setTitle("UNO Game");
        stage.show();

        // Khởi tạo chế độ và lượt chơi
        GameMode.setMode(GameMode.OFFLINE);
        GameMode.setCurrentPlayer("Người chơi 1");
    }
}