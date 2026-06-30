package vgu.pe2026.ttt.basis;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Server {

    private static final int PORT = 9090;
    private static final Gson GSON = new Gson();
    private static final ComputerPlayer COMPUTER = new ComputerPlayer();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/move", new MoveHandler());
        server.setExecutor(null); // single-threaded
        server.start();
        System.out.println("HTTP Server started on port " + PORT);
    }

    static class MoveHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405,
                        GSON.toJson(new MoveResponse("error", null)));
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            MoveRequest req = GSON.fromJson(body, MoveRequest.class);

            MoveResponse resp = handleMove(req);

            sendResponse(exchange, 200, GSON.toJson(resp));
        }

        private void sendResponse(HttpExchange exchange, int code, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(code, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }

    private static MoveResponse handleMove(MoveRequest req) {
        Board board;
        try {
            board = Board.fromLine(req.board);
        } catch (IllegalArgumentException e) {
            return new MoveResponse("invalid", "000000000");
        }

        if (!board.isValidMove(req.move)) {
            return new MoveResponse("invalid", board.toLine());
        }

        board.place(req.move, Board.HUMAN);
        if (board.hasWon(Board.HUMAN)) return new MoveResponse("win",  board.toLine());
        if (board.isFull())            return new MoveResponse("draw", board.toLine());

        int compMove = COMPUTER.chooseCell(board);
        if (compMove != -1) board.place(compMove, Board.COMPUTER);
        if (board.hasWon(Board.COMPUTER)) return new MoveResponse("lose", board.toLine());
        if (board.isFull())               return new MoveResponse("draw", board.toLine());

        return new MoveResponse("ongoing", board.toLine());
    }
}
