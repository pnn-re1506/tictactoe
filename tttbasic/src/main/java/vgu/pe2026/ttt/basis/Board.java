package vgu.pe2026.ttt.basis;

// Represents the Tic-Tac-Toe game board.
// The board state is managed as a 1D array of 9 integers, representing the 3x3 grid.
public class Board {

    public static final int EMPTY = 0;
    public static final int HUMAN = 1;
    public static final int COMPUTER = 2;

    // 1D array representing the 3x3 board (indices 0 to 8)
    private int[] cells = new int[9];
    // Predefined winning combinations (indices in the 1D array)
    private int[][] winLines = {
            { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, // rows
            { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, // columns
            { 0, 4, 8 }, { 2, 4, 6 } // diagonals
    };

    // Retrieves the value of a specific cell.
    // cell parameter is 1-based index (1 to 9)
    public int getCell(int cell) {
        return cells[cell - 1]; // Convert 1-based to 0-based index
    }

    // Checks if a cell is currently empty.
    public boolean isEmpty(int cell) {
        return cells[cell - 1] == 0;
    }

    // Validates if a user's move is within bounds and the chosen cell is empty.
    public boolean isValidMove(int cell) {
        return cell >= 1 && cell <= 9 && isEmpty(cell);
    }

    // Places a mark (HUMAN or COMPUTER) on the board.
    public void place(int cell, int mark) {
        cells[cell - 1] = mark;
    }

    // Checks if the given player (mark) has achieved any of the winning lines.
    public boolean hasWon(int mark) {
        for (int[] line : winLines) {
            if (cells[line[0]] == mark && cells[line[1]] == mark && cells[line[2]] == mark)
                return true;
        }
        return false;
    }

    // Checks if all cells on the board are filled, resulting in a draw if no one has won.
    public boolean isFull() {
        for (int num : cells) {
            if (num == 0)
                return false;
        }
        return true;
    }

    // Finds the first available empty cell. Used by the simple ComputerPlayer AI.
    // Returns 1-based index of the first empty cell, or -1 if full.
    public int firstEmptyCell() {
        for (int i = 0; i < 9; i++) {
            if (cells[i] == 0)
                return i + 1; // Return 1-based index
        }
        return -1;
    }

    // Serializes the board state into a 9-character string for network transmission.
    // Example: "100200000" where 1=HUMAN, 2=COMPUTER, 0=EMPTY
    public String toLine() {
        StringBuilder builder = new StringBuilder(9);
        for (int cell : cells) {
            builder.append(cell);
        }
        return builder.toString();
    }

    // Deserializes a 9-character string back into a Board object.
    // Used by the Server to reconstruct state from Client request, and vice versa.
    public static Board fromLine(String line) {
        if (line == null || line.length() != 9) {
            throw new IllegalArgumentException("Board must contain exactly 9 cells.");
        }

        Board board = new Board();
        for (int i = 0; i < 9; i++) {
            char value = line.charAt(i);
            if (value < '0' || value > '2') {
                throw new IllegalArgumentException("Board cells must be 0, 1, or 2.");
            }
            // Convert character to integer ('1' - '0' = 1)
            board.cells[i] = value - '0';
        }
        return board;
    }

    // Prints a visual representation of the 3x3 board to the console.
    public void printMatrix() {
        System.out.println();
        for (int row = 0; row < 3; row++) {
            System.out.print("| ");
            for (int column = 0; column < 3; column++) {
                System.out.print(cells[row * 3 + column]);
                System.out.print(" | ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
