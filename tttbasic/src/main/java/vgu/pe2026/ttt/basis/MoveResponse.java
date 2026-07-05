package vgu.pe2026.ttt.basis;

// Class representing the Response returned from Server to Client
public class MoveResponse {
    // The game status after processing the move
    // Possible values: "ongoing", "win", "lose", "draw", "invalid"
    String status;  
    
    // The latest board state as a 9-character string after both players (human and computer) have moved
    String board;   

    // Constructor to initialize a new response
    public MoveResponse(String status, String board) {
        this.status = status;
        this.board = board;
    }
}
