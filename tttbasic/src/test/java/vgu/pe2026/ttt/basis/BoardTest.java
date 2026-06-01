package vgu.pe2026.ttt.basis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    @Test
    // verify that a newly created board has all its cells empty.
    void testNewBoardIsEmpty() {
        Board board = new Board();
        for (int cell = 1; cell <= 9; cell++) {
            assertTrue(board.isEmpty(cell));
        }
    }

    @Test
    // verify that placing a mark on a cell correctly updates its state to non-empty.
    void testPlaceMark() {
        Board board = new Board();
        board.place(5, 1);
        assertFalse(board.isEmpty(5));
    }

    @Test
    // verify that placing marks in a complete horizontal row is recognized as a win.
    void testRowWin() {
        Board board = new Board();
        board.place(1, 1);
        board.place(2, 1);
        board.place(3, 1);
        assertTrue(board.hasWon(1));
    }

    @Test
    // verify that placing marks in a complete diagonal line is recognized as a win.
    void testDiagonalWin() {
        Board board = new Board();
        board.place(1, 2);
        board.place(5, 2);
        board.place(9, 2);
        assertTrue(board.hasWon(2));
    }

    @Test
    // verify that a board without a complete line of marks is not considered a win.
    void testNoWinningLine() {
        Board board = new Board();
        board.place(1, 1);
        board.place(2, 1);
        board.place(5, 2);
        assertFalse(board.hasWon(1));
        assertFalse(board.hasWon(2));
    }

    @Test
    // verify that the board correctly identifies the first available free cell.
    void testFirstEmptyCell() {
        Board board = new Board();
        board.place(1, 1);
        board.place(2, 2);
        assertEquals(3, board.firstEmptyCell());
    }

    @Test
    // verify that a completely filled board is recognized as full and has no empty cells.
    void testBoardIsFull() {
        Board board = new Board();
        for (int cell = 1; cell <= 9; cell++) {
            board.place(cell, (cell % 2) + 1);
        }
        assertTrue(board.isFull());
        assertEquals(-1, board.firstEmptyCell());
    }
}
