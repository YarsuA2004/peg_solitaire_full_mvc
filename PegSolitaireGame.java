import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Pure Java game model: no Swing components, listeners, or timers. */
public final class PegSolitaireGame {
    private final Board board;

    public PegSolitaireGame(int size, int type) {
        this(new Board(size, type));
    }

    public PegSolitaireGame(Board board) {
        if (board == null) throw new IllegalArgumentException("Board cannot be null.");
        this.board = board;
    }

    public int getBoardSize() { return board.getSize(); }
    public int getBoardType() { return board.getType(); }
    public int getCell(int row, int col) { return board.getCell(row, col); }
    public int countPegs() { return board.countPegs(); }
    public boolean hasLegalMoves() { return board.hasLegalMoves(); }
    public boolean isValidPosition(int row, int col) { return board.isValidPosition(row, col); }
    public List<Board.Move> getLegalMoves() { return board.getLegalMoves(); }

    public boolean movePeg(int startRow, int startCol, int endRow, int endCol) {
        return board.movePeg(startRow, startCol, endRow, endCol);
    }

    /** Advances the first legal move; returns false if none remain. */
    public boolean performAutomaticStep() {
        List<Board.Move> moves = board.getLegalMoves();
        if (moves.isEmpty()) return false;
        Board.Move move = moves.get(0);
        return board.movePeg(move.getStartRow(), move.getStartCol(),
                move.getEndRow(), move.getEndCol());
    }

    /** Swaps playable pegs and empty cells without moving walls. */
    public boolean randomizeBoard() { return board.randomize(); }
    public void saveGame(Path path) throws IOException { board.save(path); }
    public static PegSolitaireGame loadGame(Path path) throws IOException {
        return new PegSolitaireGame(Board.load(path));
    }
}
