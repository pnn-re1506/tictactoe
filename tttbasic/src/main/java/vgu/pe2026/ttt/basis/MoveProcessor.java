package vgu.pe2026.ttt.basis;

import java.util.Arrays;

/**
 * Pure function: (board, cell) → Result.
 * Không có state, không có side effect.
 * Tái dụng Board + ComputerPlayer để giữ nguyên logic cũ.
 */
public class MoveProcessor {

    /** Kết quả sau một lượt đánh hoàn chỉnh (người + AI). */
    public record Result(int[] board, String status, String message) {}
    // status: "PLAYING" | "WIN_PLAYER" | "WIN_COMPUTER" | "DRAW"

    /**
     * Xử lý một lượt: đặt quân người → AI phản đòn → trả Result.
     *
     * @param board mảng 9 phần tử (0=trống, 1=người, 2=AI)
     * @param cell  ô người chọn, 1-indexed (1..9)
     * @return Result chứa board mới và trạng thái game
     */
    public static Result process(int[] board, int cell) {
        // Copy board — stateless: không mutate input của request khác
        int[] newBoard = Arrays.copyOf(board, 9);

        // Wrap int[] vào Board object để tái dùng hasWon / isFull / firstEmptyCell
        Board b = arrayToBoard(newBoard);

        // 1. Đặt quân người (mark = 1)
        b.place(cell, 1);
        syncToArray(b, newBoard);

        // 2. Người thắng?
        if (b.hasWon(1))
            return new Result(newBoard, "WIN_PLAYER", "You win!");

        // 3. Hòa sau nước người?
        if (b.isFull())
            return new Result(newBoard, "DRAW", "It's a draw!");

        // 4. AI đánh — dùng ComputerPlayer để tái dùng logic chooseCell
        ComputerPlayer ai = new ComputerPlayer();
        int aiCell = ai.chooseCell(b);
        b.place(aiCell, 2);
        syncToArray(b, newBoard);

        // 5. AI thắng?
        if (b.hasWon(2))
            return new Result(newBoard, "WIN_COMPUTER", "Computer wins!");

        // 6. Hòa sau nước AI?
        if (b.isFull())
            return new Result(newBoard, "DRAW", "It's a draw!");

        return new Result(newBoard, "PLAYING", "Your turn!");
    }

    // -------------------------------------------------------
    // Helpers để bridge giữa int[] (HTTP layer) và Board object
    // -------------------------------------------------------

    /** Tạo Board từ int[9]. */
    private static Board arrayToBoard(int[] arr) {
        Board b = new Board();
        for (int i = 0; i < 9; i++) {
            if (arr[i] != 0) {
                b.place(i + 1, arr[i]);
            }
        }
        return b;
    }

    /** Đồng bộ Board → int[] (dùng getCell). */
    private static void syncToArray(Board b, int[] arr) {
        for (int i = 0; i < 9; i++) {
            arr[i] = b.getCell(i + 1);
        }
    }
}
