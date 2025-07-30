package com.example.gameuno;

import java.io.*;
import java.net.*;
import java.util.*;

public class UnoServer {

    private static final int PORT = 12345;
    private static UnoDeck deck;
    private static UnoCard currentCard;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> playerNames = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Integer> playerCardCounts = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Boolean> calledUno = Collections.synchronizedMap(new HashMap<>());
    private static int expectedPlayers = 0;
    private static int currentPlayer = 1; // 1-based
    private static volatile boolean gameStarted = false;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("🌟 UNO Server đang chạy trên cổng " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            synchronized (clients) {
                if (gameStarted) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("ERROR:Game already started");
                    socket.close();
                } else {
                    ClientHandler handler = new ClientHandler(socket);
                    clients.add(handler);
                    new Thread(handler).start();
                }
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private volatile boolean running = true;
        private String playerName = null;
        private int playerIndex = -1;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("🔗 Client mới đã kết nối.");
        }

        public void run() {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    System.out.println("📩 " + line);

                    if (line.startsWith("PLAYER_COUNT:")) {
                        synchronized (clients) {
                            if (expectedPlayers == 0) {
                                expectedPlayers = Integer.parseInt(line.split(":")[1]);
                                System.out.println("✅ Số người chơi: " + expectedPlayers);
                                broadcast("WAITING_PLAYERS:" + (expectedPlayers - playerNames.size()));
                            } else {
                                out.println("INFO:IGNORED_COUNT:" + expectedPlayers);
                                out.flush();
                            }
                        }
                    } else if (line.startsWith("PLAYER_NAME:")) {
                        synchronized (clients) {
                            if (playerName == null && !gameStarted) {
                                playerName = line.split(":")[1];
                                playerIndex = playerNames.size();
                                playerNames.add(playerName);
                                playerCardCounts.put(playerName, 7); // Khởi tạo với 7 lá
                                calledUno.put(playerName, false); // Khởi tạo trạng thái UNO
                                System.out.println("✅ Đăng ký: " + playerName + " [slot " + playerIndex + "]");
                                broadcast("WAITING_PLAYERS:" + (expectedPlayers - playerNames.size()));
                                checkStartGame();
                            }
                        }
                    } else if (line.startsWith("PLAY_CARD:")) {
                        try {
                            String[] mainParts = line.split(":", 2);
                            if (mainParts.length < 2) {
                                System.out.println("❗ Lỗi định dạng PLAY_CARD: " + line);
                                return;
                            }

                            String payload = mainParts[1].trim();
                            String[] splitPayload = payload.split(" ");
                            if (splitPayload.length != 2) {
                                System.out.println("❗ PLAY_CARD sai định dạng: " + payload);
                                return;
                            }

                            String playerName = splitPayload[0];
                            String[] cardParts = splitPayload[1].split(",");
                            if (cardParts.length != 2) {
                                System.out.println("❗ Lá bài không hợp lệ: " + splitPayload[1]);
                                return;
                            }

                            if ((currentPlayer - 1) != playerIndex) {
                                System.out.println("⚠️ Sai lượt: " + playerName);
                                return;
                            }

                            UnoCard.Color color = UnoCard.Color.valueOf(cardParts[0]);
                            UnoCard.Value value = UnoCard.Value.valueOf(cardParts[1]);
                            UnoCard played = new UnoCard(color, value);

                            // Cập nhật số lá bài
                            int currentCount = playerCardCounts.getOrDefault(playerName, 7);
                            currentCount--;
                            playerCardCounts.put(playerName, currentCount);

                            // Kiểm tra UNO khi còn 1 lá
                            if (currentCount == 1 && !calledUno.getOrDefault(playerName, false)) {
                                System.out.println("⚠️ " + playerName + " chưa gọi UNO!");
                                broadcast("INFO:UNO_NOT_CALLED:" + playerName);
                                // Phạt rút 2 lá
                                broadcast("DRAW_CARD:" + playerName + ":2");
                                playerCardCounts.put(playerName, currentCount + 2);
                                calledUno.put(playerName, false); // Reset trạng thái UNO
                                broadcast("PLAYER_CARD_COUNT:" + playerName + ":" + playerCardCounts.get(playerName));
                                nextPlayer();
                                continue; // Bỏ qua việc đánh lá để xử lý phạt
                            }

                            currentCard = played;
                            System.out.println("🔥 " + playerName + " đánh: " + currentCard);
                            broadcast("PLAY_CARD:" + playerName + " " + cardParts[0] + "," + cardParts[1]);
                            broadcast("PLAYER_CARD_COUNT:" + playerName + ":" + currentCount);

                            if (currentCount == 0) {
                                System.out.println("🏆 " + playerName + " hết bài!");
                                broadcast("GAME_OVER:" + playerName);
                                continue;
                            }

                            if (value == UnoCard.Value.Skip) {
                                System.out.println("🚫 Bỏ lượt người tiếp theo!");
                                broadcast("INFO:SKIP:" + getNextPlayerName());
                                nextPlayer();
                                nextPlayer();
                            } else if (value == UnoCard.Value.Reverse) {
                                System.out.println("🔄 Reverse được chơi");
                                broadcast("INFO:REVERSE:" + playerName);
                                if (expectedPlayers == 2) {
                                    nextPlayer();
                                } else {
                                    nextPlayer();
                                }
                            } else if (value == UnoCard.Value.DrawTwo) {
                                String nextPlayerName = getNextPlayerName();
                                nextPlayer();
                                System.out.println("📤 " + nextPlayerName + " phải rút 2 lá!");
                                broadcast("DRAW_CARD:" + nextPlayerName + ":2");
                                playerCardCounts.put(nextPlayerName, playerCardCounts.getOrDefault(nextPlayerName, 7) + 2);
                                broadcast("PLAYER_CARD_COUNT:" + nextPlayerName + ":" + playerCardCounts.get(nextPlayerName));
                                nextPlayer();
                            } else if (value == UnoCard.Value.WildDrawFour) {
                                String nextPlayerName = getNextPlayerName();
                                nextPlayer();
                                System.out.println("📤 " + nextPlayerName + " phải rút 4 lá!");
                                broadcast("DRAW_CARD:" + nextPlayerName + ":4");
                                playerCardCounts.put(nextPlayerName, playerCardCounts.getOrDefault(nextPlayerName, 7) + 4);
                                broadcast("PLAYER_CARD_COUNT:" + nextPlayerName + ":" + playerCardCounts.get(nextPlayerName));
                                nextPlayer();
                            } else {
                                nextPlayer();
                            }

                            // Reset trạng thái UNO nếu không còn 1 lá
                            if (currentCount > 1) {
                                calledUno.put(playerName, false);
                            }
                        } catch (Exception e) {
                            System.out.println("❗ Lỗi xử lý PLAY_CARD: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else if (line.startsWith("DRAW_CARD:")) {
                        String[] parts = line.split(":");
                        if (parts.length < 2) {
                            System.out.println("❗ Lỗi định dạng DRAW_CARD: " + line);
                            return;
                        }
                        String name = parts[1];
                        int drawCount = (parts.length > 2) ? Integer.parseInt(parts[2]) : 1;
                        if ((currentPlayer - 1) != playerIndex) {
                            System.out.println("⚠️ Sai lượt rút: " + playerName);
                            continue;
                        }
                        System.out.println("📤 " + name + " rút " + drawCount + " lá!");
                        playerCardCounts.put(name, playerCardCounts.getOrDefault(name, 7) + drawCount);
                        broadcast("DRAW_CARD:" + name + ":" + drawCount);
                        broadcast("PLAYER_CARD_COUNT:" + name + ":" + playerCardCounts.get(name));
                        // Reset trạng thái UNO nếu rút bài
                        calledUno.put(name, false);
                        nextPlayer();
                    } else if (line.startsWith("CALL_UNO:")) {
                        calledUno.put(playerName, true);
                        broadcast("CALL_UNO:" + playerName);
                    } else if (line.startsWith("GAME_OVER:")) {
                        String winner = line.split(":")[1];
                        System.out.println("🏆 Trò chơi kết thúc, người thắng: " + winner);
                        broadcast("GAME_OVER:" + winner);
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Mất kết nối [" + playerName + "]: " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void checkStartGame() {
            if (playerNames.size() == expectedPlayers && !gameStarted) {
                gameStarted = true;
                startGame();
            }
        }

        private void startGame() {
            deck = new UnoDeck();
            do {
                currentCard = deck.drawCard();
            } while (currentCard.getColor() == UnoCard.Color.Wild);

            System.out.println("🚀 Bắt đầu game với lá: " + currentCard);

            broadcast("GAME_START:" + String.join(",", playerNames));
            broadcast("CURRENT_CARD:" + currentCard.getColor() + "," + currentCard.getValue());

            List<List<UnoCard>> hands = new ArrayList<>();
            for (int i = 0; i < expectedPlayers; i++) {
                List<UnoCard> hand = new ArrayList<>();
                for (int j = 0; j < 7; j++) hand.add(deck.drawCard());
                hands.add(hand);
                playerCardCounts.put(playerNames.get(i), 7); // Cập nhật số lá bài
                calledUno.put(playerNames.get(i), false); // Reset trạng thái UNO
            }

            for (int i = 0; i < clients.size() && i < hands.size(); i++) {
                StringBuilder sb = new StringBuilder("INITIAL_HAND:");
                for (UnoCard card : hands.get(i)) {
                    sb.append(card.getColor()).append(",").append(card.getValue()).append(";");
                }
                clients.get(i).out.println(sb);
                clients.get(i).out.flush();
            }

            currentPlayer = 1;
            broadcast("CURRENT_PLAYER:" + currentPlayer);
        }

        private void nextPlayer() {
            if (expectedPlayers > 0) {
                currentPlayer = (currentPlayer % expectedPlayers) + 1;
                broadcast("CURRENT_PLAYER:" + currentPlayer);
            }
        }

        private void broadcast(String msg) {
            synchronized (clients) {
                for (ClientHandler c : clients) {
                    if (c.isConnected()) {
                        c.out.println(msg);
                        c.out.flush();
                    }
                }
            }
        }

        private boolean isConnected() {
            return socket != null && socket.isConnected() && !socket.isClosed();
        }

        private void disconnect() {
            synchronized (clients) {
                running = false;
                if (playerName != null) {
                    playerNames.remove(playerName);
                    playerCardCounts.remove(playerName);
                    calledUno.remove(playerName);
                    System.out.println("🧹 Xoá [" + playerName + "]");
                    broadcast("PLAYER_LEFT:" + playerName);
                    if (playerNames.size() == 1 && gameStarted) {
                        String winner = playerNames.get(0);
                        System.out.println("🏆 Chỉ còn 1 người chơi, người thắng: " + winner);
                        broadcast("GAME_OVER:" + winner);
                    }
                }
                clients.remove(this);
                if (!gameStarted) {
                    broadcast("WAITING_PLAYERS:" + (expectedPlayers - playerNames.size()));
                }
            }
            close();
        }

        private void close() {
            try { in.close(); } catch (Exception ignored) {}
            try { out.close(); } catch (Exception ignored) {}
            try { socket.close(); } catch (Exception ignored) {}
            System.out.println("🔌 Đóng [" + playerName + "]");
        }

        private String getCurrentPlayerName() {
            return playerNames.get(currentPlayer - 1);
        }

        private String getNextPlayerName() {
            return playerNames.get(currentPlayer % expectedPlayers);
        }
    }
}