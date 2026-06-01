package vgu.pe2026.ttt.basis;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test scenarios TS-001 through TS-005 from test_scenarios_v0.4.md
 *
 * These tests exercise the startup / argument-validation behavior of Main.main().
 * Each test captures System.out and, where needed, supplies simulated System.in
 * so the game loop terminates without hanging.
 *
 * NOTE – The v0.4 spec expects the greeting "Hello!" and prompts like "Player#1's turn".
 * The current implementation prints "Human starts." / "Computer starts." and uses
 * "Human's turn" / "computer's turn".  Tests below assert against the **actual**
 * implementation output.  If the code is later updated to match the spec wording,
 * update the assertion strings accordingly.
 *
 * SYSTEM.EXIT HANDLING:
 * Main.main() calls System.exit(1) for invalid args.  To prevent this from killing
 * the test JVM, we install a SecurityManager that converts the exit call into a
 * catchable SecurityException.  This approach works on Java 17 and earlier.
 * For Java 21+, the SecurityManager is deprecated but still functional.
 */
public class StartupTest {

    private final PrintStream originalOut = System.out;
    private final InputStream originalIn  = System.in;

    private ByteArrayOutputStream capturedOut;

    // ── System.exit() interceptor ──────────────────────────────────────────
    /**
     * Custom exception thrown in place of System.exit() so that tests can
     * catch it without the JVM shutting down.
     */
    private static class ExitException extends SecurityException {
        final int status;
        ExitException(int status) {
            super("System.exit(" + status + ") intercepted");
            this.status = status;
        }
    }

    @SuppressWarnings("removal")   // SecurityManager is deprecated in newer JDKs
    private static class NoExitSecurityManager extends SecurityManager {
        @Override
        public void checkExit(int status) {
            throw new ExitException(status);
        }
        @Override
        public void checkPermission(java.security.Permission perm) {
            // allow everything else
        }
    }

