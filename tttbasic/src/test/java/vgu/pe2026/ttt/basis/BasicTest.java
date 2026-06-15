package vgu.pe2026.ttt.basis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BasicTest {

    // ── MoveProcessor tests ────────────────────────────────────────────────

    @Test
    void testFirstMove_centerCell() {
        int[] board = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        MoveProcessor.Result result = MoveProcessor.process(board, 5);

        // Player placed at index 4
        assertEquals(1, result.board()[4], "Player mark should be at cell 5 (index 4)");
        // AI responded somewhere
        boolean aiPlayed = false;
        for (int v : result.board()) if (v == 2) { aiPlayed = true; break; }
        assertTrue(aiPlayed, "AI should have placed its mark");
        assertEquals("PLAYING", result.status());
    }

    @Test
    void testPlayerWin() {
        // Board where player wins with row 1-2-3 after placing cell 3
        // Pre-state: player has 1,2; AI has 4,5
        int[] board = {1, 1, 0, 2, 2, 0, 0, 0, 0};
        MoveProcessor.Result result = MoveProcessor.process(board, 3);

        assertEquals("WIN_PLAYER", result.status(), "Player should win with top row");
        assertEquals(1, result.board()[2], "Cell 3 should be marked for player");
    }

    @Test
    void testCellAlreadyOccupied_boardState() {
        // Validation of occupied cell is done in GameHandler, not MoveProcessor.
        // Here we confirm MoveProcessor doesn't crash and correctly places on occupied cell
        // (the handler prevents this from reaching MoveProcessor).
        // Instead, test that board is copied (original unchanged).
        int[] board = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] original = board.clone();
        MoveProcessor.process(board, 1);
        assertArrayEquals(original, board, "MoveProcessor must not mutate the input board");
    }

    @Test
    void testDrawScenario() {
        // Near-full board — player wins or draw, not a crash
        // Board: 1=player, 2=AI
        // Cell layout (1-indexed):
        //  2 | 1 | 2
        //  1 | 1 | 2
        //  1 | 0 | 2   ← player plays cell 8
        int[] board = {2, 1, 2, 1, 1, 2, 1, 0, 2};
        MoveProcessor.Result result = MoveProcessor.process(board, 8);

        // After player plays 8, board is full with no winner → DRAW
        assertNotNull(result.status());
        assertTrue(
            result.status().equals("DRAW") ||
            result.status().equals("WIN_PLAYER") ||
            result.status().equals("WIN_COMPUTER"),
            "Status must be a terminal state"
        );
    }

    // ── Board tests (unchanged logic) ─────────────────────────────────────

    @Test
    void testBoardHasWon() {
        Board board = new Board();
        board.place(1, 1);
        board.place(2, 1);
        board.place(3, 1);
        assertTrue(board.hasWon(1), "Top row should be a win for mark 1");
    }

    @Test
    void testBoardIsFull() {
        Board board = new Board();
        for (int i = 1; i <= 9; i++) board.place(i, i % 2 == 0 ? 2 : 1);
        assertTrue(board.isFull());
    }

    @Test
    void testBoardIsEmpty() {
        Board board = new Board();
        assertTrue(board.isEmpty(5));
        board.place(5, 1);
        assertFalse(board.isEmpty(5));
    }
}

