package com.example.gameuno;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class Menu {

    @FXML
    private Button soundButton;

    @FXML
    private void initialize() {
        if (SoundManager.isSoundOn()) {
            soundButton.setText("🔊");
            SoundManager.playBGM();
        } else {
            soundButton.setText("🔇");
        }
    }

    @FXML
    private void toggleSound() {
        SoundManager.setSoundOn(!SoundManager.isSoundOn());

        if (SoundManager.isSoundOn()) {
            soundButton.setText("🔊");
            SoundManager.playBGM();
        } else {
            soundButton.setText("🔇");
            SoundManager.stopBGM();
        }
    }

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("rule.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Thể lệ UNO");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exitGame(ActionEvent event) {
        System.exit(0);
    }
}
