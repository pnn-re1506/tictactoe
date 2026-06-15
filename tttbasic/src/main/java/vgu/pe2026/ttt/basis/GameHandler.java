package vgu.pe2026.ttt.basis;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * HTTP handler cho endpoint POST /api/move.
 * Không có HMAC — server tin tưởng board do client gửi lên.
 *
 * Flow:
 *   1. CORS preflight (OPTIONS) → 200
 *   2. Validate method = POST
 *   3. Parse JSON body → board[9], cell
 *   4. Validate cell [1-9]
 *   5. Validate cell chưa bị chiếm
 *   6. Validate game chưa kết thúc
 *   7. Gọi MoveProcessor.process(board, cell)
 *   8. Trả JSON response 200
 */
public class GameHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Luôn thêm CORS headers — cần thiết vì client mở từ file://
        addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();

        // Xử lý OPTIONS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        // Chỉ chấp nhận POST
        if (!"POST".equalsIgnoreCase(method)) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        // Đọc body
        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        // Parse board
        int[] board;
        try {
            board = parseBoard(body);
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid board format");
            return;
        }

        // Parse cell
        int cell;
        try {
            cell = parseCell(body);
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid cell format");
            return;
        }

        // --- Validations ---

        // 1. Cell nằm trong [1-9]
        if (cell < 1 || cell > 9) {
            sendError(exchange, 400, "Cell must be between 1 and 9");
            return;
        }

        // 2. Cell chưa bị chiếm
        if (board[cell - 1] != 0) {
            sendError(exchange, 400, "Cell is already occupied");
            return;
        }

        // 3. Game chưa kết thúc (kiểm tra trạng thái board hiện tại)
        if (isGameOver(board)) {
            sendError(exchange, 400, "Game already over");
            return;
        }

        // --- Xử lý nước đi ---
        MoveProcessor.Result result = MoveProcessor.process(board, cell);

        // --- Trả response ---
        String json = buildResponse(result.board(), result.status(), result.message());
        sendResponse(exchange, 200, json);
    }

    // -------------------------------------------------------
    // JSON Parsers (tự build, không cần thư viện ngoài)
    // -------------------------------------------------------

    /**
     * Parse "board" từ JSON body.
     * Ví dụ: {"board":[0,0,0,0,0,0,0,0,0],"cell":5}
     */
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

    /**
     * Parse "cell" từ JSON body.
     * Ví dụ: {"board":[...],"cell":5}
     */
    private int parseCell(String body) {
        String key = "\"cell\"";
        int idx = body.indexOf(key);
        if (idx == -1) throw new IllegalArgumentException("No cell field");

        // Tìm số sau dấu ':' kể từ vị trí key
        int colon = body.indexOf(":", idx + key.length());
        int numStart = colon + 1;

        // Bỏ qua khoảng trắng
        while (numStart < body.length() && body.charAt(numStart) == ' ') numStart++;

        // Đọc đến ký tự không phải số
        int numEnd = numStart;
        while (numEnd < body.length() && Character.isDigit(body.charAt(numEnd))) numEnd++;

        return Integer.parseInt(body.substring(numStart, numEnd));
    }

    // -------------------------------------------------------
    // Game state check
    // -------------------------------------------------------

    private static final int[][] WIN_LINES = {
        {0,1,2},{3,4,5},{6,7,8},   // rows
        {0,3,6},{1,4,7},{2,5,8},   // cols
        {0,4,8},{2,4,6}            // diagonals
    };

    /** Kiểm tra board hiện tại đã kết thúc chưa (ai đó thắng hoặc full). */
    private boolean isGameOver(int[] board) {
        for (int[] line : WIN_LINES) {
            int v = board[line[0]];
            if (v != 0 && v == board[line[1]] && v == board[line[2]]) return true;
        }
        for (int cell : board) { if (cell == 0) return false; }
        return true; // full → draw
    }

    // -------------------------------------------------------
    // JSON Builder
    // -------------------------------------------------------

    private String buildResponse(int[] board, String status, String message) {
        String boardArr = Arrays.stream(board)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));
        return String.format(
            "{\"board\":[%s],\"status\":\"%s\",\"message\":\"%s\"}",
            boardArr, status, message
        );
    }

    // -------------------------------------------------------
    // HTTP Helpers
    // -------------------------------------------------------

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
