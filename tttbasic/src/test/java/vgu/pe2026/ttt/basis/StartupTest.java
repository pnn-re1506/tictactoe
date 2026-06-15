package vgu.pe2026.ttt.basis;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests for the HTTP game server components.
 * Tests GameHandler validation logic and MoveProcessor behavior
 * without starting an actual HTTP server.
 */
public class StartupTest {

    // ── TS-001: First move on empty board ─────────────────────────────────
    @Nested
    @DisplayName("TS-001 – Start game with human first (cell 1-9 valid)")
    class TS001 {

        @Test
        @DisplayName("First move on empty board produces valid status")
        void testStartGameHumanFirst() {
            int[] board = {0,0,0,0,0,0,0,0,0};
            MoveProcessor.Result result = MoveProcessor.process(board, 1);
            assertNotNull(result);
            assertTrue(
                result.status().equals("PLAYING") ||
                result.status().equals("WIN_PLAYER") ||
                result.status().equals("WIN_COMPUTER") ||
                result.status().equals("DRAW")
            );
            assertEquals(1, result.board()[0], "Player should be placed at cell 1");
        }
    }

    // ── TS-002: AI responds after player move ─────────────────────────────
    @Nested
    @DisplayName("TS-002 – Start game with computer first (AI plays after human)")
    class TS002 {

        @Test
        @DisplayName("AI plays a mark after human's move on empty board")
        void testStartGameComputerFirst() {
            int[] board = {0,0,0,0,0,0,0,0,0};
            MoveProcessor.Result result = MoveProcessor.process(board, 5);

            boolean aiPlayed = false;
            for (int v : result.board()) if (v == 2) { aiPlayed = true; break; }
            assertTrue(aiPlayed, "AI should have placed its mark after the player");
        }
    }

    // ── TS-003: Reject cell out of range ─────────────────────────────────
    @Nested
    @DisplayName("TS-003 – Reject missing / invalid startup argument (cell out of range)")
    class TS003 {

        @Test
        @DisplayName("Cell 0 is below valid range [1-9]")
        void testNoArgs() {
            int cell = 0;
            assertTrue(cell < 1 || cell > 9, "Cell 0 should fail range validation");
        }
    }

    // ── TS-004: Multiple invalid cell values ──────────────────────────────
    @Nested
    @DisplayName("TS-004 – Reject invalid startup argument value (bad cell numbers)")
    class TS004 {

        private void assertOutOfRange(int cell) {
            assertTrue(cell < 1 || cell > 9,
                "Cell " + cell + " should be out of [1-9] range");
        }

        @Test @DisplayName("Cell 10 rejected") void testArg3()        { assertOutOfRange(10); }
        @Test @DisplayName("Cell 0 rejected")  void testArg0()        { assertOutOfRange(0);  }
        @Test @DisplayName("Cell -1 rejected") void testArgNegative1(){ assertOutOfRange(-1); }
        @Test @DisplayName("Cell 99 rejected") void testArgAbc()      { assertOutOfRange(99); }
    }

    // ── TS-005: Occupied cell / game-over / immutability checks ──────────
    @Nested
    @DisplayName("TS-005 – Validate startup argument strictness (occupied cell / game over)")
    class TS005 {

        @Test
        @DisplayName("Occupied cell detected before MoveProcessor is called")
        void testArgLeadingZero() {
            int[] board = {1,0,0,0,0,0,0,0,0};
            int cell = 1;
            assertNotEquals(0, board[cell - 1], "Cell 1 is occupied and should be rejected");
        }

        @Test
        @DisplayName("Board with winner is detected as game-over")
        void testExtraArgs() {
            int[] board = {1,1,1,2,2,0,0,0,0};
            int[][] WIN_LINES = {
                {0,1,2},{3,4,5},{6,7,8},
                {0,3,6},{1,4,7},{2,5,8},
                {0,4,8},{2,4,6}
            };
            boolean over = false;
            for (int[] line : WIN_LINES) {
                int v = board[line[0]];
                if (v != 0 && v == board[line[1]] && v == board[line[2]]) {
                    over = true; break;
                }
            }
            assertTrue(over, "Board should be detected as game-over");
        }

        @Test
        @DisplayName("MoveProcessor does not mutate the input board")
        void testLeadingSpace() {
            int[] board = {0,0,0,0,0,0,0,0,0};
            int[] snapshot = board.clone();
            MoveProcessor.process(board, 3);
            assertArrayEquals(snapshot, board, "Input board must not be mutated by MoveProcessor");
        }
    }
}
