package com.example.gameuno;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GameLogger {
    private static final String FILE_PATH = "game_history.txt";

    private static List<String> logs = new ArrayList<>();
    private static LocalDateTime gameStartTime;

    // Gọi khi bắt đầu ván
    public static void startGame(List<String> playerNames) {
        logs.clear();
        gameStartTime = LocalDateTime.now();

        logs.add("🕹️  VÁN CHƠI MỚI\n");
        logs.add("🧑‍🤝‍🧑 Người chơi tham gia: " + String.join(", ", playerNames) + "\n");
        logs.add("⏰ Bắt đầu: " + gameStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
    }

    // Gọi khi một người chơi thực hiện hành động
    public static void logMove(String playerName, String action) {
        if (gameStartTime == null) return;

        Duration elapsed = Duration.between(gameStartTime, LocalDateTime.now());
        long seconds = elapsed.getSeconds();
        String timestamp = String.format("[%02d:%02d]", (seconds / 60), seconds % 60);
        logs.add(timestamp + " " + playerName + " " + action + "\n");
    }

    // Gọi khi kết thúc ván
    public static void logResult(String winner, int totalPlayers, int cardsLeft, List<String> playerNames) {
        logs.add("🏆 Người thắng: " + winner + "\n");
        logs.add("🃏 Số người chơi: " + totalPlayers + "\n");
        logs.add("📥 Số lá còn lại (người thắng): " + cardsLeft + "\n");
        logs.add("------------------------------------------------------------\n\n");

        try (FileWriter writer = new FileWriter(FILE_PATH, true)) {
            for (String line : logs) {
                writer.write(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        logs.clear();
        gameStartTime = null;
    }
}
