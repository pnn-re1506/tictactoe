package vgu.pe2026.ttt.basis;

public class ComputerPlayer extends Player {
    public ComputerPlayer(){
        super(2, "computer");
    }
    @Override
    public int chooseCell(Board board){
        int cell = board.firstEmptyCell();
        return cell;
    }
}