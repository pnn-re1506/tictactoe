package vgu.pe2026.ttt.basis;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet("/move")
public class MoveServlet extends HttpServlet {

    private static final Gson GSON = new Gson();
    private static final ComputerPlayer COMPUTER = new ComputerPlayer();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        MoveRequest moveReq = GSON.fromJson(body, MoveRequest.class);

        MoveResponse moveResp = handleMove(moveReq);

        resp.getWriter().write(GSON.toJson(moveResp));
    }

    // Reuse from Server.java
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
