package client;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Start screen: create room / join random / join by id / change name
 */
public class StartPanel extends JPanel {
    private final JLabel nameLabel = new JLabel("Tên: Người chơi");
    private final JLabel roomLabel = new JLabel("ID phòng: -");

    public StartPanel(Consumer<String> sender, Runnable changeNameAction) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        JLabel title = new JLabel("Oẳn Tù Tì");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setAlignmentX(CENTER_ALIGNMENT);

        nameLabel.setAlignmentX(CENTER_ALIGNMENT);
        roomLabel.setAlignmentX(CENTER_ALIGNMENT);

        JButton btnCreate = new JButton("Tạo phòng");
        btnCreate.setAlignmentX(CENTER_ALIGNMENT);
        btnCreate.addActionListener(e -> sender.accept("{CREATE_ROOM}"));

        JButton btnJoinRandom = new JButton("Tham gia ngẫu nhiên");
        btnJoinRandom.setAlignmentX(CENTER_ALIGNMENT);
        btnJoinRandom.addActionListener(e -> sender.accept("{JOIN_RANDOM}"));

        JButton btnJoinById = new JButton("Tham gia bằng ID");
        btnJoinById.setAlignmentX(CENTER_ALIGNMENT);
        btnJoinById.addActionListener(e -> {
            String id = JOptionPane.showInputDialog(this, "Nhập ID phòng:");
            if (id != null && !id.trim().isEmpty()) sender.accept("{JOIN_ROOM:" + id.trim() + "}");
        });

        JButton btnRename = new JButton("Đổi tên");
        btnRename.setAlignmentX(CENTER_ALIGNMENT);
        btnRename.addActionListener(e -> changeNameAction.run());

        JButton btnExit = new JButton("Thoát");
        btnExit.setAlignmentX(CENTER_ALIGNMENT);
        btnExit.addActionListener(e -> System.exit(0));

        add(title);
        add(Box.createVerticalStrut(12));
        add(nameLabel);
        add(Box.createVerticalStrut(6));
        add(roomLabel);
        add(Box.createVerticalStrut(20));
        add(btnCreate);
        add(Box.createVerticalStrut(8));
        add(btnJoinRandom);
        add(Box.createVerticalStrut(8));
        add(btnJoinById);
        add(Box.createVerticalStrut(8));
        add(btnRename);
        add(Box.createVerticalStrut(8));
        add(btnExit);
    }

    public void setPlayerName(String name) {
        SwingUtilities.invokeLater(() -> nameLabel.setText("Tên: " + name));
    }

    public void setRoomId(String id) {
        SwingUtilities.invokeLater(() -> roomLabel.setText("ID phòng: " + id));
    }
}
