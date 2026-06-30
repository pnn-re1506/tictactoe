package vgu.pe2026.ttt.basis;

import java.util.Scanner;

public class HumanPlayer {

    private Scanner scanner;

    public HumanPlayer(Scanner scanner) {
        this.scanner = scanner;
    }

    public int chooseCell(Board board) {
        while (true) {
            System.out.println("Player#1 turn");
            System.out.print("Your move (1-9, q to quit): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("q")) {
                return -1;
            }

            int cell;
            try {
                cell = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a number from 1 to 9.");
                continue;
            }

            if (cell < 1 || cell > 9) {
                System.out.println("Out of range. Enter a number from 1 to 9.");
                continue;
            }

            if (!board.isEmpty(cell)) {
                System.out.println("Cell " + cell + " is already taken. Choose another.");
                continue;
            }

            return cell;
        }
    }
}
