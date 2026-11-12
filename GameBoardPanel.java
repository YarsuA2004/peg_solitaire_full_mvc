import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/** Swing representation of one board. No game rules or changes to the model live here. */
public final class GameBoardPanel extends JPanel {
    private final JButton[][] buttons;
    private BiConsumer<Integer, Integer> cellClickListener = (row, col) -> {};

    public GameBoardPanel(int size) {
        super(new GridLayout(size, size, 5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(45, 45, 45));
        buttons = new JButton[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                final int r = row, c = col;
                JButton button = new JButton();
                button.setFont(new Font("SansSerif", Font.BOLD, 24));
                button.setFocusPainted(false);
                button.setPreferredSize(new Dimension(60, 60));
                button.addActionListener(e -> cellClickListener.accept(r, c));
                buttons[row][col] = button;
                add(button);
            }
        }
    }

    /** Supplies a callback; the controller interprets the click. */
    public void onCellClicked(BiConsumer<Integer, Integer> listener) {
        cellClickListener = java.util.Objects.requireNonNull(listener);
    }

    /** Renders the given read-only game data and the controller's selection. */
    public void render(PegSolitaireGame game, int selectedRow, int selectedCol) {
        if (game.getBoardSize() != buttons.length) {
            throw new IllegalArgumentException("Board dimensions do not match this panel.");
        }
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons.length; col++) {
                JButton button = buttons[row][col];
                int cell = game.getCell(row, col);
                button.setText(cell == 1 ? "\u25CF" : "");
                button.setEnabled(cell != -1);
                if (cell == -1) {
                    button.setBackground(new Color(35, 35, 35));
                } else if (cell == 1) {
                    button.setForeground(new Color(80, 80, 80));
                    button.setBackground(new Color(230, 230, 230));
                } else {
                    button.setBackground(new Color(190, 220, 190));
                }
                if (row == selectedRow && col == selectedCol) {
                    button.setBackground(new Color(255, 220, 100));
                }
            }
        }
    }
}
