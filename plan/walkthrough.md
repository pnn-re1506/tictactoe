# Implement HMAC Security cho Stateless Tic-Tac-Toe

Tôi đã hoàn tất việc thực thi kế hoạch refactor bảo mật bằng HMAC SHA-256 theo đúng `implementation_plan.md` bạn cung cấp. Dưới đây là tóm tắt các thay đổi đã thực hiện và kết quả kiểm thử.

## 1. Các thay đổi đã thực hiện

### Server Component (`Server.java`)
- Bổ sung thư viện mã hóa `javax.crypto.Mac` và `javax.crypto.spec.SecretKeySpec`.
- Thêm hằng số `SECRET_KEY` cố định.
- Triển khai hàm `generateHMAC(String data)` trả về chuỗi Base64 của thuật toán HMAC SHA-256.
- Cập nhật luồng xử lý `handleMove` để:
  - Hỗ trợ câu lệnh `START` từ Client (Cấp mã Token khởi tạo: bàn cờ rỗng + chữ ký).
  - Phân tích và kiểm chứng chữ ký của yêu cầu `MOVE`. Trả về `RESULT error 000000000 Invalid_Signature` nếu giả mạo.
  - Luôn đính kèm chữ ký mới (`new_signature`) cho mọi bàn cờ trả về.

```diff:Server.java
package vgu.pe2026.ttt.basis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 12345;

    public static void main(String[] args) {
        ComputerPlayer computer = new ComputerPlayer();

        // Outer try-with-resources: manages the ServerSocket lifecycle
        // Automatically closes the port when the server shuts down
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            // Infinite loop to keep the server running and accepting new connections
            while (true) {
                // Inner try-with-resources: manages a single client connection lifecycle
                // accept() blocks until a client connects. 
                // socket, input, and output are automatically closed at the end of this block,
                // enforcing the stateless architecture.
                try (Socket socket = serverSocket.accept();
                        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

                    // Read request from client
                    String request = input.readLine();
                    if (request == null || request.isBlank()) {
                        output.println("ERROR Empty request");
                        continue;
                    }

                    // Process the move and get response string
                    String response = handleMove(request, computer);
                    
                    // Send response back to client
                    output.println(response);
                    // The socket is automatically closed when exiting this block
                    // output.close()
                    // input.close()
                    // socket.close()

                } catch (IOException e) {
                    System.out.println("Connection error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Cannot start server: " + e.getMessage());
        }
    }

    // Core game logic processor
    private static String handleMove(String request, ComputerPlayer computer) {
        // Parse request: e.g., "MOVE 100000000 5"
        String[] parts = request.split(" ");
        if (parts.length != 3 || !parts[0].equals("MOVE")) {
            return "RESULT invalid 000000000";
        }

        Board board;
        int humanMove;
        try {
            board = Board.fromLine(parts[1]); // Reconstruct board state
            humanMove = Integer.parseInt(parts[2]); // Parse requested move
        } catch (IllegalArgumentException e) {
            return "RESULT invalid 000000000";
        }

        // Validate human move against the reconstructed board
        if (!board.isValidMove(humanMove)) {
            return "RESULT invalid " + board.toLine();
        }

        // Apply human move
        board.place(humanMove, Board.HUMAN);
        if (board.hasWon(Board.HUMAN)) {
            return "RESULT win " + board.toLine();
        }
        if (board.isFull()) {
            return "RESULT draw " + board.toLine();
        }

        // AI turn
        int computerMove = computer.chooseCell(board);
        if (computerMove != -1) {
            board.place(computerMove, Board.COMPUTER);
        }

        // Check for AI win or draw
        if (board.hasWon(Board.COMPUTER)) {
            return "RESULT lose " + board.toLine();
        }
        if (board.isFull()) {
            return "RESULT draw " + board.toLine();
        }

        // Game continues
        return "RESULT ongoing " + board.toLine();
    }

}
===
package vgu.pe2026.ttt.basis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class Server {

    private static final int PORT = 12345;
    private static final String SECRET_KEY = "Vgu_Tictactoe_Secret";

    private static String generateHMAC(String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }

    public static void main(String[] args) {
        ComputerPlayer computer = new ComputerPlayer();

        // Outer try-with-resources: manages the ServerSocket lifecycle
        // Automatically closes the port when the server shuts down
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            // Infinite loop to keep the server running and accepting new connections
            while (true) {
                // Inner try-with-resources: manages a single client connection lifecycle
                // accept() blocks until a client connects. 
                // socket, input, and output are automatically closed at the end of this block,
                // enforcing the stateless architecture.
                try (Socket socket = serverSocket.accept();
                        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

                    // Read request from client
                    String request = input.readLine();
                    if (request == null || request.isBlank()) {
                        output.println("ERROR Empty request");
                        continue;
                    }

                    // Process the move and get response string
                    String response = handleMove(request, computer);
                    
                    // Send response back to client
                    output.println(response);
                    // The socket is automatically closed when exiting this block
                    // output.close()
                    // input.close()
                    // socket.close()

                } catch (IOException e) {
                    System.out.println("Connection error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Cannot start server: " + e.getMessage());
        }
    }

    // Core game logic processor
    private static String handleMove(String request, ComputerPlayer computer) {
        if (request.equals("START")) {
            String initialBoard = "000000000";
            return "RESULT start " + initialBoard + " " + generateHMAC(initialBoard);
        }

        // Parse request: e.g., "MOVE 100000000 5 signature"
        String[] parts = request.split(" ");
        if (parts.length != 4 || !parts[0].equals("MOVE")) {
            return "RESULT invalid 000000000 " + generateHMAC("000000000");
        }

        String boardLine = parts[1];
        int humanMove;
        String clientSignature = parts[3];

        // Validate HMAC signature
        if (!generateHMAC(boardLine).equals(clientSignature)) {
            return "RESULT error 000000000 Invalid_Signature";
        }

        Board board;
        try {
            board = Board.fromLine(boardLine); // Reconstruct board state
            humanMove = Integer.parseInt(parts[2]); // Parse requested move
        } catch (IllegalArgumentException e) {
            return "RESULT invalid " + boardLine + " " + generateHMAC(boardLine);
        }

        // Validate human move against the reconstructed board
        if (!board.isValidMove(humanMove)) {
            return "RESULT invalid " + board.toLine() + " " + generateHMAC(board.toLine());
        }

        // Apply human move
        board.place(humanMove, Board.HUMAN);
        if (board.hasWon(Board.HUMAN)) {
            return "RESULT win " + board.toLine() + " " + generateHMAC(board.toLine());
        }
        if (board.isFull()) {
            return "RESULT draw " + board.toLine() + " " + generateHMAC(board.toLine());
        }

        // AI turn
        int computerMove = computer.chooseCell(board);
        if (computerMove != -1) {
            board.place(computerMove, Board.COMPUTER);
        }

        // Check for AI win or draw
        if (board.hasWon(Board.COMPUTER)) {
            return "RESULT lose " + board.toLine() + " " + generateHMAC(board.toLine());
        }
        if (board.isFull()) {
            return "RESULT draw " + board.toLine() + " " + generateHMAC(board.toLine());
        }

        // Game continues
        return "RESULT ongoing " + board.toLine() + " " + generateHMAC(board.toLine());
    }

}
```

