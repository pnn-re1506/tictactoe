package vgu.pe2026.ttt.basis;

import vgu.pe2026.ttt.basis.Board;
import vgu.pe2026.ttt.basis.ComputerPlayer;
import vgu.pe2026.ttt.basis.GamePlay;
import vgu.pe2026.ttt.basis.HumanPlayer;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        if (args.length != 1 || (!args[0].equals("1") && !args[0].equals("2"))) {
            System.out.println("Please, input a valid option [1-2]");
            System.out.println("  1 = Human starts,  2 = Computer starts");
            System.exit(1);
        }

        int startingPlayer = Integer.parseInt(args[0]);
        Scanner scanner = new Scanner(System.in);

        HumanPlayer human = new HumanPlayer(scanner);
        ComputerPlayer computer = new ComputerPlayer();
        Board board = new Board();

        GamePlay engine;
        if (startingPlayer == 1) {
            System.out.println("Human starts.");
            engine = new GamePlay(board, human, computer);
        } else {
            System.out.println("Computer starts.");
            engine = new GamePlay(board, computer, human);
        }

        engine.play();
        scanner.close();
    }
}