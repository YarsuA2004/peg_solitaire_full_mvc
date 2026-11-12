import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Board {
    /**
     * A legal jump from a peg's starting cell to its destination cell.
     */
    public static class Move {
        private final int startRow;
        private final int startCol;
        private final int endRow;
        private final int endCol;

        public Move(int startRow, int startCol, int endRow, int endCol) {
            this.startRow = startRow;
            this.startCol = startCol;
            this.endRow = endRow;
            this.endCol = endCol;
        }

        public int getStartRow() { return startRow; }
        public int getStartCol() { return startCol; }
        public int getEndRow() { return endRow; }
        public int getEndCol() { return endCol; }
    }

    private final int boardSize;
    private final int boardType;
    private final int[][] grid;

    // Grid cells -> 1 == peg, 0 == empty, -1 == wall
    // boardType -> 1 == English, 2 == Hexagon, 3 == Diamond
    public Board(int size, int boardType) {
        if (size <= 0) {
            throw new IllegalArgumentException("Board Size must be positive, non-zero integer.");
        }
        if (boardType != 1 && boardType != 2 && boardType != 3) {
            throw new IllegalArgumentException("Board type must be English (1), Hexagon (2), or Diamond (3).");
        }
        
        this.boardSize = size;

        // English board type
        if (boardType == 1) {
            // If size is less than 7, or even, or not cleanly divisible by 4, throw exception
            if ((size < 7) || (size % 2 != 1) || ((size + 1) % 4 != 0)) {
                throw new IllegalArgumentException("English board type only works with the sizes calculated by this algorithm: (7 + 4x), i.e. 7, 11, 15, 19, 23 ...");
            }
            else {
                this.grid = englishBoardCalculation(size);
            }
        }
        // Hexagon board type
        else if (boardType == 2) {
            // If size is less than 7, or even, or not cleanly divisible by 4, throw exception
            if ((size < 7) || (size % 2 != 1) || ((size + 1) % 4 != 0)) {
                throw new IllegalArgumentException("Hexagon board type only works with the sizes calculated by this algorithm: (7 + 4x), i.e. 7, 11, 15, 19, 23 ...");
            }
            else {
                this.grid = hexagonBoardCalculation(size);
            }
        }
        // Diamond board type
        else {
            // If size is less than 5, or even, throw exception
            if ((size < 5) || (size % 2 != 1)) {
                throw new IllegalArgumentException("Diamond board type only works with the sizes calculated by this algorithm: (5 + 2x), i.e. 5, 7, 9, 11, 13 ...");
            }
            else {
                this.grid = diamondBoardCalculation(size);
            }
        }

        this.boardType = boardType;

        this.grid[(boardSize / 2)][(boardSize / 2)] = 0;

    }

    /**
     * Randomizes peg and empty-cell positions without moving walls.
     * The number of pegs and empty cells stays the same.
     * Returns false if no swap is possible (all playable cells match).
     */
    public boolean randomize() {
        List<int[]> positions = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (grid[row][col] != -1) {
                    positions.add(new int[] {row, col});
                    values.add(grid[row][col]);
                }
            }
        }

        if (!values.contains(0) || !values.contains(1)) {
            return false;
        }

        Collections.shuffle(values);

        // Guarantee a different arrangement even if shuffle returns the original.
        boolean changed = false;
        for (int i = 0; i < positions.size(); i++) {
            int[] pos = positions.get(i);
            if (grid[pos[0]][pos[1]] != values.get(i)) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            int pegIndex = values.indexOf(1);
            int emptyIndex = values.indexOf(0);
            Collections.swap(values, pegIndex, emptyIndex);
        }

        for (int i = 0; i < positions.size(); i++) {
            int[] pos = positions.get(i);
            grid[pos[0]][pos[1]] = values.get(i);
        }
        return true;
    }

    /**
     * Saves the complete current board, including its size, type, and every cell.
     * Format: magic header, board size, board type, then one space-separated row per line.
     */
    public void save(Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("PEG_SOLITAIRE_V1");
            writer.newLine();
            writer.write(Integer.toString(boardSize));
            writer.newLine();
            writer.write(Integer.toString(boardType));
            writer.newLine();
            for (int row = 0; row < boardSize; row++) {
                for (int col = 0; col < boardSize; col++) {
                    if (col > 0) writer.write(" ");
                    writer.write(Integer.toString(grid[row][col]));
                }
                writer.newLine();
            }
        }
    }

    /**
     * Restores a saved board, rejecting files with invalid dimensions, cells,
     * or walls that do not match the selected board shape.
     */
    public static Board load(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            if (!"PEG_SOLITAIRE_V1".equals(reader.readLine())) {
                throw new IOException("Not a supported Peg Solitaire save file.");
            }

            try {
                int size = Integer.parseInt(requireLine(reader, "board size").trim());
                int type = Integer.parseInt(requireLine(reader, "board type").trim());
                // Reuse the existing board-size/type validation and shape generation.
                Board loaded = new Board(size, type);

                for (int row = 0; row < size; row++) {
                    String[] cells = requireLine(reader, "board row " + (row + 1))
                            .trim().split("\\s+");
                    if (cells.length != size) {
                        throw new IOException("Incorrect number of cells in row " + (row + 1) + ".");
                    }
                    for (int col = 0; col < size; col++) {
                        int value = Integer.parseInt(cells[col]);
                        if (value != -1 && value != 0 && value != 1) {
                            throw new IOException("Invalid cell value at row " + (row + 1) + ".");
                        }
                        // A saved game can change peg occupancy, but never the walls.
                        if ((value == -1) != (loaded.grid[row][col] == -1)) {
                            throw new IOException("Saved walls do not match this board type.");
                        }
                        loaded.grid[row][col] = value;
                    }
                }
                if (reader.readLine() != null) {
                    throw new IOException("Unexpected extra data after the board.");
                }
                return loaded;
            } catch (IllegalArgumentException ex) {
                throw new IOException("Invalid saved board: " + ex.getMessage(), ex);
            }
        }
    }

    private static String requireLine(BufferedReader reader, String description) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Save file is missing " + description + ".");
        }
        return line;
    }

    // PRE:     A valid boardSize integer (x >= 5; x % 2 == 1) (boardSize == 5 || 7 || 9 || 11 || 13 || 15 || 17 ...)
    // POST:    The function will return a grid with the corners removed (turned into walls)
    public int[][] diamondBoardCalculation(int boardSize) {
        int[][] grid = new int[boardSize][boardSize];

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (isInsideDiamond(row, col, (Math.floorDiv(boardSize, 2)), (Math.floorDiv(boardSize, 2)), (Math.floorDiv(boardSize, 2)))) {
                    grid[row][col] = 1;
                    continue;
                }
                grid[row][col] = -1;
            }
        }

        return grid;
    }

    // PRE:     valid grid cells for selected cell and the center cell of the grid
    // POST:    the function will return true if the selected cell is within the grid diamond, or false if not
    public boolean isInsideDiamond(int row, int col, int centerRow, int centerCol, double radius) {
        // Translate relative to center
        double dx = Math.abs(row - centerRow);
        double dy = Math.abs(col - centerCol);

        return (dx + dy <= radius) && (dx <= radius) && (dy <= radius);
    }

    // PRE:     A valid boardSize integer (boardSize == 7 || 11 || 15 || 19 || 23 ...) because the board type will only work this way
    // POST:    The function will return a hexagon grid (corners turned into walls)
    public int[][] hexagonBoardCalculation(int boardSize) {
        int[][] grid = new int[boardSize][boardSize];

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (isInsideHexagon(row, col, (Math.floorDiv(boardSize, 2)), (Math.floorDiv(boardSize, 2)), (Math.floorDiv(boardSize, 2)))) {
                    grid[row][col] = 1;
                    continue;
                }
                grid[row][col] = -1;
            }
        }

        return grid;
    }

    // PRE:     valid grid cells for selected cell and the center cell of the grid
    // POST:    the function will return true if the selected cell is within the grid hexagon, or false if not
    public boolean isInsideHexagon(int row, int col, int centerRow, int centerCol, double radius) {
        // Translate relative to center
        double dx = Math.abs(row - centerRow);
        double dy = Math.abs(col - centerCol);

        return (dx + dy <= radius * 1.5) && (dx <= radius) && (dy <= radius);
    }

    // PRE:     A valid boardSize integer (boardSize == 7 || 11 || 15 || 19 || 23 ...) because the english board type will only be even this way
    // POST:    The function will return a grid with the corners removed (turned into walls).
    public int[][] englishBoardCalculation(int boardSize) {
        int[][] grid = new int[boardSize][boardSize];
        int removedSquareSize = (Math.ceilDiv(boardSize, 2));               // 7-> 4, 11-> 6, 15->8, 19->10, 23-> 12, ...
        int individualRemovedSquareSize = removedSquareSize / 2;            // 4->2, 6->3, 8->4, 10->5, 12->6, ...
        int indRemSqArrayIndexHelper = individualRemovedSquareSize - 1;     // For easier array calculations: (0..1) (1), (0..2) (2), (0..3) (3), ...

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                // If the grid cell falls within the square of one of the wall corners, set cell as a wall. Else, set as a peg.
                if (row <= indRemSqArrayIndexHelper && col <= indRemSqArrayIndexHelper ||                                       // Top-Left Square
                    row <= indRemSqArrayIndexHelper && col >= (boardSize - (indRemSqArrayIndexHelper + 1)) ||                   // Top-Right Square
                    row >= (boardSize - (indRemSqArrayIndexHelper + 1)) && col <= indRemSqArrayIndexHelper ||                   // Bottom-Left Square
                    row >= (boardSize - (indRemSqArrayIndexHelper + 1)) && col >= (boardSize - (indRemSqArrayIndexHelper + 1))  // Bottom-Right Square
                ) {
                    grid[row][col] = -1;
                    continue;
                }
                grid[row][col] = 1;
            }
        }

        return grid;
    }

    // PRE:     
    // POST:    returns the size of the board
    public int getSize() {
        return boardSize;
    }

    // PRE:     
    // POST:    returns the type of the board
    public int getType() {
        return boardType;
    }

    /**
     * Returns every currently legal jump. The board is not changed.
     * The order is deterministic: cells are scanned top-to-bottom,
     * left-to-right, using the direction order listed below.
     */
    public List<Move> getLegalMoves() {
        List<Move> moves = new ArrayList<>();

        int[][] directions = {
            {-2, 0}, {2, 0}, {0, -2}, {0, 2},
            {-2, -2}, {-2, 2}, {2, -2}, {2, 2}
        };

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (grid[row][col] != 1) {
                    continue;
                }

                for (int[] direction : directions) {
                    int endRow = row + direction[0];
                    int endCol = col + direction[1];

                    if (!isValidPosition(endRow, endCol)
                            || grid[endRow][endCol] != 0) {
                        continue;
                    }

                    int middleRow = (row + endRow) / 2;
                    int middleCol = (col + endCol) / 2;

                    if (grid[middleRow][middleCol] == 1) {
                        moves.add(new Move(row, col, endRow, endCol));
                    }
                }
            }
        }

        return moves;
    }

    // PRE:     a valid row and integer (row >= 0 && row < boardSize && col >= 0 && col < boardSize)
    // POST:    returns the integer representation of a cell (1 == peg, 0 == empty, -1 == wall)
    public int getCell(int row, int col) {
        return grid[row][col];
    }

    // PRE:     
    // POST:    returns the number of pegs on the board
    public int countPegs() {
        int count = 0;

        for (int[] row : grid) {
            for (int cell : row) {
                if (cell == 1) {
                    count++;
                }
            }
        }

        return count;
    }

    // PRE:     
    // POST:    returns true if the specified position is within the bounds of the board
    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < boardSize 
                && col >= 0 && col < boardSize;
    }

    // PRE:     
    // POST:    applies a movement action on the board if (start == 1 && end == 0 && midpoint == 1). Returns true if the function worked.
    public boolean movePeg(int startRow, int startCol, int endRow, int endCol) {
        if (!isValidPosition(startRow, startCol) || !isValidPosition(endRow, endCol)) {
            return false;
        }

        if (grid[startRow][startCol] != 1 || grid[endRow][endCol] != 0) {
            return false;
        }

        int rowDiff = endRow - startRow;
        int colDiff = endCol - startCol;

        boolean horizontal = Math.abs(colDiff) == 2 && rowDiff == 0;
        boolean vertical = Math.abs(rowDiff) == 2 && colDiff == 0;
        boolean diagonal = Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 2;

        if (!horizontal && !vertical && !diagonal) {
            return false;
        }

        int middleRow = (startRow + endRow) / 2;
        int middleCol = (startCol + endCol) / 2;

        if (grid[middleRow][middleCol] != 1) {
            return false;
        }

        // Apply the jump
        grid[startRow][startCol] = 0;
        grid[middleRow][middleCol] = 0;
        grid[endRow][endCol] = 1;

        return true;
    }

    // PRE:
    // POST:    returns true if there are any legal moves left to make on the board.
    public boolean hasLegalMoves() {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (grid[row][col] != 1) {
                    continue;
                }

                // Check jumps in all directions
                int[][] directions = {
                    {-2, 0},    // Up
                    {2, 0},     // Down
                    {0, -2},    // Left
                    {0, 2},     // Right
                    {-2, -2},   // Top-Left
                    {-2, 2},    // Top-Right
                    {2, -2},    // Bottom-Left
                    {2, 2}      // Bottom-Right
                };

                // For each direction, 
                // if the end cell is a valid cell (within bounds of grid) and is empty, 
                // and if the cell between the start and end cells is a peg,
                // return true. 
                for (int[] direction : directions) {
                    int endRow = row + direction[0];
                    int endCol = col + direction[1];

                    if (isValidPosition(endRow, endCol)
                            && grid[endRow][endCol] == 0) {
                        int middleRow = (row + endRow) / 2;
                        int middleCol = (col + endCol) / 2;

                        if (grid[middleRow][middleCol] == 1) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
