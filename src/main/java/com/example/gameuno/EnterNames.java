package com.example.gameuno;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller cho màn nhập tên người chơi (Offline Mode).
 * Chỉ nhập 1 tên, các slot còn lại là AI Bot.
 */
public class EnterNames {

    @FXML
    private VBox namesBox;

    private int numberOfPlayers;

    /**
     * Gọi từ PlayerSelection để nhận số người chơi.
     */
    public void initNumberOfPlayers(int num) {
        this.numberOfPlayers = num;

        // 👉 OFFLINE: luôn chỉ nhập 1 TextField duy nhất cho người thật
        TextField tf = new TextField();
        tf.setPromptText("Tên người chơi");
        tf.setPrefWidth(300);
        tf.setStyle("-fx-font-size: 16; -fx-background-radius: 10; -fx-padding: 10;");
        namesBox.getChildren().add(tf);
    }

    /**
     * Khi bấm nút "XÁC NHẬN".
     */
    @FXML
    private void startGame(ActionEvent event) {
        try {
            List<String> playerNames = new ArrayList<>();

            // Lấy tên người thật
            TextField tf = (TextField) namesBox.getChildren().get(0);
            String name = tf.getText().trim();
            if (name.isEmpty()) {
                name = "Player 1";
            }
            playerNames.add(name);

            // Thêm AI Bot tự động
            for (int i = 2; i <= numberOfPlayers; i++) {
                playerNames.add("AI Bot " + i);
            }

            System.out.println("✅ Danh sách người chơi: " + playerNames);

            // Load Game Scene và truyền tên
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gameuno/game.fxml"));
            Scene scene = new Scene(loader.load());

            Game game = loader.getController();
            game.initPlayers(numberOfPlayers, playerNames);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("UNO GAME");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
