package vgu.pe2026.ttt.basis;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class Client {

    private static final String SERVER_URL = "http://localhost:9090/move";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void main(String[] args) {
        Board board = new Board();

        try (Scanner scanner = new Scanner(System.in)) {
            HumanPlayer human = new HumanPlayer(scanner);

            System.out.println("\n=== Tic-Tac-Toe ===");
            System.out.println("You are 1. Computer is 2.");

            while (true) {
                board.printMatrix();
                int humanMove = human.chooseCell(board);
                if (humanMove == -1) {
                    System.out.println("End of the game");
                    return;
                }

                MoveResponse resp = send(board.toLine(), humanMove);

                if (resp.status.equals("invalid")) {
                    System.out.println("Invalid move. Please try again.");
                    continue;
                }

                board = Board.fromLine(resp.board);

                if (resp.status.equals("ongoing")) {
                    continue;
                }

                board.printMatrix();
                printResult(resp.status);
                return;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Cannot connect to server: " + e.getMessage());
        }
    }

    private static MoveResponse send(String boardLine, int move)
            throws IOException, InterruptedException {

        String requestBody = GSON.toJson(new MoveRequest(boardLine, move));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Server error: HTTP " + response.statusCode());
        }

        return GSON.fromJson(response.body(), MoveResponse.class);
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