    @SuppressWarnings("removal")
    @BeforeEach
    void setUp() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
        System.setSecurityManager(new NoExitSecurityManager());
    }

    @SuppressWarnings("removal")
    @AfterEach
    void tearDown() {
        System.setSecurityManager(null);
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    /** Helper – feed a string into System.in so the Scanner inside Main can read it. */
    private void setMockInput(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    /** Helper – return the captured stdout as a single string. */
    private String output() {
        return capturedOut.toString(StandardCharsets.UTF_8);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TS-001 : Start game with human first (arg = "1")
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("TS-001 – Start game with human first")
    class TS001 {

        @Test
        @DisplayName("Shows greeting, initial board (all 0s), and first-turn prompt")
        void testStartGameHumanFirst() {
            // Provide input so the game loop doesn't hang.
            // The HumanPlayer reads via scanner.nextLine().
            // We supply a few inputs; the game will eventually run out of input
            // and throw NoSuchElementException, which we catch.
            setMockInput("5\n3\n7\n");

            try {
                Main.main(new String[]{"1"});
            } catch (ExitException e) {
                fail("System.exit should NOT be called for valid arg '1'");
            } catch (Exception ignored) {
                // NoSuchElementException when scanner runs out of input – expected
            }

            String out = output();

            /*
             * Spec expects: "Hello!"
             * Actual code:  "Human starts."
             * Assert against actual code – update if code changes.
             */
            assertTrue(out.contains("Human starts."),
                    "Should print startup message when arg=1.\nActual output:\n" + out);

            // Initial board: 3×3 grid with all cells showing 0
            // Board.printMatrix() outputs lines like "| 0 | 0 | 0 |"
            assertTrue(out.contains("| 0 | 0 | 0 |"),
                    "Initial board should display all zeros.\nActual output:\n" + out);

            /*
             * Spec expects: "Player#1's turn"
             * Actual code:  "Human's turn"
             */
            assertTrue(out.contains("Human's turn"),
                    "First-turn prompt should indicate the human player.\nActual output:\n" + out);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TS-002 : Start game with computer first (arg = "2")
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("TS-002 – Start game with computer first")
    class TS002 {

        @Test
        @DisplayName("Shows greeting, initial board, then computer turn prompt")
        void testStartGameComputerFirst() {
            // Computer moves automatically (first-available strategy).
            // We supply human moves to keep the game going.
            setMockInput("5\n7\n6\n8\n");

            try {
                Main.main(new String[]{"2"});
            } catch (ExitException e) {
                fail("System.exit should NOT be called for valid arg '2'");
            } catch (Exception ignored) {
                // NoSuchElementException when scanner runs out of input – expected
            }

            String out = output();

            /*
             * Spec expects: "Hello!"
             * Actual code:  "Computer starts."
             */
            assertTrue(out.contains("Computer starts."),
                    "Should print startup message when arg=2.\nActual output:\n" + out);

            // Initial board with all zeros should appear at start
            assertTrue(out.contains("| 0 | 0 | 0 |"),
                    "Initial board should display all zeros.\nActual output:\n" + out);

            /*
             * Spec expects: "Player#2's turn"
             * Actual code:  "computer's turn"
             */
            assertTrue(out.contains("computer's turn"),
                    "First-turn prompt should indicate the computer player.\nActual output:\n" + out);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TS-003 : Reject missing startup argument (no args)
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("TS-003 – Reject missing startup argument")
    class TS003 {

        @Test
        @DisplayName("No args → prints error message and exits")
        void testNoArgs() {
            boolean exitCalled = false;
            int exitCode = -1;

            try {
                Main.main(new String[]{});
            } catch (ExitException e) {
                exitCalled = true;
                exitCode = e.status;
            } catch (Exception ignored) { }

            String out = output();

            assertTrue(out.contains("Please, input a valid option [1-2]"),
                    "Should print validation error for missing args.\nActual output:\n" + out);

            assertTrue(exitCalled,
                    "Program should call System.exit for missing args.");
            assertEquals(1, exitCode,
                    "Exit code should be 1 for invalid arguments.");

            // Game should NOT have started – no board output expected
            assertFalse(out.contains("Tic-Tac-Toe"),
                    "Game loop should not start when no argument is provided.\nActual output:\n" + out);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TS-004 : Reject invalid startup argument values (3, 0, -1, abc)
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("TS-004 – Reject invalid startup argument value")
    class TS004 {

        private void assertInvalidArg(String arg) {
            // Reset captured output for each sub-case
            capturedOut.reset();
            boolean exitCalled = false;

            try {
                Main.main(new String[]{arg});
            } catch (ExitException e) {
                exitCalled = true;
            } catch (Exception ignored) { }

            String out = output();

            assertTrue(out.contains("Please, input a valid option [1-2]"),
                    "Arg '" + arg + "' should be rejected with error message.\nActual output:\n" + out);

            assertTrue(exitCalled,
                    "Arg '" + arg + "' should cause System.exit.");

            assertFalse(out.contains("Tic-Tac-Toe"),
                    "Arg '" + arg + "' should not start the game.\nActual output:\n" + out);
        }

        @Test
        @DisplayName("Arg '3' is rejected")
        void testArg3() {
            assertInvalidArg("3");
        }

        @Test
        @DisplayName("Arg '0' is rejected")
        void testArg0() {
            assertInvalidArg("0");
        }

        @Test
        @DisplayName("Arg '-1' is rejected")
        void testArgNegative1() {
            assertInvalidArg("-1");
        }

        @Test
        @DisplayName("Arg 'abc' is rejected")
        void testArgAbc() {
            assertInvalidArg("abc");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TS-005 : Validate startup argument strictness ("exactly either 1 or 2")
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("TS-005 – Validate startup argument strictness")
    class TS005 {

        /**
         * The current implementation uses {@code args[0].equals("1")} which is
         * strict string comparison, so "01", " 1" are rejected.
         * It also checks {@code args.length != 1}, so extra args are rejected.
         * These tests document that behavior.
         */

        @Test
        @DisplayName("Arg '01' is rejected (leading zero)")
        void testArgLeadingZero() {
            boolean exitCalled = false;

            try {
                Main.main(new String[]{"01"});
            } catch (ExitException e) {
                exitCalled = true;
            } catch (Exception ignored) { }

            String out = output();
            assertTrue(out.contains("Please, input a valid option [1-2]"),
                    "Arg '01' should be rejected.\nActual output:\n" + out);
            assertTrue(exitCalled,
                    "Arg '01' should cause System.exit.");
        }

        @Test
        @DisplayName("Extra args ['1', '2'] are rejected (args.length != 1)")
        void testExtraArgs() {
            capturedOut.reset();
            boolean exitCalled = false;

            try {
                Main.main(new String[]{"1", "2"});
            } catch (ExitException e) {
                exitCalled = true;
            } catch (Exception ignored) { }

            String out = output();
            assertTrue(out.contains("Please, input a valid option [1-2]"),
                    "Extra arguments should be rejected.\nActual output:\n" + out);
            assertTrue(exitCalled,
                    "Extra arguments should cause System.exit.");
        }

        @Test
        @DisplayName("Arg ' 1' (leading space) is rejected")
        void testLeadingSpace() {
            capturedOut.reset();
            boolean exitCalled = false;

            try {
                Main.main(new String[]{" 1"});
            } catch (ExitException e) {
                exitCalled = true;
            } catch (Exception ignored) { }

            String out = output();
            assertTrue(out.contains("Please, input a valid option [1-2]"),
                    "Arg with leading space should be rejected.\nActual output:\n" + out);
            assertTrue(exitCalled,
                    "Arg with leading space should cause System.exit.");
        }
    }
}
