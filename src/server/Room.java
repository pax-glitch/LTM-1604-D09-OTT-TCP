package server;

import java.util.*;

/**
 * Room: quản lý người chơi trong phòng (tối đa 2), moves, votes
 * Các message gửi tới client: 
 *  - JOIN_OK:<roomId>
 *  - PLAYER_LIST:<csv_names>
 *  - THONGBAO:...
 *  - START:GAME
 *  - RESULT:<text>
 *  - ROOM_DELETED:<id>
 *  - LEAVE_OK
 */
public class Room {
    private final String roomId;
    private final List<ClientHandler> players = new ArrayList<>(2);
    private final Map<ClientHandler, String> moves = new HashMap<>();
    private final Map<ClientHandler, Boolean> continueVotes = new HashMap<>();

    public Room(String roomId, ClientHandler creator) {
        this.roomId = roomId;
        addPlayer(creator);
    }

    public String getRoomId() { return roomId; }

    public synchronized int getPlayersCount() { return players.size(); }

    // Thêm người chơi
    public synchronized void addPlayer(ClientHandler ch) {
        if (players.size() >= 2) {
            ch.send("LOI:Phòng đã đủ người.");
            return;
        }

        players.add(ch);

        // Gửi JOIN_OK cho riêng người mới
        ch.send("JOIN_OK:" + roomId);

        // Cập nhật danh sách cho tất cả
        broadcast("PLAYER_LIST:" + getPlayerNames());
        broadcast("THONGBAO:" + ch.getPlayerName() + " đã vào phòng.");

        // Nếu đủ 2 người thì bắt đầu game
        if (players.size() == 2) {
            broadcast("THONGBAO:Đã đủ 2 người. Bắt đầu trò chơi!");
            broadcast("START:GAME");
        }
    }

    // Xóa người chơi khỏi phòng
    public synchronized void removePlayer(ClientHandler ch) {
        boolean removed = players.remove(ch);
        moves.remove(ch);
        continueVotes.remove(ch);
        if (removed) {
            ch.send("LEAVE_OK");
            broadcast("THONGBAO:" + ch.getPlayerName() + " đã rời phòng.");
            broadcast("PLAYER_LIST:" + getPlayerNames());
        }
        if (players.isEmpty()) {
            GameServer.deleteRoom(roomId);
            broadcast("ROOM_DELETED:" + roomId);
        } else {
            for (ClientHandler p : players) {
                p.send("THONGBAO:Phòng " + roomId + " đang chờ người chơi mới...");
            }
        }
    }

    // Nhận lựa chọn
    public synchronized void receiveMove(ClientHandler ch, String move) {
        moves.put(ch, move);
        for (ClientHandler p : players) {
            if (p != ch) p.send("THONGBAO:" + ch.getPlayerName() + " đã chọn (ẩn).");
        }
        if (moves.size() == 2) {
            computeResult();
        }
    }

    private void computeResult() {
        Iterator<Map.Entry<ClientHandler, String>> it = moves.entrySet().iterator();
        Map.Entry<ClientHandler, String> e1 = it.next();
        Map.Entry<ClientHandler, String> e2 = it.next();
        ClientHandler p1 = e1.getKey(), p2 = e2.getKey();
        String m1 = e1.getValue(), m2 = e2.getValue();

        String resultText;
        if (m1.equals(m2)) {
            resultText = "Hòa! Cả hai đều chọn " + m1;
        } else if (isWinner(m1, m2)) {
            resultText = p1.getPlayerName() + " (" + m1 + ") thắng " + p2.getPlayerName() + " (" + m2 + ")";
        } else {
            resultText = p2.getPlayerName() + " (" + m2 + ") thắng " + p1.getPlayerName() + " (" + m1 + ")";
        }

        broadcast("RESULT:" + resultText);
        broadcast("THONGBAO:Ván đã kết thúc! Hãy chọn {PLAY_AGAIN} hoặc {LEAVE}");
        moves.clear();
    }

    private boolean isWinner(String a, String b) {
        return (a.equals("ROCK") && b.equals("SCISSORS")) ||
               (a.equals("PAPER") && b.equals("ROCK")) ||
               (a.equals("SCISSORS") && b.equals("PAPER"));
    }

    // Xử lý vote tiếp tục
    public synchronized void voteContinue(ClientHandler ch, boolean again) {
        continueVotes.put(ch, again);
        if (continueVotes.size() == players.size()) {
            long yes = continueVotes.values().stream().filter(v -> v).count();
            long no = continueVotes.values().stream().filter(v -> !v).count();

            if (yes == players.size() && players.size() == 2) {
                continueVotes.clear();
                broadcast("THONGBAO:Cả hai đều muốn chơi tiếp. Bắt đầu ván mới!");
                broadcast("START:GAME");
            } else if (no == players.size()) {
                broadcast("THONGBAO:Cả hai đều rời phòng. Phòng sẽ bị xóa.");
                for (ClientHandler p : new ArrayList<>(players)) {
                    p.send("THONGBAO:Phòng bị đóng.");
                }
                players.clear();
                GameServer.deleteRoom(roomId);
                broadcast("ROOM_DELETED:" + roomId);
            } else {
                List<ClientHandler> leavers = new ArrayList<>();
                for (Map.Entry<ClientHandler, Boolean> e : continueVotes.entrySet()) {
                    if (!e.getValue()) leavers.add(e.getKey());
                }
                for (ClientHandler l : leavers) {
                    removePlayer(l);
                }
                continueVotes.clear();
            }
        }
    }

    // Trả về danh sách người chơi
    public synchronized String getPlayerNames() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(players.get(i).getPlayerName());
        }
        return sb.toString();
    }

    // Gửi message tới toàn bộ player
    public synchronized void broadcast(String msg) {
        for (ClientHandler p : new ArrayList<>(players)) {
            p.send(msg);
        }
    }
}
