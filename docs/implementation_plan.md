# Implementation Plan: TicTacToe — Stateless HTTP + HMAC

## Quyết định đã xác nhận
- Architecture: **Stateless** — server không lưu state per-game
- Chống gian lận: **HMAC** (HmacSHA256, built-in Java `javax.crypto`)
- Branch: `main`
- Server: single-threaded, `com.sun.net.httpserver` (không cần dependency)
- JSON: tự build String (không cần dependency)
- Client: `index.html` mở bằng `file://` trong browser
- Game mode: người vs computer

---

## Kiến trúc stateless với HMAC

```
Client                              Server (stateless)
  │                                      │
  │── POST /api/move ──────────────────▶ │
  │   { board: [0..0], cell: 5 }         │  1. board toàn 0? → không cần verify
  │                                      │  2. validate cell
  │                                      │  3. đặt quân người → AI đánh
  │                                      │  4. token = HMAC(newBoard, secretKey)
  │◀── { board, token, status } ─────── │
  │                                      │
  │  Client lưu board + token            │
  │                                      │
  │── POST /api/move ──────────────────▶ │
  │   { board, token, cell: 1 }          │  1. verify: HMAC(board, secretKey) == token?
  │                                      │     ❌ FAIL → 400 Invalid board
  │                                      │     ✅ PASS → xử lý tiếp
  │◀── { board, token, status } ─────── │
```

**secretKey:** sinh ngẫu nhiên 1 lần khi server khởi động, giữ trong RAM.
Không lưu ra file, không chia sẻ với client. Server restart → key mới,
mọi token cũ đều vô hiệu (client phải bắt đầu game mới).

---

## Phân tích file hiện tại trên `main`

```
Board.java          ✅ KHÔNG THAY ĐỔI — logic thuần
Player.java         ✅ KHÔNG THAY ĐỔI — abstract class
ComputerPlayer.java ✅ KHÔNG THAY ĐỔI — AI logic
HumanPlayer.java    🗑️ XÓA — đọc stdin, không dùng trong HTTP
GamePlay.java       🗑️ XÓA — vòng lặp đồng bộ, không dùng trong HTTP
Server.java         🗑️ XÓA — TCP ServerSocket, thay bằng HTTP
Client.java         🗑️ XÓA — TCP Socket, thay bằng browser
Main.java           ✏️ SỬA — gọi HttpGameServer.start() thay vì GamePlay
```

**Không có GameSession, không có GameStore** — đây là điểm khác biệt
quan trọng so với stateful.

---

## File mới cần tạo

```
GameHandler.java    🆕 — xử lý HTTP request /api/move
HttpGameServer.java 🆕 — khởi động HTTP server
MoveProcessor.java  🆕 — pure function: (board, cell) → kết quả
HmacUtil.java       🆕 — ký và xác minh board state
index.html          🆕 — giao diện browser (file://)
```

---

## Thiết kế chi tiết từng file mới

### `HmacUtil.java`

Dùng `javax.crypto.Mac` (built-in Java, không cần dependency).

```java
public class HmacUtil {
    private final SecretKeySpec key;

    public HmacUtil() {
        // Sinh key ngẫu nhiên 256-bit khi server khởi động
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        this.key = new SecretKeySpec(raw, "HmacSHA256");
    }

    // Tạo token cho board
    public String sign(int[] board) {
        // input: "0,0,0,0,1,0,0,2,0"
        // output: hex string của HMAC
    }

    // Kiểm tra token có khớp với board không
    public boolean verify(int[] board, String token) {
        return sign(board).equals(token);
    }
}
```

### `MoveProcessor.java` — pure function, không có state

```java
public class MoveProcessor {

    // Kết quả sau 1 lượt đánh
    public record Result(int[] board, String status, String message) {}
    // status: "PLAYING" | "WIN_PLAYER" | "WIN_COMPUTER" | "DRAW"

    public static Result process(int[] board, int cell) {
        // 1. Copy board (không mutate input)
        int[] newBoard = Arrays.copyOf(board, 9);

        // 2. Đặt quân người (mark = 1)
        newBoard[cell - 1] = 1;

        // 3. Kiểm tra người thắng
        if (hasWon(newBoard, 1))
            return new Result(newBoard, "WIN_PLAYER", "You win!");

        // 4. Kiểm tra hòa
        if (isFull(newBoard))
            return new Result(newBoard, "DRAW", "It's a draw!");

        // 5. AI đánh (mark = 2)
        int aiCell = chooseAiCell(newBoard);
        newBoard[aiCell - 1] = 2;

        // 6. Kiểm tra AI thắng
        if (hasWon(newBoard, 2))
            return new Result(newBoard, "WIN_COMPUTER", "Computer wins!");

        // 7. Kiểm tra hòa sau AI
        if (isFull(newBoard))
            return new Result(newBoard, "DRAW", "It's a draw!");

        return new Result(newBoard, "PLAYING", "Your turn!");
    }
}
```

> Lý do dùng `Arrays.copyOf`: stateless có nghĩa là mỗi request độc lập,
> không được sửa dữ liệu của request khác. Copy board trước khi xử lý
> đảm bảo không có side effect.

### `GameHandler.java` — xử lý request