### Client Component (`Client.java`)
- Bổ sung bước gửi yêu cầu `START` trước khi vào vòng lặp game để nhận Token khởi tạo.
- Điều chỉnh lệnh gửi `MOVE` để luôn đính kèm `currentSignature`.
- Xử lý phản hồi từ Server gồm 4 thành phần (Thêm `new_signature` ở cuối).
- Cập nhật `currentSignature` sau mỗi lượt đi hợp lệ hoặc không hợp lệ.
- Bắt và xử lý trạng thái lỗi bảo mật `error`.

```diff:Client.java
package vgu.pe2026.ttt.basis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    // Server configuration
    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        Board board = new Board();

        try (Scanner scanner = new Scanner(System.in)) {
            HumanPlayer human = new HumanPlayer(scanner);

            System.out.println("\n=== Tic-Tac-Toe ===");
            System.out.println("You are 1. Computer is 2.");

            // Main game loop - represents turns
            while (true) {
                board.printMatrix();
                
                // 1. Get input from human player
                int humanMove = human.chooseCell(board);
                // If human player types 'q' to quit, chooseCell returns -1
                if (humanMove == -1) {
                    System.out.println("End of the game");
                    return;
                }

                // 2. Send move to server over a stateless connection
                String response = send("MOVE " + board.toLine() + " " + humanMove);
                
                // 3. Parse server response ("RESULT <status> <new_board>")
                String[] parts = response.split(" ");

                if (parts.length != 3 || !parts[0].equals("RESULT")) {
                    System.out.println("Invalid server response: " + response);
                    return; // Exit game on bad server response
                }

                String status = parts[1];
                if (status.equals("invalid")) {
                    System.out.println("Invalid move. Please try again.");
                    continue; // Skip the rest of the loop and prompt human again
                }

                // 4. Update local board with the state provided by the server
                board = Board.fromLine(parts[2]);

                if (status.equals("ongoing")) {
                    continue; // Game is not over, loop back to human's turn
                }

                // Game over
                board.printMatrix();
                printResult(status);
                return;
            }
        } catch (IOException e) {
            System.out.println("Cannot connect to server: " + e.getMessage());
        }
    }

    /**
     * Helper method to send a request to the server and receive its response.
     * Implements a stateless connection: Opens socket, sends, receives, then closes immediately.
     */
    private static String send(String request) throws IOException {
        // The try-with-resources syntax automatically closes socket, input, and output
        // when the block exits, preventing resource leaks.
        try (Socket socket = new Socket(HOST, PORT);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) { // true = auto-flush

            output.println(request); // Send request string
            return input.readLine(); // Wait for and return the response line
        }
    }

    private static void printResult(String status) {
        if (status.equals("win")) {
            System.out.println("Player#1 won!");
        } else if (status.equals("lose")) {
            System.out.println("Player#2 won!");
        } else if (status.equals("draw")) {
            System.out.println("It's a draw!");
        } else {
            System.out.println("Game ended with status: " + status);
        }
    }
}
===
package vgu.pe2026.ttt.basis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    // Server configuration
    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        Board board = new Board();

        try (Scanner scanner = new Scanner(System.in)) {
            HumanPlayer human = new HumanPlayer(scanner);

            System.out.println("\n=== Tic-Tac-Toe ===");
            System.out.println("You are 1. Computer is 2.");

            // Request initial state and token
            String startResponse = send("START");
            String[] startParts = startResponse.split(" ");
            if (startParts.length != 4 || !startParts[0].equals("RESULT") || !startParts[1].equals("start")) {
                System.out.println("Failed to start game: " + startResponse);
                return;
            }
            board = Board.fromLine(startParts[2]);
            String currentSignature = startParts[3];

            // Main game loop - represents turns
            while (true) {
                board.printMatrix();
                
                // 1. Get input from human player
                int humanMove = human.chooseCell(board);
                // If human player types 'q' to quit, chooseCell returns -1
                if (humanMove == -1) {
                    System.out.println("End of the game");
                    return;
                }

                // 2. Send move to server over a stateless connection
                String response = send("MOVE " + board.toLine() + " " + humanMove + " " + currentSignature);
                
                // 3. Parse server response ("RESULT <status> <new_board> <new_signature>")
                String[] parts = response.split(" ");

                if (parts.length != 4 || !parts[0].equals("RESULT")) {
                    System.out.println("Invalid server response: " + response);
                    return; // Exit game on bad server response
                }

                String status = parts[1];
                if (status.equals("error")) {
                    System.out.println("Security Error: " + response);
                    return;
                }
                if (status.equals("invalid")) {
                    System.out.println("Invalid move. Please try again.");
                    currentSignature = parts[3];
                    continue; // Skip the rest of the loop and prompt human again
                }

                // 4. Update local board with the state provided by the server
                board = Board.fromLine(parts[2]);
                currentSignature = parts[3];

                if (status.equals("ongoing")) {
                    continue; // Game is not over, loop back to human's turn
                }

                // Game over
                board.printMatrix();
                printResult(status);
                return;
            }
        } catch (IOException e) {
            System.out.println("Cannot connect to server: " + e.getMessage());
        }
    }

    /**
     * Helper method to send a request to the server and receive its response.
     * Implements a stateless connection: Opens socket, sends, receives, then closes immediately.
     */
    private static String send(String request) throws IOException {
        // The try-with-resources syntax automatically closes socket, input, and output
        // when the block exits, preventing resource leaks.
        try (Socket socket = new Socket(HOST, PORT);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) { // true = auto-flush

            output.println(request); // Send request string
            return input.readLine(); // Wait for and return the response line
        }
    }

    private static void printResult(String status) {
        if (status.equals("win")) {
            System.out.println("Player#1 won!");
        } else if (status.equals("lose")) {
            System.out.println("Player#2 won!");
        } else if (status.equals("draw")) {
            System.out.println("It's a draw!");
        } else {
            System.out.println("Game ended with status: " + status);
        }
    }
}
```

## 2. Kết quả kiểm thử (Verification)

> [!SUCCESS]
> **Happy Path**: Tôi đã chạy đồng thời Server và Client. Việc gửi và nhận lượt đi diễn ra bình thường. Client duy trì và cập nhật trạng thái bàn cờ chính xác thông qua chuỗi signature cung cấp bởi Server.

> [!SUCCESS]
> **Anti-Cheating Test**: Tôi đã thử nghiệm thay đổi mã nguồn Client giả lập hành vi gian lận bằng cách gửi một bàn cờ giả mạo (ví dụ: `111000000`) cùng với một chữ ký không khớp. Server ngay lập tức phát hiện và phản hồi lỗi bảo mật, khiến Client đóng kết nối với thông báo `Security Error: RESULT error 000000000 Invalid_Signature`. Chức năng ngăn chặn State Manipulation đã hoạt động xuất sắc.

Codebase hiện tại đã bảo mật và an toàn hơn trước hành vi giả mạo trạng thái từ phía Client. Bạn có thể kiểm tra qua artifact này. Mọi thứ đã sẵn sàng!
