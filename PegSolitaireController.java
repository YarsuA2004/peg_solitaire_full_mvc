import javax.swing.Timer;
import java.io.IOException;
import java.nio.file.Path;

/** Coordinates model actions, UI events, and the automatic-play timer. */
public final class PegSolitaireController {
    private static final int AUTOMATIC_MOVE_DELAY_MS = 650;
    private final PegSolitaireView view;
    private final Timer automaticTimer;
    private PegSolitaireGame currentGame;
    private int selectedRow = -1;
    private int selectedCol = -1;

    public PegSolitaireController(PegSolitaireView view) {
        this.view = view;
        automaticTimer = new Timer(AUTOMATIC_MOVE_DELAY_MS, e -> automaticStep());
        view.onSave(e -> save());
        view.onLoad(e -> load());
        view.onRandomize(e -> randomize());
        view.onNewGame(e -> newGame());
        view.onModeChanged(e -> changeMode());
        view.onAutomaticControl(e -> toggleAutomaticPlay());
        installGame(new PegSolitaireGame(7, 1));
    }

    private void installGame(PegSolitaireGame replacement) {
        automaticTimer.stop();
        currentGame = replacement;
        clearSelection();
        view.showGame(replacement, this::handleCellClick);
        view.renderBoard(currentGame, selectedRow, selectedCol);
        view.setAutomaticControls(view.isAutomaticModeSelected(), false);
        view.setStatus("Pegs remaining: " + currentGame.countPegs()
                + (view.isAutomaticModeSelected()
                ? " | Automatic mode selected. Press Start Automatic to begin."
                : " | Select a peg to move."));
    }

    private void clearSelection() { selectedRow = -1; selectedCol = -1; }
    private void refresh() { view.renderBoard(currentGame, selectedRow, selectedCol); }

    private void handleCellClick(int row, int col) {
        if (view.isAutomaticModeSelected()) {
            view.setStatus("Manual moves are disabled in Automatic mode.");
            return;
        }
        if (selectedRow == -1) {
            if (currentGame.getCell(row, col) == 1) {
                selectedRow = row;
                selectedCol = col;
                view.setStatus("Peg selected. Click an empty destination.");
            } else {
                view.setStatus("Select a cell containing a peg.");
            }
        } else if (row == selectedRow && col == selectedCol) {
            clearSelection();
            view.setStatus("Selection cancelled.");
        } else {
            boolean moved = currentGame.movePeg(selectedRow, selectedCol, row, col);
            clearSelection();
            if (moved) updateGameStatus("Good move!");
            else view.setStatus("Invalid move. Select a peg and try again.");
        }
        refresh();
    }

    private void updateGameStatus(String prefix) {
        int pegs = currentGame.countPegs();
        if (pegs == 1) view.setStatus("Congratulations! You won with one peg!");
        else if (!currentGame.hasLegalMoves()) view.setStatus("Game over! Pegs remaining: " + pegs);
        else view.setStatus(prefix + " Pegs remaining: " + pegs);
    }

    private void save() {
        Path path = view.chooseSavePath();
        if (path == null) return;
        try {
            currentGame.saveGame(path);
            view.showInformation("Game saved to:\n" + path.toAbsolutePath());
        } catch (IOException | SecurityException ex) {
            view.showError("Save Failed", ex.getMessage());
        }
    }

    private void load() {
        Path path = view.chooseLoadPath();
        if (path == null) return;
        try {
            PegSolitaireGame loaded = PegSolitaireGame.loadGame(path);
            installGame(loaded);
            view.setBoardSettings(loaded.getBoardSize(), loaded.getBoardType());
        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            view.showError("Load Failed", ex.getMessage());
        }
    }

    private void randomize() {
        automaticTimer.stop();
        clearSelection();
        boolean changed = currentGame.randomizeBoard();
        refresh();
        if (changed) updateGameStatus("Board randomized!");
        else view.setStatus("Cannot randomize: all playable cells have the same value.");
        view.setAutomaticControls(view.isAutomaticModeSelected(), false);
    }

    private void newGame() {
        try {
            PegSolitaireGame replacement = new PegSolitaireGame(
                    view.getRequestedBoardSize(), view.getRequestedBoardType());
            installGame(replacement);
        } catch (IllegalArgumentException ex) {
            view.showError("Invalid Game Settings", ex.getMessage());
        }
    }

    private void changeMode() {
        if (currentGame == null) return;
        automaticTimer.stop();
        clearSelection();
        refresh();
        boolean automatic = view.isAutomaticModeSelected();
        view.setAutomaticControls(automatic, false);
        view.setStatus(automatic
                ? "Automatic mode selected. Press Start Automatic to begin."
                : "Manual mode. Select a peg to move.");
    }

    private void toggleAutomaticPlay() {
        if (automaticTimer.isRunning()) {
            automaticTimer.stop();
            view.setStatus("Automatic play paused. Press Start Automatic to continue.");
        } else if (!view.isAutomaticModeSelected()) {
            view.setStatus("Select Automatic mode before starting.");
        } else if (currentGame.countPegs() <= 1 || !currentGame.hasLegalMoves()) {
            showAutomaticFinishedStatus();
        } else {
            automaticTimer.start();
            view.setStatus("Automatic play running...");
        }
        view.setAutomaticControls(view.isAutomaticModeSelected(), automaticTimer.isRunning());
    }

    private void automaticStep() {
        if (!currentGame.performAutomaticStep()) {
            automaticTimer.stop();
            showAutomaticFinishedStatus();
        } else {
            refresh();
            if (currentGame.countPegs() == 1 || !currentGame.hasLegalMoves()) {
                automaticTimer.stop();
                showAutomaticFinishedStatus();
            } else {
                view.setStatus("Automatic play... Pegs remaining: " + currentGame.countPegs());
            }
        }
        view.setAutomaticControls(view.isAutomaticModeSelected(), automaticTimer.isRunning());
    }

    private void showAutomaticFinishedStatus() {
        int pegs = currentGame.countPegs();
        if (pegs == 1) view.setStatus("Congratulations! Automatic play won with one peg!");
        else view.setStatus("Automatic play finished. No legal moves remain. Pegs: " + pegs);
    }
}
