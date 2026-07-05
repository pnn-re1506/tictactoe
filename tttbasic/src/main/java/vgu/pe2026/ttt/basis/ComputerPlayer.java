package vgu.pe2026.ttt.basis;

public class ComputerPlayer {

    // Simple AI: chooses the first available empty cell on the board
    public int chooseCell(Board board) {
        return board.firstEmptyCell();
    }
}
