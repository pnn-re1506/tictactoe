package vgu.pe2026.ttt.basis;

import java.io.*;
import java.net.*;

public class Server {

    public static void main(String[] args) {
        int port = 12345;
        int startingPlayer = 1;

        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using default: " + port);
            }
        }
        if (args.length >= 2) {
            if (args[1].equals("1") || args[1].equals("2")) {
                startingPlayer = Integer.parseInt(args[1]);
            } else {
                System.out.println("Please, input a valid option [1-2]");
                System.exit(1);
            }
        }

        System.out.println("Server starting on port " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Waiting for client connection...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            Board board = new Board();
            ComputerPlayer computer = new ComputerPlayer();

            boolean humanTurn = (startingPlayer == 1);

            out.println("MSG|Hello!");
            out.println("BOARD|" + boardState(board));

            while (true) {
                if (humanTurn) {
                    out.println("YOUR_TURN");

                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }

                    if (line.startsWith("QUIT")) {
                        out.println("MSG|End of the game");
                        break;
                    }

                    if (line.startsWith("MOVE|")) {
                        int cell;
                        try {
                            cell = Integer.parseInt(line.substring(5));
                        } catch (NumberFormatException e) {
                            out.println("MSG|Please, input a valid number [1-9]");
                            continue;
                        }

                        if (cell < 1 || cell > 9) {
                            out.println("MSG|Please, input a valid number [1-9]");
                            continue;
                        }

                        if (!board.isEmpty(cell)) {
                            out.println("MSG|The cell is occupied!");
                            continue;
                        }

                        board.place(cell, 1);
                        out.println("BOARD|" + boardState(board));

                        if (board.hasWon(1)) {
                            out.println("WIN|1");
                            break;
                        }
                        if (board.isFull()) {
                            out.println("DRAW");
                            break;
                        }
                        humanTurn = false;
                    }
                } else {
                    int cell = computer.chooseCell(board);
                    board.place(cell, 2);
                    out.println("BOARD|" + boardState(board));

                    if (board.hasWon(2)) {
                        out.println("WIN|2");
                        break;
                    }
                    if (board.isFull()) {
                        out.println("DRAW");
                        break;
                    }
                    humanTurn = true;
                }
            }

            clientSocket.close();
            System.out.println("Game ended. Server shutting down.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String boardState(Board board) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 9; i++) {
            if (i > 1) sb.append(",");
            sb.append(board.getCell(i));
        }
        return sb.toString();
    }
}
