package vgu.pe2026.ttt.basis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        Board board = new Board();

        try (Scanner scanner = new Scanner(System.in)) {
            HumanPlayer human = new HumanPlayer(scanner);
            waitForGameSlot();

            System.out.println("\n=== Tic-Tac-Toe ===");
            System.out.println("You are 1. Computer is 2.");

            while (true) {
                board.printMatrix();
                int humanMove = human.chooseCell(board);
                if (humanMove == -1) {
                    send("QUIT");
                    System.out.println("End of the game");
                    return;
                }

                String response = send("MOVE " + board.toLine() + " " + humanMove);
                String[] parts = response.split(" ");

                if (parts.length != 4 || !parts[0].equals("RESULT")) {
                    System.out.println("Invalid server response: " + response);
                    send("QUIT");
                    return;
                }

                String status = parts[1];
                if (status.equals("invalid")) {
                    System.out.println("Invalid move. Please try again.");
                    continue;
                }

                board = Board.fromLine(parts[2]);
                int computerMove = Integer.parseInt(parts[3]);
                if (computerMove > 0) {
                    System.out.println("Computer chose cell " + computerMove + ".");
                }

                if (status.equals("ongoing")) {
                    continue;
                }

                board.printMatrix();
                printResult(status);
                return;
            }
        } catch (IOException e) {
            System.out.println("Cannot connect to server: " + e.getMessage());
        }
    }

    private static void waitForGameSlot() throws IOException {
        while (true) {
            String startResponse = send("START");
            if (startResponse.equals("OK")) {
                return;
            }
            if (!startResponse.equals("WAIT")) {
                throw new IOException("Cannot start game: " + startResponse);
            }

            System.out.println("Server is busy. Waiting...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for server.", e);
            }
        }
    }

    private static String send(String request) throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

            output.println(request);
            return input.readLine();
        }
    }

    private static void printResult(String status) {
        if (status.equals("win")) {
            System.out.println("You win!");
        } else if (status.equals("lose")) {
            System.out.println("Computer wins!");
        } else if (status.equals("draw")) {
            System.out.println("It's a draw!");
        } else {
            System.out.println("Game ended with status: " + status);
        }
    }
}