```
Nhận: POST /api/move
      body = { "board": [...], "cell": 5, "token": "abc..." }

Logic:
  1. Parse body → board[], cell, token
  2. Validate cell nằm trong [1-9]
  3. Nếu board là [0,0,...,0]:
       → game mới, bỏ qua verify token
  4. Nếu board không phải toàn 0:
       → verify: HmacUtil.verify(board, token) == true?
       → Sai → response 400 { "error": "Invalid board state" }
  5. Validate cell chưa bị chiếm (board[cell-1] == 0)
       → Sai → response 400 { "error": "Cell is already occupied" }
  6. Validate game chưa kết thúc
       → Sai → response 400 { "error": "Game already over" }
  7. Gọi MoveProcessor.process(board, cell)
  8. Ký board mới: newToken = HmacUtil.sign(result.board())
  9. response 200 { "board": [...], "token": newToken, "status": "...", "message": "..." }
```

Thêm header `Access-Control-Allow-Origin: *` và xử lý `OPTIONS` preflight
cho CORS (cần thiết vì browser `file://` gọi `http://localhost`).

### `HttpGameServer.java`

```java
public class HttpGameServer {
    public static void start(int port) throws IOException {
        HmacUtil hmac = new HmacUtil();           // key sinh 1 lần
        GameHandler handler = new GameHandler(hmac);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/move", handler);
        server.setExecutor(null);                 // single-threaded
        server.start();

        System.out.println("Server started on http://localhost:" + port);
    }
}
```

### `Main.java` — sửa lại

```java
public static void main(String[] args) {
    int port = 8080;
    if (args.length >= 1) {
        port = Integer.parseInt(args[0]);
    }
    HttpGameServer.start(port);
}
```

### `index.html` — giao diện browser

```
Trạng thái JS client (biến toàn cục):
  let board = null;   // null = chưa bắt đầu
  let token = null;   // null = chưa bắt đầu

Giao diện:
  [New Game] → gọi startGame()
  Hiển thị board dạng text (3x3)
  <input type="number" min="1" max="9"> + [Move]
  <p> status message

startGame():
  board = [0,0,0,0,0,0,0,0,0]
  token = null
  render board
  enable input

makeMove():
  cell = parseInt(input.value)
  response = fetch POST /api/move { board, token, cell }
  board = response.board
  token = response.token
  render board + status
  if status != "PLAYING": disable input
```

Không dùng polling — chỉ gọi API khi người dùng bấm Move.

---

## API Contract (1 endpoint duy nhất)

### `POST /api/move`

**Request — lần đầu (game mới):**
```json
{ "board": [0,0,0,0,0,0,0,0,0], "cell": 5 }
```
*(không có `token`)*

**Request — các lần tiếp theo:**
```json
{ "board": [0,0,0,0,1,0,0,2,0], "token": "3a9f...", "cell": 1 }
```

**Response 200 — game đang tiếp tục:**
```json
{
  "board": [1,0,0,0,1,0,0,2,0],
  "token": "b2c8...",
  "status": "PLAYING",
  "message": "Your turn!"
}
```

**Response 200 — game kết thúc:**
```json
{
  "board": [1,1,1,0,2,0,0,2,0],
  "token": "f1a3...",
  "status": "WIN_PLAYER",
  "message": "You win!"
}
```
*(token vẫn trả về nhưng client không nên dùng nữa)*

**Response 400 — lỗi:**
```json
{ "error": "Cell is already occupied" }
{ "error": "Invalid board state" }
{ "error": "Cell must be between 1 and 9" }
{ "error": "Game already over" }
```

`board`: mảng 9 phần tử, `0`=trống, `1`=người, `2`=AI.

---

## Test bằng curl

```bash
# Bước 1: Khởi động server
mvn package -q && java -jar target/tttbasic-1.0-SNAPSHOT.jar 8080

# Bước 2: Đánh ô 5 (game mới, không cần token)
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d "{\"board\":[0,0,0,0,0,0,0,0,0],\"cell\":5}"
# → {"board":[0,0,0,0,1,0,0,2,0],"token":"3a9f...","status":"PLAYING","message":"Your turn!"}

# Bước 3: Lưu token, đánh ô 1
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d "{\"board\":[0,0,0,0,1,0,0,2,0],\"token\":\"3a9f...\",\"cell\":1}"

# Bước 4: Test gian lận — sửa board, tạo token giả
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d "{\"board\":[1,1,1,0,1,0,0,0,0],\"token\":\"fake\",\"cell\":2}"
# → HTTP 400: {"error":"Invalid board state"}

# Bước 5: Test gian lận — board đúng nhưng token sai
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d "{\"board\":[0,0,0,0,1,0,0,2,0],\"token\":\"wrongtoken\",\"cell\":1}"
# → HTTP 400: {"error":"Invalid board state"}

# Bước 6: Multi-client — 2 game độc lập cùng lúc (không cần gameId!)
# Game A: đánh ô 1
curl -s -X POST http://localhost:8080/api/move \
  -d "{\"board\":[0,0,0,0,0,0,0,0,0],\"cell\":1}" \
  -H "Content-Type: application/json"

# Game B: đánh ô 9 (cùng lúc, hoàn toàn độc lập)
curl -s -X POST http://localhost:8080/api/move \
  -d "{\"board\":[0,0,0,0,0,0,0,0,0],\"cell\":9}" \
  -H "Content-Type: application/json"
```

---

## Verification Plan

1. `mvn package` thành công, không lỗi compile
2. Server khởi động, log ra port
3. Curl test tuần tự: game mới → đánh vài nước → kết thúc
4. Curl test gian lận: 400 với token sai
5. Mở `index.html` bằng `file://`, chơi thử ván hoàn chỉnh trong browser
6. Mở 2 tab → xác nhận 2 game độc lập (stateless, không cần gameId)
