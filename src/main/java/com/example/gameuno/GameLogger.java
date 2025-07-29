package com.example.gameuno;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GameLogger {
    private static final File LOG_FILE;
    private static LocalDateTime gameStartTime;

    // Initialize the temporary file in a static block
    static {
        try {
            LOG_FILE = File.createTempFile("game_history", ".txt");
            LOG_FILE.deleteOnExit(); // Ensure file is deleted on JVM exit
            System.out.println("📁 [GameLogger] Đã tạo file tạm: " + LOG_FILE.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("❌ [GameLogger] Không thể tạo file tạm:", e);
        }
    }

    // Gọi khi bắt đầu ván
    public static void startGame(List<String> playerNames) {
        gameStartTime = LocalDateTime.now();

        StringBuilder sb = new StringBuilder();
        sb.append("🕹️  VÁN CHƠI MỚI\n");
        sb.append("🧑‍🤝‍🧑 Người chơi tham gia: ").append(String.join(", ", playerNames)).append("\n");
        sb.append("⏰ Bắt đầu: ").append(gameStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");

        appendToFile(sb.toString());
        System.out.println("📁 [GameLogger] Đường dẫn ghi file: " + LOG_FILE.getAbsolutePath());
    }

    // Gọi khi một người chơi thực hiện hành động
    public static void logMove(String playerName, String action) {
        if (gameStartTime == null) {
            System.out.println("⚠️ [GameLogger] Không ghi logMove vì gameStartTime = null");
            return;
        }

        Duration elapsed = Duration.between(gameStartTime, LocalDateTime.now());
        long seconds = elapsed.getSeconds();
        String timestamp = String.format("[%02d:%02d]", (seconds / 60), seconds % 60);
        String log = timestamp + " " + playerName + " " + action + "\n";

        appendToFile(log);
        System.out.println("📝 [GameLogger] Ghi logMove: " + log.trim());
    }

    // Gọi khi kết thúc ván
    public static void logResult(String winner, int totalPlayers, int cardsLeft, List<String> playerNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("🏆 Người thắng: ").append(winner).append("\n");
        sb.append("🃏 Số người chơi: ").append(totalPlayers).append("\n");
        sb.append("📥 Số lá còn lại (người thắng): ").append(cardsLeft).append("\n");
        sb.append("------------------------------------------------------------\n\n");

        appendToFile(sb.toString());
        System.out.println("🏁 [GameLogger] Ghi kết quả ván chơi: " + winner);
        gameStartTime = null;
    }

    // Ghi vào file
    private static void appendToFile(String content) {
        System.out.println("📁 [GameLogger] Đường dẫn ghi file: " + LOG_FILE.getAbsolutePath());

        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(content);
            System.out.println("✅ [GameLogger] Ghi file thành công.");
        } catch (IOException e) {
            System.err.println("❌ [GameLogger] Lỗi khi ghi file lịch sử:");
            e.printStackTrace();
        }
    }
}