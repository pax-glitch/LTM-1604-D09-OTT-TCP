package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

/**
 * Main client UI - quản lý kết nối và panels
 */
public class GameClientGUI extends JFrame {
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    private String playerName = "Người chơi";
    private String currentRoomId = "";

    private StartPanel startPanel;
    private RoomPanel roomPanel;
    private GamePanel gamePanel;

    private BackgroundPanel backgroundPanel;

    public GameClientGUI(String host, int port) {
        setTitle("Oẳn Tù Tì Online");
        setSize(760, 540);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // tạo background panel với ảnh
        backgroundPanel = new BackgroundPanel("/client/images/background.png");
        setContentPane(backgroundPanel);

        // init panels
        startPanel = new StartPanel(this::sendCommand, this::promptChangeName);
        roomPanel = new RoomPanel(this::sendCommand, this::onLeaveFromUI);
        gamePanel = new GamePanel(this::sendCommand, this::onLeaveFromUI);

        // mặc định hiển thị startPanel
        startPanel.setOpaque(false);
        roomPanel.setOpaque(false);
        gamePanel.setOpaque(false);

        backgroundPanel.add(startPanel, BorderLayout.CENTER);

        // connect tới server
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // request name immediately
            SwingUtilities.invokeLater(() -> {
                String name = JOptionPane.showInputDialog(this, "Nhập tên của bạn:", "Đặt tên", JOptionPane.PLAIN_MESSAGE);
                if (name != null && !name.trim().isEmpty()) {
                    playerName = name.trim();
                    sendCommand("{SET_NAME:" + playerName + "}");
                    startPanel.setPlayerName(playerName);
                }
            });

            // start listening thread
            new Thread(this::listenFromServer, "ServerListener").start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối server: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void listenFromServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final String msg = line;
                System.out.println("SERVER -> " + msg);

                if (msg.startsWith("ASSIGN_NAME:")) {
                    playerName = msg.substring("ASSIGN_NAME:".length()).trim();
                    SwingUtilities.invokeLater(() -> startPanel.setPlayerName(playerName));

                } else if (msg.startsWith("ROOM_CREATED:")) {
                    currentRoomId = msg.substring("ROOM_CREATED:".length()).trim();
                    SwingUtilities.invokeLater(() -> {
                        startPanel.setRoomId(currentRoomId);
                        roomPanel.updateRoom(currentRoomId, playerName, "-");
                        switchPanel(roomPanel);
                    });

                } else if (msg.startsWith("JOIN_OK:")) {
                    currentRoomId = msg.substring("JOIN_OK:".length()).trim();
                    SwingUtilities.invokeLater(() -> {
                        roomPanel.updateRoom(currentRoomId, playerName, "-");
                        switchPanel(roomPanel);
                    });

                } else if (msg.startsWith("PLAYER_LIST:")) {
                    String list = msg.substring("PLAYER_LIST:".length()).trim();
                    SwingUtilities.invokeLater(() -> roomPanel.updatePlayers(list));

                } else if (msg.startsWith("THONGBAO:")) {
                    String info = msg.substring("THONGBAO:".length()).trim();
                    SwingUtilities.invokeLater(() -> roomPanel.appendMessage(info));

                } else if (msg.startsWith("START:GAME")) {
                    SwingUtilities.invokeLater(() -> {
                        gamePanel.reset();
                        switchPanel(gamePanel);
                    });

                } else if (msg.startsWith("RESULT:")) {
                    String res = msg.substring("RESULT:".length()).trim();
                    SwingUtilities.invokeLater(() -> {
                        gamePanel.showResult(res);
                        roomPanel.appendMessage("KẾT QUẢ: " + res);
                    });

                } else if (msg.startsWith("LEAVE_OK")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Bạn đã rời phòng.");
                        switchPanel(startPanel);
                        roomPanel.resetForNewRoom();
                    });

                } else if (msg.startsWith("ROOM_DELETED:")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Phòng đã bị đóng.");
                        switchPanel(startPanel);
                        roomPanel.resetForNewRoom();
                    });

                } else if (msg.startsWith("LOI:")) {
                    String err = msg.substring("LOI:".length()).trim();
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, err, "Lỗi", JOptionPane.ERROR_MESSAGE));

                } else {
                    SwingUtilities.invokeLater(() -> roomPanel.appendMessage(msg));
                }
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Mất kết nối tới server.", "Lỗi", JOptionPane.ERROR_MESSAGE));
            System.exit(0);
        }
    }

    // đổi panel hiển thị trên background
    private void switchPanel(JPanel newPanel) {
        newPanel.setOpaque(false);
        backgroundPanel.removeAll();
        backgroundPanel.add(newPanel, BorderLayout.CENTER);
        backgroundPanel.revalidate();
        backgroundPanel.repaint();
    }

    private void sendCommand(String cmd) {
        if (out != null) out.println(cmd);
    }

    private void promptChangeName() {
        String newName = JOptionPane.showInputDialog(this, "Nhập tên mới:", playerName);
        if (newName != null && !newName.trim().isEmpty()) {
            playerName = newName.trim();
            sendCommand("{SET_NAME:" + playerName + "}");
            SwingUtilities.invokeLater(() -> startPanel.setPlayerName(playerName));
        }
    }

    private void onLeaveFromUI() {
        sendCommand("{LEAVE}");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new GameClientGUI("127.0.0.1", 12345).setVisible(true));
    }
}

/** Panel có background ảnh */
class BackgroundPanel extends JPanel {
    private final Image bg;

    public BackgroundPanel(String imagePath) {
        java.net.URL imgURL = getClass().getResource(imagePath);
        if (imgURL != null) {
            bg = new ImageIcon(imgURL).getImage();
        } else {
            System.err.println("Không tìm thấy ảnh background: " + imagePath);
            bg = null;
        }
        setLayout(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bg != null) {
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
