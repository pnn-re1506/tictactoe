package vgu.pe2026.ttt.basis;

import java.util.Scanner;

public class HumanPlayer {

    private Scanner scanner;

    public HumanPlayer(Scanner scanner) {
        this.scanner = scanner;
    }

    public int chooseCell(Board board) {
        // Loop until a valid move is entered
        while (true) {
            System.out.println("Player#1 turn");
            System.out.print("Your move (1-9, q to quit): ");
            String input = scanner.nextLine().trim();

            // Check if user wants to quit
            if (input.equalsIgnoreCase("q")) {
                return -1; // -1 is used as a signal to terminate the game
            }

            int cell;
            // try-catch block prevents game from crashing if user inputs non-numeric characters
            try {
                cell = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a number from 1 to 9.");
                continue; // Skip the rest of the loop and prompt again
            }

            // Validate range
            if (cell < 1 || cell > 9) {
                System.out.println("Out of range. Enter a number from 1 to 9.");
                continue;
            }

            // Validate cell availability
            if (!board.isEmpty(cell)) {
                System.out.println("Cell " + cell + " is already taken. Choose another.");
                continue;
            }

            return cell;
        }
    }
}
