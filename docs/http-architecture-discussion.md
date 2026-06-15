# Thảo luận: Chuyển đổi TicTacToe sang Web (HTTP)

> Ngày: 2026-06-15

---

## Bối cảnh

Project TicTacToe hiện tại (`branch: main`) dùng **raw TCP socket** với giao thức
text tự định nghĩa (`MOVE|5`, `BOARD|...`, `YOUR_TURN`, `WIN|1`). Kết nối TCP tồn
tại xuyên suốt ván game, server chủ động push dữ liệu về client.

Mục tiêu: phát triển thành **web programming** — server và client giao tiếp qua **HTTP**.

---

## 1. Vấn đề cốt lõi khi chuyển sang HTTP

HTTP là **stateless** và **request-response**: client hỏi → server trả lời → connection đóng.
Điều này xung đột với model TCP cũ:

| Yêu cầu | TCP (cũ) | HTTP (thách thức) |
|---|---|---|
| Server push `YOUR_TURN` | ✅ Tự nhiên | ❌ Server không tự push được |
| Chờ nước đi đối thủ | ✅ Block `readLine()` | ❌ Connection đã đóng |
| Trạng thái game liên tục | ✅ Biến trong RAM | ❌ Mỗi request độc lập |

---

## 2. Hai hướng kiến trúc

### Stateful (Server giữ state)

```
Client → POST /api/game/abc123/move  { "cell": 5 }
Server → tra Map<gameId, GameSession> → xử lý
```

- Server lưu `GameSession` cho từng game trong `Map`
- Client chỉ gửi `gameId` + `cell`
- Server là **nguồn sự thật** (source of truth)
- Cần 3 endpoint: `POST /api/game`, `GET /api/game/{id}`, `POST /api/game/{id}/move`

### Stateless (Client giữ state)

```
Client → POST /api/move  { "board": [...], "cell": 5 }
Server → xử lý → trả board mới → quên luôn
```

- Server **không lưu gì** giữa các request
- Client gửi lên **toàn bộ board** mỗi lần
- Server là **pure function**: `(board, cell) → newBoard`
- Chỉ cần 1 endpoint: `POST /api/move`
- Multi-client tự nhiên — không cần `gameId`

---

## 3. Vấn đề gian lận trong Stateless

Vì client tự giữ board và gửi lên, client có thể gửi board giả:

```bash
# Client tự bịa board có lợi cho mình
POST /api/move  { "board": [1,1,0,0,1,0,0,0,0], "cell": 3 }
```

### Tại sao hash thuần (SHA-256) không giải quyết được

Hash là thuật toán **công khai** — client tự tính hash cho board giả được:

```
board_gia  = [1,1,0,0,1,0,0,0,0]
hash_gia   = SHA256(board_gia)    ← client tính được hoàn toàn
→ server verify: SHA256(board) == hash → PASS → bị qua mặt
```

Hash chỉ đảm bảo **data không bị lỗi khi truyền** (integrity),
không đảm bảo **data do server tạo ra** (authenticity).

### Giải pháp: HMAC (Hash + Secret Key)

`HMAC = Hash(data + secretKey)` — có thêm **secret key chỉ server biết**.

```
secretKey  = 32 byte ngẫu nhiên, sinh 1 lần khi server start, giữ trong RAM

# Server trả về:
board = [0,0,0,0,1,0,0,2,0]
token = HMAC("0,0,0,0,1,0,0,2,0", secretKey) = "a3f9..."

# Client muốn gian lận:
board_gia = [1,1,0,0,1,0,0,0,0]
token_gia = HMAC(board_gia, ???)   ← không biết secretKey → không tính được!

# Server verify:
HMAC(board_gia, secretKey) == "fake_token"? → ❌ FAIL → reject 400
```

**Token thay đổi mỗi lượt** vì board thay đổi → input HMAC thay đổi → output khác hoàn toàn.

---

## 4. Quyết định cuối cùng

| Tiêu chí | Lựa chọn |
|---|---|
| Architecture | **Stateless** |
| Chống gian lận | **HMAC (HmacSHA256)** |
| Branch | `main` |
| HTTP Server | `com.sun.net.httpserver` (built-in Java, không cần dependency) |
| JSON | Tự build String (không cần dependency) |
| Client | `index.html` mở bằng `file://` trong browser |
| Game mode | Người vs Computer |
| Threading | Single-threaded (`setExecutor(null)`) |

