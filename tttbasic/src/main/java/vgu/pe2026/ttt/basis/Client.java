package vgu.pe2026.ttt.basis;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using default: " + port);
            }
        }

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server at " + host + ":" + port);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("BOARD|")) {
                    displayBoard(line.substring(6));
                } else if (line.startsWith("YOUR_TURN")) {
                    handleHumanTurn(scanner, out);
                } else if (line.startsWith("WIN|")) {
                    String winner = line.substring(4);
                    if (winner.equals("1")) {
                        System.out.println("Player#1 won!");
                    } else {
                        System.out.println("Player#2 won!");
                    }
                    break;
                } else if (line.startsWith("DRAW")) {
                    System.out.println("It is a draw!");
                    break;
                } else if (line.startsWith("MSG|")) {
                    System.out.println(line.substring(4));
                }
            }

        } catch (ConnectException e) {
            System.out.println("Cannot connect to server at " + host + ":" + port);
            System.out.println("Make sure the server is running first.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleHumanTurn(Scanner scanner, PrintWriter out) {
        while (true) {
            System.out.print("Player#1's turn: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("q")) {
                out.println("QUIT");
                return;
            }

            int cell;
            try {
                cell = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please, input a valid number [1-9]");
                continue;
            }

            if (cell < 1 || cell > 9) {
                System.out.println("Please, input a valid number [1-9]");
                continue;
            }

            out.println("MOVE|" + cell);
            return;
        }
    }

    private static void displayBoard(String state) {
        String[] cells = state.split(",");
        System.out.println();
        for (int row = 0; row < 3; row++) {
            System.out.print("| ");
            for (int col = 0; col < 3; col++) {
                System.out.print(cells[row * 3 + col]);
                System.out.print(" | ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
