package client;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * GamePanel: màn chơi với 3 nút MOVE và 2 nút Play again / Leave
 * Popup kết quả sinh động với hình ảnh (WIN/LOSE/DRAW)
 */
public class GamePanel extends JPanel {
    private final JTextArea logArea = new JTextArea(8, 40);

    private final JButton btnRock;
    private final JButton btnPaper;
    private final JButton btnScissors;
    private final JButton btnPlayAgain = new JButton("Chơi lại");
    private final JButton btnLeave = new JButton("Rời phòng");

    private final Consumer<String> sender;
    private final Runnable onLeaveLocal;

    private static final int IMG_SIZE = 100;

    private String playerName; // tên người chơi local

    public GamePanel(Consumer<String> sender, Runnable onLeaveLocal) {
        this.sender = sender;
        this.onLeaveLocal = onLeaveLocal;
        setLayout(new BorderLayout(10, 10));

        // load icons cho các nút chọn
        btnRock = new JButton(resizeIcon(loadIcon("rock.png"), IMG_SIZE, IMG_SIZE));
        btnPaper = new JButton(resizeIcon(loadIcon("paper.png"), IMG_SIZE, IMG_SIZE));
        btnScissors = new JButton(resizeIcon(loadIcon("scissors.png"), IMG_SIZE, IMG_SIZE));

        btnRock.setToolTipText("✊ Rock");
        btnPaper.setToolTipText("✋ Paper");
        btnScissors.setToolTipText("✌ Scissors");

        // log area
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // panel chứa các nút chọn
        JPanel moves = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnRock.setPreferredSize(new Dimension(120, 120));
        btnPaper.setPreferredSize(new Dimension(120, 120));
        btnScissors.setPreferredSize(new Dimension(120, 120));

        moves.add(btnRock);
        moves.add(btnPaper);
        moves.add(btnScissors);
        add(moves, BorderLayout.SOUTH);

        // panel phía trên (play again + leave)
        JPanel topOpts = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        btnPlayAgain.setEnabled(false);
        topOpts.add(btnPlayAgain);
        topOpts.add(btnLeave);
        add(topOpts, BorderLayout.NORTH);

        // gán hành động cho các nút
        btnRock.addActionListener(e -> sendMove("ROCK"));
        btnPaper.addActionListener(e -> sendMove("PAPER"));
        btnScissors.addActionListener(e -> sendMove("SCISSORS"));

        btnPlayAgain.addActionListener(e -> {
            sender.accept("{PLAY_AGAIN}");
            appendLog("Bạn chọn Chơi lại. Đang chờ đối thủ...");
            btnPlayAgain.setEnabled(false);
        });

        btnLeave.addActionListener(e -> {
            sender.accept("{LEAVE}");
            if (onLeaveLocal != null) onLeaveLocal.run();
        });
    }

    /** Gán tên người chơi local */
    public void setPlayerName(String name) {
        this.playerName = name;
    }

    private void sendMove(String move) {
        sender.accept("{MOVE:" + move + "}");
        appendLog("Bạn đã chọn " + move + ". Đang chờ đối thủ...");
        setMoveButtonsEnabled(false);
    }

    public void reset() {
        SwingUtilities.invokeLater(() -> {
            logArea.setText("Trò chơi bắt đầu!\n");
            setMoveButtonsEnabled(true);
            btnPlayAgain.setEnabled(false);
        });
    }

