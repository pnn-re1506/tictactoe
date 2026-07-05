package vgu.pe2026.ttt.basis;

// Class representing the Move Request sent from Client to Server
public class MoveRequest {
    // Current board state represented as a 9-character string (e.g., "102010000")
    String board;   
    // The cell position the user wants to play (from 1 to 9)
    int move;       

    // Constructor to initialize a new request
    public MoveRequest(String board, int move) {
        this.board = board;
        this.move = move;
    }
}
