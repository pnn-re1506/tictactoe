package vgu.pe2026.ttt.basis;

import vgu.pe2026.ttt.basis.Board;
import vgu.pe2026.ttt.basis.Player;

import java.util.Scanner;

public class HumanPlayer extends Player {

    private Scanner scanner;

    public HumanPlayer(Scanner scanner) {
        super(1, "Human");
        this.scanner = scanner;
    }

    @Override
    public int chooseCell(Board board) {
        while (true) {
            System.out.print("Your move (1-9): ");
            String input = scanner.nextLine().trim();

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