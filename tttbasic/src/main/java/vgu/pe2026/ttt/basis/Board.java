package vgu.pe2026.ttt.basis;

public class Board {
    

    private int[] cells = new int[9];
    private int[][] winLines = {
            { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, // rows
            { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, // columns
            { 0, 4, 8 }, { 2, 4, 6 } // diagonals
    };

    public int getCell(int cell) {
        return cells[cell - 1];
    }

    public boolean isEmpty(int cell) {
        return cells[cell - 1] == 0;
    }

    public void place(int cell, int mark) {
        cells[cell - 1] = mark;
    }

    public boolean hasWon(int mark) {
        for (int[] line : winLines) {
            if (cells[line[0]] == mark && cells[line[1]] == mark && cells[line[2]] == mark)
                return true;
        }
        return false;
    }

    public boolean isFull() {
        for (int num : cells) {
            if (num == 0)
                return false;
        }
        return true;
    }

    public int firstEmptyCell() {
        for (int i = 0; i < 9; i++) {
            if (cells[i] == 0)
                return i + 1;
        }
        return -1;
    }

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