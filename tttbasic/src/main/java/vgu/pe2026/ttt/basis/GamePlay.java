package vgu.pe2026.ttt.basis;

public class GamePlay {

    private Board board;
    private Player[] players;
    private int current = 0;

    public GamePlay(Board board, Player first, Player second) {
        this.board = board;
        this.players = new Player[]{first, second};
    }

    public void play() {
        System.out.println("\n=== Tic-Tac-Toe ===");
        board.printMatrix();

        while (true) {
            Player p = players[current];
            System.out.println(p.getName() + "'s turn");

            int cell = p.chooseCell(board);
            board.place(cell, p.getMark());
            board.printMatrix();

            if (board.hasWon(p.getMark())) {
                System.out.println(p.getName() + " wins!");
                return;
            }

            if (board.isFull()) {
                System.out.println("It's a draw!");
                return;
            }

            current = 1 - current;
        }
    }
}