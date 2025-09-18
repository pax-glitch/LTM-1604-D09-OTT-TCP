package client;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * GamePanel: màn chơi với 3 nút MOVE và 2 nút Play again / Leave
 */
public class GamePanel extends JPanel {
    private final JTextArea logArea = new JTextArea(12, 40);
    private final JButton btnRock = new JButton("✊ Rock");
    private final JButton btnPaper = new JButton("✋ Paper");
    private final JButton btnScissors = new JButton("✌ Scissors");
    private final JButton btnPlayAgain = new JButton("Chơi lại");
    private final JButton btnLeave = new JButton("Rời phòng");

    public GamePanel(Consumer<String> sender, Runnable onLeaveLocal) {
        setLayout(new BorderLayout(6, 6));

        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel moves = new JPanel(new FlowLayout());
        btnRock.addActionListener(e -> {
            sender.accept("{MOVE:ROCK}");
            appendLog("Bạn đã chọn ROCK. Đang chờ đối thủ...");
            setMoveButtonsEnabled(false);
        });
        btnPaper.addActionListener(e -> {
            sender.accept("{MOVE:PAPER}");
            appendLog("Bạn đã chọn PAPER. Đang chờ đối thủ...");
            setMoveButtonsEnabled(false);
        });
        btnScissors.addActionListener(e -> {
            sender.accept("{MOVE:SCISSORS}");
            appendLog("Bạn đã chọn SCISSORS. Đang chờ đối thủ...");
            setMoveButtonsEnabled(false);
        });

        btnRock.setPreferredSize(new Dimension(120, 48));
        btnPaper.setPreferredSize(new Dimension(120, 48));
        btnScissors.setPreferredSize(new Dimension(120, 48));
        moves.add(btnRock);
        moves.add(btnPaper);
        moves.add(btnScissors);
        add(moves, BorderLayout.SOUTH);

        JPanel topOpts = new JPanel(new FlowLayout());
        btnPlayAgain.setEnabled(false);
        btnPlayAgain.addActionListener(e -> {
            sender.accept("{PLAY_AGAIN}");
            appendLog("Bạn chọn Chơi lại. Đang chờ đối thủ...");
            btnPlayAgain.setEnabled(false);
        });
        btnLeave.addActionListener(e -> {
            sender.accept("{LEAVE}");
            // onLeaveLocal should handle UI switching after server confirms LEAVE_OK
            if (onLeaveLocal != null) onLeaveLocal.run();
        });
        topOpts.add(btnPlayAgain);
        topOpts.add(btnLeave);
        add(topOpts, BorderLayout.NORTH);
    }

    public void reset() {
        SwingUtilities.invokeLater(() -> {
            logArea.setText("Trò chơi bắt đầu!\n");
            setMoveButtonsEnabled(true);
            btnPlayAgain.setEnabled(false);
        });
    }

    public void showResult(String result) {
        appendLog(result);
        // enable play again & leave after result
        SwingUtilities.invokeLater(() -> {
            btnPlayAgain.setEnabled(true);
            // leave button already enabled
        });
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
}
