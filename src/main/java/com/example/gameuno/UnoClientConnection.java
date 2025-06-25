package com.example.gameuno;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Kết nối client với server UNO.
 * - Quản lý kết nối TCP
 * - Gửi lệnh, nhận tin nhắn
 * - Đảm bảo đóng an toàn
 */
public class UnoClientConnection {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    /**
     * Tạo kết nối TCP đến server.
     *
     * @param server IP/domain ví dụ "localhost"
     * @param port   cổng ví dụ 12345
     * @throws IOException nếu không kết nối được
     */
    public UnoClientConnection(String server, int port) throws IOException {
        socket = new Socket(server, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("✅ Kết nối thành công: " + server + ":" + port);
    }

    /**
     * Gửi dữ liệu tới server.
     *
     * @param msg lệnh hoặc tin nhắn
     */
    public synchronized void send(String msg) {
        if (isConnected()) {
            out.println(msg);
            out.flush();
            System.out.println("📤 Đã gửi: " + msg);
        } else {
            System.err.println("⚠️ Không gửi được: Kết nối đã đóng.");
        }
    }

    /**
     * Nhận dữ liệu từ server.
     *
     * @return BufferedReader để đọc
     */
    public BufferedReader getIn() {
        return in;
    }

    /**
     * Kiểm tra còn kết nối không.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Đóng tất cả stream và socket.
     */
    public void close() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        System.out.println("🔌 Đã đóng kết nối.");
    }
}
