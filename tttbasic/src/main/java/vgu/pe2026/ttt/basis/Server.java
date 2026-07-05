package vgu.pe2026.ttt.basis;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

// Server class that initializes an HTTP Server to handle move requests from the Client
public class Server {

    // The port on which the server will listen for incoming connections
    private static final int PORT = 9090;
    // Initialize Gson object to be shared for JSON parsing
    private static final Gson GSON = new Gson();
    // Initialize a static ComputerPlayer instance to be shared across requests
    private static final ComputerPlayer COMPUTER = new ComputerPlayer();

    public static void main(String[] args) throws IOException {
        // Create an HTTP Server bound to port 9090
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Create an endpoint context "/move". 
        // Any request sent to this path will be handled by the MoveHandler class
        server.createContext("/move", new MoveHandler());
        
        // Set executor to null to run the Server in single-threaded mode
        server.setExecutor(null); 
        
        // Start the server
        server.start();
        System.out.println("HTTP Server started on port " + PORT);
    }

    // Inner class that handles the logic when an HTTP Request arrives
    static class MoveHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Verify HTTP method. If it's not POST, return a 405 Method Not Allowed error
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405,
                        GSON.toJson(new MoveResponse("error", null)));
                return;
            }

            // Read the entire Request Body and convert it into a UTF-8 String
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            
            // Deserialize the received JSON string into a MoveRequest object
            MoveRequest req = GSON.fromJson(body, MoveRequest.class);

            // Process the game logic based on the incoming request
            MoveResponse resp = handleMove(req);

            // Send back an HTTP 200 OK response with the JSON representation of MoveResponse
            sendResponse(exchange, 200, GSON.toJson(resp));
        }

        // Utility method to package and send the HTTP Response
        private void sendResponse(HttpExchange exchange, int code, String json) throws IOException {
            // Convert the JSON string into a byte array
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            
            // Set the HTTP header to indicate the response content is JSON
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            
            // Begin sending the response (status code and content length)
            exchange.sendResponseHeaders(code, bytes.length);
            
            // Write the JSON byte array into the output stream
            exchange.getResponseBody().write(bytes);
            
            // Close the connection to finalize the response
            exchange.close();
        }
    }

    // Core game logic processing (stateless - server does not remember board state)
    private static MoveResponse handleMove(MoveRequest req) {
        Board board;
        try {
            // Reconstruct the board object from the string sent by the client
            board = Board.fromLine(req.board);
        } catch (IllegalArgumentException e) {
            // If the board string format is invalid
            return new MoveResponse("invalid", "000000000");
        }

        // Check if the human player's requested move is valid
        if (!board.isValidMove(req.move)) {
            // Return 'invalid' status but retain the previous board state
            return new MoveResponse("invalid", board.toLine());
        }

        // Apply the human player's move (HUMAN = 1)
        board.place(req.move, Board.HUMAN);
        
        // Check if the Human player has won after this move
        if (board.hasWon(Board.HUMAN)) return new MoveResponse("win",  board.toLine());
        
        // Check if the board is completely filled (draw)
        if (board.isFull())            return new MoveResponse("draw", board.toLine());

        // It is now the Computer's turn
        int compMove = COMPUTER.chooseCell(board);
        if (compMove != -1) {
            // Apply the computer's move (COMPUTER = 2)
            board.place(compMove, Board.COMPUTER);
        }
        
        // Check if the Computer has won
        if (board.hasWon(Board.COMPUTER)) return new MoveResponse("lose", board.toLine());
        
        // Check if the board is completely filled (draw)
        if (board.isFull())               return new MoveResponse("draw", board.toLine());

        // If no one has won and it's not a draw, the game is ongoing
        return new MoveResponse("ongoing", board.toLine());
    }
}
