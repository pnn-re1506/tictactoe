package vgu.pe2026.ttt.basis;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

// HTTP handler for POST /api/move — stateless, no HMAC
public class GameHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();

        // handle CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(method)) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        int[] board;
        try {
            board = parseBoard(body);
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid board format");
            return;
        }

        int cell;
        try {
            cell = parseCell(body);
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid cell format");
            return;
        }

        // validate cell range
        if (cell < 1 || cell > 9) {
            sendError(exchange, 400, "Cell must be between 1 and 9");
            return;
        }

        // validate cell not occupied
        if (board[cell - 1] != 0) {
            sendError(exchange, 400, "Cell is already occupied");
            return;
        }

        // validate game not already over
        if (isGameOver(board)) {
            sendError(exchange, 400, "Game already over");
            return;
        }

        MoveProcessor.Result result = MoveProcessor.process(board, cell);
        String json = buildResponse(result.board(), result.status(), result.message());
        sendResponse(exchange, 200, json);
    }

    // --- JSON parsers (manual, no external lib) ---

    // parse "board" array from JSON body
    private int[] parseBoard(String body) {
        int start = body.indexOf("[");
        int end = body.indexOf("]");
        if (start == -1 || end == -1) throw new IllegalArgumentException("No board array");

        String arr = body.substring(start + 1, end);
        String[] parts = arr.split(",");
        if (parts.length != 9) throw new IllegalArgumentException("Board must have 9 cells");

        int[] board = new int[9];
        for (int i = 0; i < 9; i++) {
            board[i] = Integer.parseInt(parts[i].trim());
        }
        return board;
    }

    // parse "cell" integer from JSON body
    private int parseCell(String body) {
        String key = "\"cell\"";
        int idx = body.indexOf(key);
        if (idx == -1) throw new IllegalArgumentException("No cell field");

        int colon = body.indexOf(":", idx + key.length());
        int numStart = colon + 1;
        while (numStart < body.length() && body.charAt(numStart) == ' ') numStart++;

        int numEnd = numStart;
        while (numEnd < body.length() && Character.isDigit(body.charAt(numEnd))) numEnd++;

        return Integer.parseInt(body.substring(numStart, numEnd));
    }

    // --- game state check ---

    private static final int[][] WIN_LINES = {
        {0,1,2},{3,4,5},{6,7,8},   // rows
        {0,3,6},{1,4,7},{2,5,8},   // cols
        {0,4,8},{2,4,6}            // diagonals
    };

    // true if someone has won or the board is full
    private boolean isGameOver(int[] board) {
        for (int[] line : WIN_LINES) {
            int v = board[line[0]];
            if (v != 0 && v == board[line[1]] && v == board[line[2]]) return true;
        }
        for (int cell : board) { if (cell == 0) return false; }
        return true;
    }

    // --- response builders ---

    private String buildResponse(int[] board, String status, String message) {
        String boardArr = Arrays.stream(board)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));
        return String.format(
            "{\"board\":[%s],\"status\":\"%s\",\"message\":\"%s\"}",
            boardArr, status, message
        );
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
    }

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String json = String.format("{\"error\":\"%s\"}", message);
        sendResponse(exchange, code, json);
    }
}
