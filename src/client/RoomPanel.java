package client;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * RoomPanel: hiển thị thông tin phòng và log, nút rời phòng
 */
public class RoomPanel extends JPanel {
    private final JLabel roomLabel = new JLabel("Phòng: -");
    private final JLabel meLabel = new JLabel("Bạn: -");
    private final JLabel opponentLabel = new JLabel("Đối thủ: -");
    private final JTextArea logArea = new JTextArea(15, 40);

    private final Consumer<String> sender;
    private final Runnable onLeaveLocal;

    public RoomPanel(Consumer<String> sender, Runnable onLeaveLocal) {
        this.sender = sender;
        this.onLeaveLocal = onLeaveLocal;
        buildUI();
    }

    // alternate constructor used in full client files above (two-arg). If not provided, we still support one-arg
    public RoomPanel(Consumer<String> sender) {
        this(sender, null);
    }

    private void buildUI() {
        setLayout(new BorderLayout(6, 6));
        JPanel top = new JPanel(new GridLayout(1, 3));
        top.add(roomLabel);
        top.add(meLabel);
        top.add(opponentLabel);

        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton leaveBtn = new JButton("Rời phòng");
        leaveBtn.addActionListener(e -> {
            sender.accept("{LEAVE}");
            // actual switching back to StartPanel will be performed when server sends LEAVE_OK
            if (onLeaveLocal != null) onLeaveLocal.run(); // optional local action
        });
        bottom.add(leaveBtn);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void updateRoom(String roomId, String me, String opponent) {
        SwingUtilities.invokeLater(() -> {
            roomLabel.setText("Phòng: " + roomId);
            meLabel.setText("Bạn: " + me);
            opponentLabel.setText("Đối thủ: " + (opponent == null || opponent.isEmpty() ? "-" : opponent));
            logArea.setText("");
        });
    }

    public void updatePlayers(String csv) {
        SwingUtilities.invokeLater(() -> {
            String[] arr = csv.split(",");
            if (arr.length >= 1) meLabel.setText("Bạn: " + arr[0]);
            if (arr.length >= 2) opponentLabel.setText("Đối thủ: " + arr[1]);
            appendMessage("Danh sách hiện tại: " + csv);
        });
    }

    public void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void resetForNewRoom() {
        SwingUtilities.invokeLater(() -> {
            roomLabel.setText("Phòng: -");
            meLabel.setText("Bạn: -");
            opponentLabel.setText("Đối thủ: -");
            logArea.setText("");
        });
    }
}
