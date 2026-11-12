import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

/** Builds and owns the application's Swing controls. No game actions live here. */
public class PegSolitaireView extends JFrame {
    private final JPanel gameContainer = new JPanel(new BorderLayout());
    private final JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
    private GameBoardPanel boardPanel;
    private final JButton saveButton = new JButton("Save");
    private final JButton loadButton = new JButton("Load");
    private final JButton randomizeButton = new JButton("Randomize");
    private final JButton replayButton = new JButton("Replay Board");
    private final JButton newGameButton = new JButton("New Game");
    private final JButton undoButton = new JButton("Undo");
    private final JButton hintButton = new JButton("Hint");
    private final JButton automaticButton = new JButton("Start Automatic");
    private final JComboBox<String> modeSelector = new JComboBox<>(
            new String[]{"Manual Mode", "Automatic Mode"});
    private final JRadioButton englishButton = new JRadioButton("English", true);
    private final JRadioButton hexagonButton = new JRadioButton("Hexagon");
    private final JRadioButton diamondButton = new JRadioButton("Diamond");
    private final JTextField boardSizeField = new JTextField("7", 3);

    public PegSolitaireView() {
        super("Peg Solitaire");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 700));
        setLayout(new BorderLayout());

        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JPanel north = new JPanel(new BorderLayout());
        north.setBorder(border);
        north.add(new JLabel("Peg Solitaire"), BorderLayout.WEST);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(saveButton);
        controls.add(loadButton);
        controls.add(randomizeButton);
        controls.add(replayButton);
        controls.add(newGameButton);
        controls.add(undoButton);
        controls.add(hintButton);
        controls.add(modeSelector);
        controls.add(automaticButton);
        north.add(controls, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        JPanel west = new JPanel();
        west.setBorder(border);
        west.setLayout(new BoxLayout(west, BoxLayout.Y_AXIS));
        JPanel boardTypePanel = new JPanel(new GridLayout(4, 1));
        boardTypePanel.setBorder(border);
        boardTypePanel.setMaximumSize(new Dimension(200, 200));
        boardTypePanel.add(new JLabel("Select type of board:"));
        ButtonGroup types = new ButtonGroup();
        for (JRadioButton button : new JRadioButton[]{englishButton, hexagonButton, diamondButton}) {
            types.add(button);
            boardTypePanel.add(button);
        }
        west.add(boardTypePanel);
        JPanel sizePanel = new JPanel(new GridLayout(2, 1));
        sizePanel.setMaximumSize(new Dimension(200, 80));
        sizePanel.setBorder(border);
        sizePanel.add(new JLabel("Select Board Size:"));
        JPanel sizeInput = new JPanel();
        sizeInput.add(boardSizeField);
        sizePanel.add(sizeInput);
        west.add(sizePanel);
        add(west, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout());
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        center.add(statusLabel, BorderLayout.NORTH);
        center.add(gameContainer, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // These controls were placeholders in App.java; do not suggest they work yet.
        replayButton.setEnabled(false);
        undoButton.setEnabled(false);
        hintButton.setEnabled(false);
        automaticButton.setEnabled(false);
        pack();
        setLocationRelativeTo(null);
    }

    // Controllers register actions; the View does not decide what they do.
    public void onSave(ActionListener listener) { saveButton.addActionListener(listener); }
    public void onLoad(ActionListener listener) { loadButton.addActionListener(listener); }
    public void onRandomize(ActionListener listener) { randomizeButton.addActionListener(listener); }
    public void onNewGame(ActionListener listener) { newGameButton.addActionListener(listener); }
    public void onModeChanged(ActionListener listener) { modeSelector.addActionListener(listener); }
    public void onAutomaticControl(ActionListener listener) { automaticButton.addActionListener(listener); }

    /** Replaces the board widget when a new game is created or loaded. */
    public void showGame(PegSolitaireGame game,
                         java.util.function.BiConsumer<Integer, Integer> clickListener) {
        boardPanel = new GameBoardPanel(game.getBoardSize());
        boardPanel.onCellClicked(clickListener);
        gameContainer.removeAll();
        gameContainer.add(boardPanel, BorderLayout.CENTER);
        gameContainer.revalidate();
        gameContainer.repaint();
    }

    public void renderBoard(PegSolitaireGame game, int selectedRow, int selectedCol) {
        if (boardPanel != null) boardPanel.render(game, selectedRow, selectedCol);
    }

    public void setStatus(String status) { statusLabel.setText(status); }

    public int getRequestedBoardSize() {
        try {
            return Integer.parseInt(boardSizeField.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Board size must be a whole number.", e);
        }
    }

    public int getRequestedBoardType() {
        if (hexagonButton.isSelected()) return 2;
        if (diamondButton.isSelected()) return 3;
        return 1;
    }

    public void setBoardSettings(int size, int type) {
        boardSizeField.setText(Integer.toString(size));
        switch (type) {
            case 1 -> englishButton.setSelected(true);
            case 2 -> hexagonButton.setSelected(true);
            case 3 -> diamondButton.setSelected(true);
            default -> throw new IllegalArgumentException("Unknown board type: " + type);
        }
    }

    public boolean isAutomaticModeSelected() { return modeSelector.getSelectedIndex() == 1; }

    public void setAutomaticControls(boolean enabled, boolean running) {
        automaticButton.setEnabled(enabled);
        automaticButton.setText(running ? "Stop Automatic" : "Start Automatic");
    }

    /** Returns null if the user cancels. */
    public Path chooseSavePath() {
        JFileChooser chooser = createTextChooser("Save Peg Solitaire Game");
        chooser.setSelectedFile(new File("peg-solitaire-save.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        File selected = chooser.getSelectedFile();
        if (!selected.getName().toLowerCase(Locale.ROOT).endsWith(".txt")) {
            selected = new File(selected.getParentFile(), selected.getName() + ".txt");
        }
        if (selected.exists() && JOptionPane.showConfirmDialog(this,
                "Overwrite " + selected.getName() + "?", "Confirm Overwrite",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return null;
        return selected.toPath();
    }

    /** Returns null if the user cancels. */
    public Path chooseLoadPath() {
        JFileChooser chooser = createTextChooser("Load Peg Solitaire Game");
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile().toPath() : null;
    }

    private JFileChooser createTextChooser(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Peg Solitaire Text Save (*.txt)", "txt"));
        return chooser;
    }

    public void showInformation(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
