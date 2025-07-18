package com.example.gameuno;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

/**
 * Controller cho màn chọn số người chơi.
 */
public class Player_Selection {

    private int numberOfPlayers;
    private boolean isOnline;
    private UnoClientConnection client;

    /**
     * Khởi tạo chế độ chơi và kết nối client (nếu Online).
     */
    public void init(boolean isOnline, UnoClientConnection client) {
        this.isOnline = isOnline;
        this.client = client;
    }

    /**
     * Chọn 2 người chơi
     */
    @FXML
    private void selectTwoPlayers(ActionEvent event) {
        numberOfPlayers = 2;
        System.out.println("🔵 Đã chọn: " + numberOfPlayers + " người chơi");
        goToEnterNames(event);
    }

    /**
     * Chọn 3 người chơi
     */
    @FXML
    private void selectThreePlayers(ActionEvent event) {
        numberOfPlayers = 3;
        System.out.println("🟢 Đã chọn: " + numberOfPlayers + " người chơi");
        goToEnterNames(event);
    }

    /**
     * Chọn 4 người chơi
     */
    @FXML
    private void selectFourPlayers(ActionEvent event) {
        numberOfPlayers = 4;
        System.out.println("🟣 Đã chọn: " + numberOfPlayers + " người chơi");
        goToEnterNames(event);
    }

    /**
     * Trở lại Menu chính
     */
    @FXML
    private void goBackToMenu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gameuno/menu.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("UNO MENU");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hàm mở màn nhập tên người chơi
     */
    private void goToEnterNames(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gameuno/enter_names.fxml"));
            Parent root = loader.load();
            EnterNames enterNamesController = loader.getController();
            enterNamesController.initNumberOfPlayers(numberOfPlayers, isOnline, client);
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Nhập tên người chơi");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}