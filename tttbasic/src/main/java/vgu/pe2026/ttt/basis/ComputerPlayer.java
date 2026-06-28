package vgu.pe2026.ttt.basis;

public class ComputerPlayer {

    public int chooseCell(Board board) {
        return board.firstEmptyCell();
    }
}
