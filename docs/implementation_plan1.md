# Implementation Plan: TicTacToe — Stateless HTTP (No HMAC)

## Mục tiêu

Chuyển đổi TicTacToe từ **TCP socket** sang **HTTP** theo kiến trúc **Stateless**.
Phiên bản này **bỏ qua hoàn toàn HMAC** — server không ký token, không xác minh board.
Client tự giữ board state, server tin tưởng hoàn toàn vào dữ liệu client gửi lên.

> [!NOTE]
> Đây là phiên bản đơn giản hoá, phù hợp để học HTTP + stateless design trước.
> HMAC sẽ được thêm vào ở phiên bản tiếp theo như một lớp bảo vệ bổ sung.

---

## Kiến trúc: Stateless, No Token

```
Client (browser)                    Server (stateless)
       │                                   │
       │── POST /api/move ───────────────▶ │
       │   { board: [0..0], cell: 5 }      │  1. validate cell [1-9]
       │                                   │  2. validate cell chưa bị chiếm
       │                                   │  3. đặt quân người (mark=1)
       │                                   │  4. kiểm tra người thắng / hòa
       │                                   │  5. AI đánh (mark=2)
       │                                   │  6. kiểm tra AI thắng / hòa
       │◀── { board, status, message } ── │
       │                                   │
       │  Client lưu board mới             │
       │  (Không có token!)                │
```

**Không có `secretKey`, không có `HmacUtil`.**
Mỗi request là độc lập. Server xử lý xong → quên luôn.

---

## Phân tích file hiện tại

```
Board.java          ✅ KHÔNG THAY ĐỔI — logic board thuần
Player.java         ✅ KHÔNG THAY ĐỔI — abstract class
ComputerPlayer.java ✅ KHÔNG THAY ĐỔI — AI logic
HumanPlayer.java    🗑️ XÓA — đọc stdin, không dùng trong HTTP
GamePlay.java       🗑️ XÓA — vòng lặp đồng bộ, không dùng trong HTTP
Server.java         🗑️ XÓA — TCP ServerSocket, thay bằng HTTP
Client.java         🗑️ XÓA — TCP Socket, thay bằng browser
Main.java           ✏️ SỬA — gọi HttpGameServer.start() thay vì GamePlay
```

---

## File mới cần tạo

```
MoveProcessor.java  🆕 — pure function: (board[], cell) → Result
GameHandler.java    🆕 — xử lý HTTP request /api/move, routing, CORS
HttpGameServer.java 🆕 — khởi động HttpServer, setExecutor(null)
index.html          🆕 — giao diện browser, fetch() API
```

> [!IMPORTANT]
> So với plan HMAC gốc: **không tạo `HmacUtil.java`**.
> `GameHandler` đơn giản hơn vì không cần bước verify token.

---

## Thiết kế chi tiết

### [NEW] `MoveProcessor.java` — pure function, không có state

```java
public class MoveProcessor {

    public record Result(int[] board, String status, String message) {}
    // status: "PLAYING" | "WIN_PLAYER" | "WIN_COMPUTER" | "DRAW"

    public static Result process(int[] board, int cell) {
        int[] newBoard = Arrays.copyOf(board, 9);  // không mutate input

        // Đặt quân người (mark = 1) tại cell (1-indexed)
        newBoard[cell - 1] = 1;

        if (hasWon(newBoard, 1))
            return new Result(newBoard, "WIN_PLAYER", "You win!");

        if (isFull(newBoard))
            return new Result(newBoard, "DRAW", "It's a draw!");

        // AI đánh (mark = 2)
        int aiCell = chooseAiCell(newBoard);  // dùng ComputerPlayer logic
        newBoard[aiCell - 1] = 2;

        if (hasWon(newBoard, 2))
            return new Result(newBoard, "WIN_COMPUTER", "Computer wins!");

        if (isFull(newBoard))
            return new Result(newBoard, "DRAW", "It's a draw!");

        return new Result(newBoard, "PLAYING", "Your turn!");
    }

    // helper: kiểm tra 8 tổ hợp thắng
    private static boolean hasWon(int[] b, int mark) { ... }

    // helper: kiểm tra full
    private static boolean isFull(int[] b) { ... }

    // helper: AI chọn ô (wrap ComputerPlayer hoặc chọn đơn giản)
    private static int chooseAiCell(int[] b) { ... }
}
```

> Dùng `Arrays.copyOf` vì stateless — mỗi request phải độc lập, không share state.

---

### [NEW] `GameHandler.java` — HTTP request handler

```
Nhận: POST /api/move
      body = { "board": [0,0,0,0,0,0,0,0,0], "cell": 5 }

Logic (không có bước verify token):
  1. Nếu method != POST → 405 Method Not Allowed
  2. Đọc body → parse "board" (array 9 phần tử) và "cell" (int)
  3. Validate cell nằm trong [1-9]
       → Sai: 400 { "error": "Cell must be between 1 and 9" }
  4. Validate cell chưa bị chiếm (board[cell-1] == 0)
       → Sai: 400 { "error": "Cell is already occupied" }
  5. Validate game chưa kết thúc (không ai thắng, không full)
       → Sai: 400 { "error": "Game already over" }
  6. Gọi MoveProcessor.process(board, cell)
  7. response 200 { "board": [...], "status": "...", "message": "..." }
```

