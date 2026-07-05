package vgu.pe2026.ttt.basis;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

// Client class running on the user's side, communicating with the Server via HTTP
public class Client {

    // The Server API endpoint URL
    private static final String SERVER_URL = "http://localhost:9090/move";
    
    // Initialize Gson object to be shared for JSON Parsing/Serialization
    private static final Gson GSON = new Gson();
    
    // Initialize a single HttpClient instance to send/receive HTTP requests
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void main(String[] args) {
        // Create an empty Tic-Tac-Toe board
        Board board = new Board();

        // Use a try-with-resources block to automatically close the Scanner when done
        try (Scanner scanner = new Scanner(System.in)) {
            // Initialize the handler for human player input
            HumanPlayer human = new HumanPlayer(scanner);

            System.out.println("\n=== Tic-Tac-Toe ===");
            System.out.println("You are 1. Computer is 2.");

            // The main game loop
            while (true) {
                // Print the current board layout to the console
                board.printMatrix();
                
                // Request the human player to input their move
                int humanMove = human.chooseCell(board);
                
                // If the player inputs 'q' (returns -1)
                if (humanMove == -1) {
                    System.out.println("End of the game");
                    return; // Terminate the program
                }

                // Send the current board state and the human's move to the Server for processing
                MoveResponse resp = send(board.toLine(), humanMove);

                // If the Server indicates the move was invalid
                if (resp.status.equals("invalid")) {
                    System.out.println("Invalid move. Please try again.");
                    continue; // Skip the rest of the loop and ask for input again
                }

                // Update the local board with the new string state returned by the Server
                board = Board.fromLine(resp.board);

                // If the game status is "ongoing"
                if (resp.status.equals("ongoing")) {
                    continue; // Continue the loop, it's the human's turn again
                }

                // If the status is win, lose, or draw, print the final board state
                board.printMatrix();
                // Notify the user of the final result and terminate the program
                printResult(resp.status);
                return;
            }
        } catch (IOException | InterruptedException e) {
            // Catch network-related errors (e.g., cannot connect to the Server)
            System.out.println("Cannot connect to server: " + e.getMessage());
        }
    }

    // Method to send data and receive results via HTTP POST
    private static MoveResponse send(String boardLine, int move)
            throws IOException, InterruptedException {

        // Package the board state and the move into a JSON string
        String requestBody = GSON.toJson(new MoveRequest(boardLine, move));

        // Build an HTTP POST Request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL)) // Target the Server URL
                .header("Content-Type", "application/json") // Set content type to JSON
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)) // Attach the JSON string to the Body
                .build();

        // Send the Request and wait for the Response (expected as a String)
        HttpResponse<String> response = HTTP_CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString());

        // Check if the HTTP Status Code is 200 (OK/Success)
        if (response.statusCode() != 200) {
            throw new IOException("Server error: HTTP " + response.statusCode());
        }

        // Deserialize the Response Body (JSON string) back into a MoveResponse object
        return GSON.fromJson(response.body(), MoveResponse.class);
    }

    // Helper method to print the outcome based on the status string
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
