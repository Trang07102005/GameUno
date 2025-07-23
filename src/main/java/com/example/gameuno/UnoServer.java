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
        private int playerIndex = -1; // MỚI: gán slot index cố định

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
                                playerIndex = playerNames.size(); // MỚI: chỉ số slot
                                playerNames.add(playerName);
                                System.out.println("✅ Đăng ký: " + playerName + " [slot " + playerIndex + "]");
                                broadcast("WAITING_PLAYERS:" + (expectedPlayers - playerNames.size()));
                                checkStartGame();
                            }
                        }

                    } else if (line.startsWith("PLAY_CARD:")) {
                    try {
                        // ⚠ Tách theo dấu ":" đầu tiên
                        String[] mainParts = line.split(":", 2);
                        if (mainParts.length < 2) {
                            System.out.println("❗ Lỗi định dạng PLAY_CARD: " + line);
                            return;
                        }

                        String payload = mainParts[1].trim(); // ví dụ: "1 Red,Eight"
                        String[] splitPayload = payload.split(" ");
                        if (splitPayload.length != 2) {
                            System.out.println("❗ PLAY_CARD sai định dạng: " + payload);
                            return;
                        }

                        String playerName = splitPayload[0];  // "1"
                        String[] cardParts = splitPayload[1].split(",");
                        if (cardParts.length != 2) {
                            System.out.println("❗ Lá bài không hợp lệ: " + splitPayload[1]);
                            return;
                        }

                        UnoCard.Color color = UnoCard.Color.valueOf(cardParts[0]);
                        UnoCard.Value value = UnoCard.Value.valueOf(cardParts[1]);
                        UnoCard played = new UnoCard(color, value);

                        if ((currentPlayer - 1) != playerIndex) {
                            System.out.println("⚠️ Sai lượt: " + playerName);
                            return;
                        }

                        currentCard = played;
                        System.out.println("🔥 " + playerName + " đánh: " + currentCard);
                        broadcast("PLAY_CARD:" + this.playerName + " " + cardParts[0] + "," + cardParts[1]);
                        nextPlayer();

                    } catch (Exception e) {
                        System.out.println("❗ Lỗi xử lý PLAY_CARD: " + e.getMessage());
                        e.printStackTrace();
                    }

            } else if (line.startsWith("DRAW_CARD:")) {
                        String name = line.split(":")[1];
                        if ((currentPlayer - 1) != playerIndex) {
                            System.out.println("⚠️ Sai lượt rút: " + playerName);
                            continue;
                        }
                        broadcast(line);
                        nextPlayer();

                    } else if (line.startsWith("CALL_UNO:")) {
                        broadcast(line);
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
                if (playerName != null) playerNames.remove(playerName);
                clients.remove(this);
                System.out.println("🧹 Xoá [" + playerName + "]");
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
    }
}
