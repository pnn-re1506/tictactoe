package vgu.pe2026.ttt.basis;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Servlet wrapper cho game logic.
 * Tái sử dụng: MoveProcessor, Board, ComputerPlayer (không thay đổi).
 * Logic parse JSON + build response được giữ nguyên từ GameHandler.
 */
@WebServlet("/api/move")
public class GameServlet extends HttpServlet {

    // --- CORS preflight ---

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        addCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // --- Main handler: POST /api/move ---

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        addCorsHeaders(resp);
        resp.setContentType("application/json;charset=UTF-8");

        // Đọc body (giữ nguyên cách đọc từ GameHandler, chỉ đổi nguồn stream)
        String body = req.getReader().lines()
                .collect(Collectors.joining())
                .trim();

        // Parse board & cell — tái sử dụng nguyên xi logic từ GameHandler
        int[] board;
        int cell;
        try {
            board = parseBoard(body);
            cell  = parseCell(body);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid board format");
            return;
        }

        if (cell < 1 || cell > 9) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cell must be between 1 and 9");
            return;
        }

        if (board[cell - 1] != 0) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cell is already occupied");
            return;
        }

        if (isGameOver(board)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Game already over");
            return;
        }

        // Gọi MoveProcessor — tái sử dụng nguyên xi, không thay đổi
        MoveProcessor.Result result = MoveProcessor.process(board, cell);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(buildResponse(result.board(), result.status(), result.message()));
    }

    // -------------------------------------------------------
    // Tái sử dụng nguyên xi từ GameHandler (copy, không đổi logic)
    // -------------------------------------------------------

    private int[] parseBoard(String body) {
        int start = body.indexOf("[");
        int end   = body.indexOf("]");
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

    private int parseCell(String body) {
        String key = "\"cell\"";
        int idx = body.indexOf(key);
        if (idx == -1) throw new IllegalArgumentException("No cell field");

        int colon    = body.indexOf(":", idx + key.length());
        int numStart = colon + 1;
        while (numStart < body.length() && body.charAt(numStart) == ' ') numStart++;

        int numEnd = numStart;
        while (numEnd < body.length() && Character.isDigit(body.charAt(numEnd))) numEnd++;

        return Integer.parseInt(body.substring(numStart, numEnd));
    }

    // Tái sử dụng Board.hasWon() và Board.isFull() — không duplicate logic win/draw
    private boolean isGameOver(int[] arr) {
        Board b = new Board();
        for (int i = 0; i < 9; i++) {
            if (arr[i] != 0) b.place(i + 1, arr[i]);
        }
        return b.hasWon(1) || b.hasWon(2) || b.isFull();
    }

    private String buildResponse(int[] board, String status, String message) {
        String boardArr = Arrays.stream(board)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));
        return String.format(
            "{\"board\":[%s],\"status\":\"%s\",\"message\":\"%s\"}",
            boardArr, status, message
        );
    }

    private void addCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin",  "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendError(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setStatus(code);
        resp.getWriter().write(String.format("{\"error\":\"%s\"}", message));
    }
}
