package com.example.gameuno;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class RuleController {

    // 👉 KHÔNG cần TextArea hay Label vì nội dung đã viết trực tiếp trong FXML

    @FXML
    private void closeRule(ActionEvent event) {
        // Lấy cửa sổ chứa nút "Đóng" rồi đóng lại
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

}
