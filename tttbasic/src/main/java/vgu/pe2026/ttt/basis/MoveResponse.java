package vgu.pe2026.ttt.basis;

public class MoveResponse {
    String status;  // "ongoing" | "win" | "lose" | "draw" | "invalid"
    String board;   // 9-char string after both moves applied

    public MoveResponse(String status, String board) {
        this.status = status;
        this.board = board;
    }
}
