package com.example.gameuno;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class EndGameController {

    @FXML private Label winnerLabel;

    public void setWinner(String name) {
        winnerLabel.setText("🎉 Người chiến thắng là: " + name);
    }

    @FXML
    private void handleBackToMenu(ActionEvent event) {
        Stage stage = (Stage) winnerLabel.getScene().getWindow();
        stage.close(); // hoặc load menu.fxml nếu muốn
    }
}
