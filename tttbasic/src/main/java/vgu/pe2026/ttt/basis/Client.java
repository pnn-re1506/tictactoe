package vgu.pe2026.ttt.basis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    // Server configuration
    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        Board board = new Board();

        try (Scanner scanner = new Scanner(System.in)) {
            HumanPlayer human = new HumanPlayer(scanner);

            System.out.println("\n=== Tic-Tac-Toe ===");
            System.out.println("You are 1. Computer is 2.");

            // Main game loop - represents turns
            while (true) {
                board.printMatrix();
                
                // 1. Get input from human player
                int humanMove = human.chooseCell(board);
                // If human player types 'q' to quit, chooseCell returns -1
                if (humanMove == -1) {
                    System.out.println("End of the game");
                    return;
                }

                // 2. Send move to server over a stateless connection
                String response = send("MOVE " + board.toLine() + " " + humanMove);
                
                // 3. Parse server response ("RESULT <status> <new_board>")
                String[] parts = response.split(" ");

                if (parts.length != 3 || !parts[0].equals("RESULT")) {
                    System.out.println("Invalid server response: " + response);
                    return; // Exit game on bad server response
                }

                String status = parts[1];
                if (status.equals("invalid")) {
                    System.out.println("Invalid move. Please try again.");
                    continue; // Skip the rest of the loop and prompt human again
                }

                // 4. Update local board with the state provided by the server
                board = Board.fromLine(parts[2]);

                if (status.equals("ongoing")) {
                    continue; // Game is not over, loop back to human's turn
                }

                // Game over
                board.printMatrix();
                printResult(status);
                return;
            }
        } catch (IOException e) {
            System.out.println("Cannot connect to server: " + e.getMessage());
        }
    }

    /**
     * Helper method to send a request to the server and receive its response.
     * Implements a stateless connection: Opens socket, sends, receives, then closes immediately.
     */
    private static String send(String request) throws IOException {
        // The try-with-resources syntax automatically closes socket, input, and output
        // when the block exits, preventing resource leaks.
        try (Socket socket = new Socket(HOST, PORT);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) { // true = auto-flush

            output.println(request); // Send request string
            return input.readLine(); // Wait for and return the response line
        }
    }

    private static void printResult(String status) {
        if (status.equals("win")) {
            System.out.println("Player#1 won!");
        } else if (status.equals("lose")) {
            System.out.println("Player#2 won!");
        } else if (status.equals("draw")) {
            System.out.println("It's a draw!");
        } else {
            System.out.println("Game ended with status: " + status);
        }
    }
}
