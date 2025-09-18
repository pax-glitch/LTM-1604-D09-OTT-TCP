package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * GameServer: quản lý map phòng, lắng nghe kết nối, tạo ClientHandler.
 */
public class GameServer {
    private static final Map<String, Room> rooms = new HashMap<>();
    private final int port;

    public GameServer(int port) {
        this.port = port;
    }

    // Tạo phòng mới và thêm creator vào phòng
    public static synchronized Room createRoom(ClientHandler creator) {
        String id = "room" + System.currentTimeMillis();
        Room r = new Room(id, creator);
        rooms.put(id, r);
        System.out.println("Created room: " + id);
        return r;
    }

    // Join theo ID nếu phòng chưa đủ 2 người
    public static synchronized Room joinRoom(String roomId, ClientHandler player) {
        Room r = rooms.get(roomId);
        if (r != null && r.getPlayersCount() < 2) {
            r.addPlayer(player);
            return r;
        }
        return null;
    }

    // Join ngẫu nhiên: tìm phòng đang chờ (1 người)
    public static synchronized Room joinRandom(ClientHandler player) {
        for (Room r : rooms.values()) {
            if (r.getPlayersCount() < 2) {
                r.addPlayer(player);
                return r;
            }
        }
        return null;
    }

    // Xóa phòng (khi trống hoặc bị giải tán)
    public static synchronized void deleteRoom(String roomId) {
        rooms.remove(roomId);
        System.out.println("Deleted room: " + roomId);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("GameServer running on port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("New connection: " + client.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(client, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Run server
    public static void main(String[] args) {
        new GameServer(12345).start();
    }
}
