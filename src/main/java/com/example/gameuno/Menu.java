package com.example.gameuno;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

public class Menu {
    @FXML
    private void playGame(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Player_Selection.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("UNO - Chơi ngay");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void viewRules(ActionEvent event) {
        System.out.println("📋 Thể lệ UNO: Mỗi người chia 7 lá, đánh bài theo màu hoặc số, ai hết bài trước thì thắng!");
        // Bạn có thể thay = mở file FXML rules.fxml nếu muốn
    }

    @FXML
    private void exitGame(ActionEvent event) {
        System.exit(0);
    }
}
