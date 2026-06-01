package vgu.pe2026.ttt.basis;

import org.junit.jupiter.api.Test;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HumanPlayerTest {

    @Test
    // verify that a valid numeric input within range is accepted immediately.
    void testValidInput() {
        Board board = new Board();
        HumanPlayer human = new HumanPlayer(new Scanner("4\n"));
        assertEquals(4, human.chooseCell(board));
    }

    @Test
    // verify that non-numeric input is rejected and the player is prompted until a valid number is provided.
    void testNonNumericInput() {
        Board board = new Board();
        HumanPlayer human = new HumanPlayer(new Scanner("abc\n7\n"));
        assertEquals(7, human.chooseCell(board));
    }

    @Test
    // verify that numbers outside the 1-9 range are rejected and the player is prompted until a valid input is provided.
    void testOutOfRangeInput() {
        Board board = new Board();
        HumanPlayer human = new HumanPlayer(new Scanner("0\n10\n6\n"));
        assertEquals(6, human.chooseCell(board));
    }

    @Test
    // verify that choosing an already occupied cell is rejected and the player is prompted until a free cell is chosen.
    void testTakenCell() {
        Board board = new Board();
        board.place(5, 2); // mark cell 5 as occupied
        
        HumanPlayer human = new HumanPlayer(new Scanner("5\n8\n"));
        assertEquals(8, human.chooseCell(board));
    }
}
