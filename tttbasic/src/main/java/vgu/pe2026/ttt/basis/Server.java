package vgu.pe2026.ttt.basis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 12345;

    public static void main(String[] args) {
        ComputerPlayer computer = new ComputerPlayer();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                try (Socket socket = serverSocket.accept();
                        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

                    String request = input.readLine();
                    if (request == null || request.isBlank()) {
                        output.println("ERROR Empty request");
                        continue;
                    }



                    String response = handleMove(request, computer);
                    output.println(response);


                } catch (IOException e) {
                    System.out.println("Connection error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Cannot start server: " + e.getMessage());
        }
    }

    private static String handleMove(String request, ComputerPlayer computer) {
        String[] parts = request.split(" ");
        if (parts.length != 3 || !parts[0].equals("MOVE")) {
            return "RESULT invalid 000000000";
        }

        Board board;
        int humanMove;
        try {
            board = Board.fromLine(parts[1]);
            humanMove = Integer.parseInt(parts[2]);
        } catch (IllegalArgumentException e) {
            return "RESULT invalid 000000000";
        }

        if (!board.isValidMove(humanMove)) {
            return "RESULT invalid " + board.toLine();
        }

        board.place(humanMove, Board.HUMAN);
        if (board.hasWon(Board.HUMAN)) {
            return "RESULT win " + board.toLine();
        }
        if (board.isFull()) {
            return "RESULT draw " + board.toLine();
        }

        int computerMove = computer.chooseCell(board);
        if (computerMove != -1) {
            board.place(computerMove, Board.COMPUTER);
        }

        if (board.hasWon(Board.COMPUTER)) {
            return "RESULT lose " + board.toLine();
        }
        if (board.isFull()) {
            return "RESULT draw " + board.toLine();
        }

        return "RESULT ongoing " + board.toLine();
    }

}