    /** Hiển thị kết quả sinh động bằng popup */
    public void showResult(String result) {
        appendLog(result);

        SwingUtilities.invokeLater(() -> {
            btnPlayAgain.setEnabled(true);

            boolean isDraw = result.contains("Hòa");
            String p1Name = "", p1Move = "";
            String p2Name = "", p2Move = "";

            try {
                if (!isDraw) {
                    String[] parts = result.split(" thắng ");
                    String left = parts[0];   // "dan (ROCK)"
                    String right = parts[1];  // "f (SCISSORS)"

                    p1Name = left.substring(0, left.indexOf("(")).trim();
                    p1Move = left.substring(left.indexOf("(") + 1, left.indexOf(")"));

                    p2Name = right.substring(0, right.indexOf("(")).trim();
                    p2Move = right.substring(right.indexOf("(") + 1, right.indexOf(")"));
                }
            } catch (Exception e) {
                System.err.println("Không parse được kết quả: " + result);
            }

            // panel popup
            JPanel panel = new JPanel(new BorderLayout(10, 10));

            JLabel lblNames;
            if (isDraw) {
                lblNames = new JLabel("HÒA RỒI!", SwingConstants.CENTER);
            } else {
                lblNames = new JLabel(p1Name + "  VS  " + p2Name, SwingConstants.CENTER);
            }
            lblNames.setFont(new Font("Arial", Font.BOLD, 16));
            panel.add(lblNames, BorderLayout.NORTH);

            // moves + kết quả 2 bên
            JPanel movesPanel = new JPanel(new GridLayout(1, 2, 20, 10));
            movesPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            if (isDraw) {
                movesPanel.add(buildPlayerResultPanel(p1Name, p1Move, "DRAW"));
                movesPanel.add(buildPlayerResultPanel(p2Name, p2Move, "DRAW"));
            } else {
                movesPanel.add(buildPlayerResultPanel(p1Name, p1Move, "WIN"));
                movesPanel.add(buildPlayerResultPanel(p2Name, p2Move, "LOSE"));
            }

            panel.add(movesPanel, BorderLayout.CENTER);

            // hiển thị popup
            JOptionPane.showMessageDialog(
                    this,
                    panel,
                    "KẾT QUẢ",
                    JOptionPane.PLAIN_MESSAGE
            );
        });
    }

    /** Helper: dựng panel kết quả cho 1 người chơi */
    private JPanel buildPlayerResultPanel(String name, String move, String outcome) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ảnh move
        JLabel lblMove = new JLabel(resizeIcon(loadIcon(move.toLowerCase() + ".png"), IMG_SIZE, IMG_SIZE));
        lblMove.setHorizontalAlignment(SwingConstants.CENTER);

        // chữ WIN/LOSE/DRAW
        JLabel lblOutcome = new JLabel(outcome, SwingConstants.CENTER);
        lblOutcome.setFont(new Font("Arial", Font.BOLD, 14));

        Color bgColor;
        switch (outcome) {
            case "WIN":
                lblOutcome.setForeground(new Color(0, 128, 0));
                bgColor = new Color(200, 255, 200);
                break;
            case "LOSE":
                lblOutcome.setForeground(Color.RED.darker());
                bgColor = new Color(255, 200, 200);
                break;
            default: // DRAW
                lblOutcome.setForeground(Color.DARK_GRAY);
                bgColor = new Color(220, 220, 220);
                break;
        }

        // tên người chơi
        JLabel lblName = new JLabel(name, SwingConstants.CENTER);
        lblName.setFont(new Font("Arial", Font.PLAIN, 13));

        p.add(lblName, BorderLayout.NORTH);
        p.add(lblMove, BorderLayout.CENTER);
        p.add(lblOutcome, BorderLayout.SOUTH);

        // set màu nền
        p.setBackground(bgColor);
        p.setOpaque(true);

        return p;
    }

    private void appendLog(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setMoveButtonsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            btnRock.setEnabled(enabled);
            btnPaper.setEnabled(enabled);
            btnScissors.setEnabled(enabled);
        });
    }

    /** Helper: load icon từ thư mục resources (src/client/images) */
    private ImageIcon loadIcon(String name) {
        java.net.URL imgURL = getClass().getResource("/client/images/" + name);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Không tìm thấy ảnh: " + name);
            return new ImageIcon(); // fallback rỗng
        }
    }

    /** Helper: resize icon về kích thước cố định */
    private ImageIcon resizeIcon(ImageIcon icon, int width, int height) {
        if (icon == null || icon.getIconWidth() <= 0) return new ImageIcon();
        Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
}