---

## 5. Kế hoạch thay đổi file

### Giữ nguyên ✅
- `Board.java` — logic board thuần
- `Player.java` — abstract class
- `ComputerPlayer.java` — AI

### Xóa 🗑️
- `HumanPlayer.java` — đọc stdin, không dùng trong HTTP
- `GamePlay.java` — vòng lặp đồng bộ, không dùng trong HTTP
- `Server.java` — TCP, thay bằng HTTP
- `Client.java` — TCP, thay bằng browser

### Sửa ✏️
- `Main.java` — gọi `HttpGameServer.start()` thay vì `GamePlay`

### Tạo mới 🆕
| File | Vai trò |
|---|---|
| `MoveProcessor.java` | Pure function: `(board, cell) → Result` |
| `HmacUtil.java` | Ký và verify board state bằng HMAC |
| `GameHandler.java` | Xử lý HTTP request, routing, CORS |
| `HttpGameServer.java` | Khởi động server, `setExecutor(null)` |
| `index.html` | Giao diện browser, `fetch()` API, không CSS |

---

## 6. API

Chỉ có **1 endpoint duy nhất**:

### `POST /api/move`

**Request lần đầu (game mới):**
```json
{ "board": [0,0,0,0,0,0,0,0,0], "cell": 5 }
```

**Request các lần sau:**
```json
{ "board": [0,0,0,0,1,0,0,2,0], "token": "a3f9...", "cell": 1 }
```

**Response thành công:**
```json
{
  "board": [1,0,0,0,1,0,0,2,0],
  "token": "b7c1...",
  "status": "PLAYING",
  "message": "Your turn!"
}
```
`status`: `"PLAYING"` | `"WIN_PLAYER"` | `"WIN_COMPUTER"` | `"DRAW"`

**Response lỗi (400):**
```json
{ "error": "Invalid board state" }
{ "error": "Cell is already occupied" }
{ "error": "Cell must be between 1 and 9" }
```

---

## 7. HMAC Flow hoàn chỉnh

```
Server start:
  secretKey = SecureRandom().nextBytes(32)   ← sinh 1 lần, giữ mãi

Lượt 1 (game mới):
  Client → { board=[0..0], cell=5 }
  Server: board toàn 0 → bỏ qua verify
          đặt quân người → AI đánh
          newToken = HMAC("0,0,0,0,1,0,0,2,0", secretKey)
  Server → { board=[0,0,0,0,1,0,0,2,0], token="a3f9...", status="PLAYING" }
  Client lưu: board + token

Lượt 2:
  Client → { board=[0,0,0,0,1,0,0,2,0], token="a3f9...", cell=1 }
  Server: HMAC("0,0,0,0,1,0,0,2,0", secretKey) == "a3f9..."? ✅
          đặt quân người → AI đánh
          newToken = HMAC("1,0,0,0,1,0,0,2,2", secretKey) = "b7c1..."
  Server → { board=[1,0,0,0,1,0,0,2,2], token="b7c1...", status="PLAYING" }
```

---

## 8. Test bằng curl

```bash
# Tạo game mới (lần đầu, không cần token)
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d '{"board":[0,0,0,0,0,0,0,0,0],"cell":5}'

# Đánh tiếp (dùng board + token từ response trước)
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d '{"board":[0,0,0,0,1,0,0,2,0],"token":"a3f9...","cell":1}'

# Test gian lận — token sai → expect 400
curl -s -X POST http://localhost:8080/api/move \
  -H "Content-Type: application/json" \
  -d '{"board":[1,1,1,0,1,0,0,0,0],"token":"fake","cell":2}'

# Multi-client: 2 game hoàn toàn độc lập, không cần gameId
curl -X POST http://localhost:8080/api/move -d '{"board":[0,0,0,0,0,0,0,0,0],"cell":1}' -H "Content-Type: application/json"
curl -X POST http://localhost:8080/api/move -d '{"board":[0,0,0,0,0,0,0,0,0],"cell":9}' -H "Content-Type: application/json"
```