**CORS headers (bắt buộc vì browser `file://` gọi `http://localhost`):**
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: POST, OPTIONS
Access-Control-Allow-Headers: Content-Type
```
Xử lý `OPTIONS` preflight: trả 200 ngay, không xử lý game logic.

**JSON tự build bằng String (không cần dependency):**
```java
String buildResponse(int[] board, String status, String message) {
    return String.format(
        "{\"board\":[%s],\"status\":\"%s\",\"message\":\"%s\"}",
        Arrays.stream(board).mapToObj(String::valueOf).collect(Collectors.joining(",")),
        status, message
    );
}
```

---

### [NEW] `HttpGameServer.java` — khởi động server

```java
public class HttpGameServer {
    public static void start(int port) throws IOException {
        GameHandler handler = new GameHandler();   // không cần HmacUtil

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/move", handler);
        server.setExecutor(null);   // single-threaded
        server.start();

        System.out.println("Server started on http://localhost:" + port);
    }
}
```

---

### [MODIFY] `Main.java` — cập nhật entry point

```java
public static void main(String[] args) throws Exception {
    int port = 8080;
    if (args.length >= 1) {
        port = Integer.parseInt(args[0]);
    }
    HttpGameServer.start(port);
}
```

---

### [NEW] `index.html` — giao diện browser

**Trạng thái JS client (đơn giản hơn — không có `token`):**
```javascript
let board = null;   // null = chưa bắt đầu
// Không có biến token!
```

**Giao diện:**
```
[New Game]  → reset board về [0..0], enable input
Grid 3x3    → hiển thị ô trống / X / O
Status bar  → "Your turn!", "You win!", v.v.
[Move]      → gọi API với board + cell
```

**Flow:**
```javascript
startGame():
  board = [0,0,0,0,0,0,0,0,0]
  render()

makeMove(cell):
  res = await fetch("POST /api/move", { board, cell })
  board = res.board
  renderBoard(board)
  showStatus(res.status, res.message)
  if (res.status != "PLAYING") disableInput()
```

> [!NOTE]
> Không có `token` trong request/response — đây là điểm đơn giản nhất so với plan HMAC.

---

## API Contract

### `POST /api/move`

**Request (mọi lượt, kể cả lượt đầu):**
```json
{ "board": [0,0,0,0,0,0,0,0,0], "cell": 5 }
```
- `board`: mảng 9 phần tử, `0`=trống, `1`=người, `2`=AI
- `cell`: số từ 1–9 (1-indexed, trái-trên → phải-dưới)
- **Không có `token`**

**Response 200:**
```json
{
  "board": [0,0,0,0,1,0,0,2,0],
  "status": "PLAYING",
  "message": "Your turn!"
}
```
- `status`: `"PLAYING"` | `"WIN_PLAYER"` | `"WIN_COMPUTER"` | `"DRAW"`
- **Không có `token`**

**Response 400:**
```json
{ "error": "Cell must be between 1 and 9" }
{ "error": "Cell is already occupied" }
{ "error": "Game already over" }
```

---

## So sánh với plan HMAC

| Điểm | Plan HMAC | Plan No-HMAC (này) |
|---|---|---|
| `HmacUtil.java` | ✅ Có | ❌ Không tạo |
| `secretKey` trong server | ✅ Có | ❌ Không |
| `token` trong request | ✅ Có | ❌ Không |
| `token` trong response | ✅ Có | ❌ Không |
| Bước verify trong `GameHandler` | ✅ Có | ❌ Bỏ qua |
| Chống gian lận | ✅ Có | ❌ Không (client tin tưởng) |
| Độ phức tạp | Cao hơn | Đơn giản hơn |
| Mục đích | Production-ready | Học HTTP + stateless |

---

## Verification Plan

### Automated (curl)

```bash
# 1. Build
mvn package -q

# 2. Khởi động server
java -jar target/tttbasic-1.0-SNAPSHOT.jar 8080

# 3. Game mới — đánh ô 5
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d '{"board":[0,0,0,0,0,0,0,0,0],"cell":5}'
# Expect: board có 1 ở index 4, AI đã đánh, status="PLAYING"

# 4. Đánh tiếp ô 1 (dùng board từ response trên)
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d '{"board":[0,0,0,0,1,0,0,2,0],"cell":1}'
# Expect: 200, status="PLAYING" hoặc game over

# 5. Test cell sai
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d '{"board":[0,0,0,0,0,0,0,0,0],"cell":10}'
# Expect: 400, error="Cell must be between 1 and 9"

# 6. Test cell bị chiếm
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d '{"board":[0,0,0,0,1,0,0,2,0],"cell":5}'
# Expect: 400, error="Cell is already occupied"

# 7. Multi-client: 2 game độc lập
curl -s -X POST http://localhost:8080/api/move -H "Content-Type: application/json" \
  -d '{"board":[0,0,0,0,0,0,0,0,0],"cell":1}'
curl -s -X POST http://localhost:8080/api/move -H "Content-Type: application/json" \
  -d '{"board":[0,0,0,0,0,0,0,0,0],"cell":9}'
# Expect: 2 response độc lập, không bị nhiễu nhau
```

### Manual (browser)

1. Mở `index.html` bằng `file://` trong browser
2. Bấm **New Game** → board hiện ra
3. Chơi ván hoàn chỉnh từ đầu đến thắng/thua/hòa
4. Mở 2 tab → xác nhận 2 game hoàn toàn độc lập
