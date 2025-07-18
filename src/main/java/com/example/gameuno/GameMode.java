package com.example.gameuno;

import javafx.scene.control.ChoiceDialog;
import java.util.Optional;

public enum GameMode {
    OFFLINE, ONLINE;

    private static GameMode mode;
    private static GamePanel gamePanel; // Tham chiếu đến giao diện
    private static String currentPlayer; // Người chơi hiện tại

    public static void setMode(GameMode m) {
        mode = m;
    }

    public static GameMode getMode() {
        return mode;
    }

    // Khởi tạo game panel
    public static void setGamePanel(GamePanel panel) {
        gamePanel = panel;
    }

    // Đặt người chơi hiện tại và hiển thị thông báo
    public static void setCurrentPlayer(String playerName) {
        currentPlayer = playerName;
        if (gamePanel != null) {
            gamePanel.showNotification("Lượt: " + currentPlayer);
        }
    }

    // Hiển thị dialog chọn màu khi đánh lá Wild
    public static String showColorSelection() {
        if (gamePanel != null) {
            ChoiceDialog<String> dialog = new ChoiceDialog<>("Đỏ", "Đỏ", "Xanh", "Vàng", "Xanh lá");
            dialog.setTitle("Chọn màu");
            dialog.setHeaderText("Chọn màu cho lá bài Wild:");
            Optional<String> result = dialog.showAndWait();
            return result.orElse("Đỏ");
        }
        return "Đỏ"; // Mặc định nếu không có giao diện
    }

    // Kiểm tra và thông báo khi không có bài
    public static void checkAndNotifyNoCard() {
        if (gamePanel != null) {
            if (!hasPlayableCard()) { // Placeholder, bạn cần định nghĩa
                gamePanel.showNotification("Không có bài! Vui lòng rút bài.");
                drawCard(); // Placeholder, bạn cần định nghĩa
            }
        }
    }

    // Placeholder cho logic kiểm tra bài hợp lệ
    private static boolean hasPlayableCard() {
        // Thêm logic kiểm tra xem có bài nào hợp lệ không
        return false; // Placeholder, thay bằng logic thực tế
    }

    // Placeholder cho logic rút bài
    private static void drawCard() {
        // Thêm logic rút bài từ bộ bài
    }
}