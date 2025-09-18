package server;

import java.io.*;
import java.net.Socket;

/**
 * ClientHandler: xử lý 1 kết nối client
 * Giao tiếp bằng các dòng text: ASSIGN_NAME:, ROOM_CREATED:, PLAYER_LIST:, THONGBAO:, START:GAME, RESULT:, ROOM_DELETED:, LEAVE_OK, LOI:
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;

    private Room room;               // phòng hiện tại (null nếu chưa vào)
    private String playerName = "Người chơi";

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getPlayerName() {
        return playerName;
    }

    // gửi message tới client (kèm newline)
    public void send(String msg) {
        if (out != null) out.println(msg);
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true); // autoFlush true
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // initial welcome + send assigned name (default)
            send("ASSIGN_NAME:" + playerName);
            send("THONGBAO:Kết nối thành công tới server.");

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                System.out.println("From client [" + playerName + "]: " + line);

                // SET_NAME
                if (line.startsWith("{SET_NAME:") && line.endsWith("}")) {
                    String name = line.substring("{SET_NAME:".length(), line.length() - 1).trim();
                    if (!name.isEmpty()) {
                        playerName = name;
                        send("ASSIGN_NAME:" + playerName);
                    } else {
                        send("LOI:Tên không hợp lệ.");
                    }
                    continue;
                }

                // CREATE_ROOM
                if (line.equals("{CREATE_ROOM}")) {
                    if (room != null) {
                        send("LOI:Bạn đang ở trong một phòng.");
                    } else {
                        room = GameServer.createRoom(this);
                        // Room.addPlayer sẽ gửi JOIN_OK
                        send("ROOM_CREATED:" + room.getRoomId());
                        send("THONGBAO:Bạn đã tạo phòng " + room.getRoomId() + ". Đang chờ người chơi khác...");
                    }
                    continue;
                }

                // JOIN_ROOM:{id}
                if (line.startsWith("{JOIN_ROOM:") && line.endsWith("}")) {
                    String id = line.substring("{JOIN_ROOM:".length(), line.length() - 1).trim();
                    if (room != null) {
                        send("LOI:Bạn đang ở trong một phòng.");
                    } else {
                        Room r = GameServer.joinRoom(id, this);
                        if (r != null) {
                            room = r;
                            // Không gửi JOIN_OK ở đây nữa (Room.addPlayer đã gửi)
                        } else {
                            send("LOI:Không tìm thấy phòng " + id + " hoặc phòng đã đầy.");
                        }
                    }
                    continue;
                }

                // JOIN_RANDOM
                if (line.equals("{JOIN_RANDOM}")) {
                    if (room != null) {
                        send("LOI:Bạn đang ở trong một phòng.");
                    } else {
                        Room r = GameServer.joinRandom(this);
                        if (r != null) {
                            room = r;
                            // Không gửi JOIN_OK ở đây nữa (Room.addPlayer đã gửi)
                        } else {
                            send("LOI:Không có phòng trống để tham gia ngẫu nhiên.");
                        }
                    }
                    continue;
                }

                // MOVE:ROCK|PAPER|SCISSORS
                if (line.startsWith("{MOVE:") && line.endsWith("}")) {
                    String move = line.substring("{MOVE:".length(), line.length() - 1).trim().toUpperCase();
                    if (room == null) {
                        send("LOI:Bạn chưa vào phòng.");
                    } else if (!move.equals("ROCK") && !move.equals("PAPER") && !move.equals("SCISSORS")) {
                        send("LOI:Lựa chọn không hợp lệ.");
                    } else {
                        room.receiveMove(this, move);
                    }
                    continue;
                }

                // PLAY_AGAIN
                if (line.equals("{PLAY_AGAIN}")) {
                    if (room == null) send("LOI:Bạn không trong phòng.");
                    else room.voteContinue(this, true);
                    continue;
                }

                // LEAVE
                if (line.equals("{LEAVE}")) {
                    if (room == null) {
                        send("LOI:Bạn không trong phòng.");
                    } else {
                        room.removePlayer(this);
                        room = null;
                        send("LEAVE_OK");
                    }
                    continue;
                }

                // Unknown
                send("LOI:Lệnh không hợp lệ: " + line);
            }
        } catch (IOException e) {
            System.out.println("Connection lost for player: " + playerName);
        } finally {
            // cleanup on disconnect
            try {
                if (room != null) {
                    room.removePlayer(this);
                    room = null;
                }
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
