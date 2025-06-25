package com.example.gameuno;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
    private void playOffline(ActionEvent event) {
        GameMode.setMode(GameMode.OFFLINE);
        goToPlayerSelection(event, false, null);
    }

    @FXML
    private void playOnline(ActionEvent event) {
        try {
            UnoClientConnection client = new UnoClientConnection("localhost", 12345);
            GameMode.setMode(GameMode.ONLINE);
            goToPlayerSelection(event, true, client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToPlayerSelection(ActionEvent event, boolean isOnline, UnoClientConnection client) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gameuno/Player_Selection.fxml"));
            Parent root = loader.load(); // Load the Parent node
            Player_Selection controller = loader.getController();
            controller.init(isOnline, client); // Initialize controller with isOnline and client
            Scene scene = new Scene(root); // Create a new Scene with the Parent
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("UNO - Chọn số người chơi");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void viewRules(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gameuno/rule.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
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