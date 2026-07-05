package vgu.pe2026.ttt.basis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class Server {

    private static final int PORT = 12345;
    private static final String SECRET_KEY = "Vgu_Tictactoe_Secret";

    private static String generateHMAC(String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }

    public static void main(String[] args) {
        ComputerPlayer computer = new ComputerPlayer();

        // Outer try-with-resources: manages the ServerSocket lifecycle
        // Automatically closes the port when the server shuts down
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            // Infinite loop to keep the server running and accepting new connections
            while (true) {
                // Inner try-with-resources: manages a single client connection lifecycle
                // accept() blocks until a client connects. 
                // socket, input, and output are automatically closed at the end of this block,
                // enforcing the stateless architecture.
                try (Socket socket = serverSocket.accept();
                        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

                    // Read request from client
                    String request = input.readLine();
                    if (request == null || request.isBlank()) {
                        output.println("ERROR Empty request");
                        continue;
                    }

                    // Process the move and get response string
                    String response = handleMove(request, computer);
                    
                    // Send response back to client
                    output.println(response);
                    // The socket is automatically closed when exiting this block
                    // output.close()
                    // input.close()
                    // socket.close()

                } catch (IOException e) {
                    System.out.println("Connection error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Cannot start server: " + e.getMessage());
        }
    }

    // Core game logic processor
    private static String handleMove(String request, ComputerPlayer computer) {
        if (request.equals("START")) {
            String initialBoard = "000000000";
            return "RESULT start " + initialBoard + " " + generateHMAC(initialBoard);
        }

        // Parse request: e.g., "MOVE 100000000 5 signature"
        String[] parts = request.split(" ");
        if (parts.length != 4 || !parts[0].equals("MOVE")) {
            return "RESULT invalid 000000000 " + generateHMAC("000000000");
        }

        String boardLine = parts[1];
        int humanMove;
        String clientSignature = parts[3];

        // Validate HMAC signature
        if (!generateHMAC(boardLine).equals(clientSignature)) {
            return "RESULT error 000000000 Invalid_Signature";
        }

        Board board;
        try {
            board = Board.fromLine(boardLine); // Reconstruct board state
            humanMove = Integer.parseInt(parts[2]); // Parse requested move
        } catch (IllegalArgumentException e) {
            return "RESULT invalid " + boardLine + " " + generateHMAC(boardLine);
        }

        // Validate human move against the reconstructed board
        if (!board.isValidMove(humanMove)) {
            return "RESULT invalid " + board.toLine() + " " + generateHMAC(board.toLine());
        }

        // Apply human move
        board.place(humanMove, Board.HUMAN);
        if (board.hasWon(Board.HUMAN)) {
            return "RESULT win " + board.toLine() + " " + generateHMAC(board.toLine());
        }
        if (board.isFull()) {
            return "RESULT draw " + board.toLine() + " " + generateHMAC(board.toLine());
        }

        // AI turn
        int computerMove = computer.chooseCell(board);
        if (computerMove != -1) {
            board.place(computerMove, Board.COMPUTER);
        }

        // Check for AI win or draw
        if (board.hasWon(Board.COMPUTER)) {
            return "RESULT lose " + board.toLine() + " " + generateHMAC(board.toLine());
        }
        if (board.isFull()) {
            return "RESULT draw " + board.toLine() + " " + generateHMAC(board.toLine());
        }

        // Game continues
        return "RESULT ongoing " + board.toLine() + " " + generateHMAC(board.toLine());
    }

}
