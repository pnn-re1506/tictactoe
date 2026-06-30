package vgu.pe2026.ttt.basis;

public class MoveRequest {
    String board;   // 9-char string, e.g. "000000000"
    int move;       // cell 1–9

    public MoveRequest(String board, int move) {
        this.board = board;
        this.move = move;
    }
}
