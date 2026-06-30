# Refactor: TCP Socket → HTTP + Gson

> **Ngày:** 2026-06-30  
> **Branch:** week9_single  
> **Mục tiêu:** Chuyển giao tiếp Client-Server từ raw TCP Socket (port 12345) sang HTTP với JSON body, dùng thư viện Gson.

---

## 1. Kiến trúc ban đầu (TCP Socket)

- Server lắng nghe `ServerSocket` trên port **12345**
- Mỗi lượt đi: Client mở Socket, gửi 1 dòng text, nhận 1 dòng text, đóng Socket
- **Protocol text thuần:**
  - Request: `MOVE <board9chars> <cell>`  — ví dụ: `MOVE 000000000 5`
  - Response: `RESULT <status> <board9chars>` — ví dụ: `RESULT ongoing 000000005`
- Server stateless: không lưu trạng thái, Client mang toàn bộ board state

---

## 2. Kiến trúc sau refactor (HTTP + Gson)

- Server dùng `com.sun.net.httpserver.HttpServer` (JDK built-in) trên port **9090**
- Endpoint duy nhất: `POST /move`
- Client dùng `java.net.http.HttpClient` (JDK 11+)
- **Protocol JSON:**
  - Request body: `{"board":"000000000","move":5}`
  - Response body: `{"status":"ongoing","board":"000200010"}`

### Sơ đồ luồng

```
Client (terminal)                        Server (HTTP :9090)
─────────────────                        ──────────────────
board.printMatrix()
humanMove = human.chooseCell(board)

MoveRequest → GSON.toJson()
  {"board":"000000000","move":5}

POST /move HTTP/1.1  ─────────────────>  MoveHandler.handle()
Content-Type: application/json           GSON.fromJson() → MoveRequest
                                         handleMove(req) → MoveResponse
                                         GSON.toJson() → JSON string

HTTP/1.1 200 OK      <─────────────────
{"status":"ongoing","board":"000200010"}

GSON.fromJson() → MoveResponse
resp.status, resp.board → update board
```

---

## 3. Câu hỏi & giải đáp trong conversation

### POST vs GET khác gì nhau?

| Đặc điểm | GET | POST |
|---|---|---|
| Dữ liệu đi đâu | URL query string | HTTP body (ẩn) |
| Idempotent | ✅ Có | ❌ Không |
| Cacheable | ✅ Có | ❌ Không |
| Giới hạn dữ liệu | ~8KB (URL) | Không giới hạn |

**→ Dùng POST /move** vì nước đi là *hành động có side-effect*, không idempotent.

---

### Tự parse JSON vs dùng Gson — khác gì?

| Tiêu chí | Tự parse (String) | Dùng Gson |
|---|---|---|
| Dependency | Không cần | Thêm Gson vào pom.xml |
| Code lượng | Nhiều (helper functions) | Ít, gọn |
| Type-safe | ❌ Dễ lỗi typo key | ✅ Compile-time check qua DTO |
| Xử lý lỗi | Tự handle | Gson throw `JsonSyntaxException` |

**→ Chọn Gson** cho code sạch, type-safe hơn.

---

### Fat JAR là gì? Tại sao có 2 JAR?

```
mvn package
  ├─ maven-jar-plugin  → tttbasic-1.0-SNAPSHOT.jar (code của bạn, ~12KB)
  └─ maven-shade-plugin → gộp Gson vào JAR gốc → tttbasic-1.0-SNAPSHOT.jar (~320KB)
                          backup JAR gốc → original-tttbasic-1.0-SNAPSHOT.jar
```

Fat JAR chứa toàn bộ bytecode của Gson bên trong → máy khác chỉ cần Java, không cần cài Gson.

---

### `dependency-reduced-pom.xml` là gì?

- File do maven-shade-plugin **tự sinh ra** sau mỗi lần build
- Mô tả: fat JAR còn phụ thuộc vào dependency nào chưa được gộp vào
- **Không ảnh hưởng gì** nếu tắt (dự án không publish lên Maven repo)
- Tắt bằng: `<createDependencyReducedPom>false</createDependencyReducedPom>`

---

### Mang JAR sang máy khác chạy được không?

**Được**, chỉ cần máy kia có **Java 21+**. Fat JAR đã chứa sẵn Gson bên trong.

```powershell
java -cp tttbasic-1.0-SNAPSHOT.jar vgu.pe2026.ttt.basis.Server
java -cp tttbasic-1.0-SNAPSHOT.jar vgu.pe2026.ttt.basis.Client
```

---

## 4. Các file thay đổi

| File | Loại | Mô tả |
|---|---|---|
| `pom.xml` | Sửa | +Gson 2.11.0, +maven-shade-plugin, +createDependencyReducedPom=false |
| `MoveRequest.java` | **[NEW]** | DTO `{String board, int move}` |
| `MoveResponse.java` | **[NEW]** | DTO `{String status, String board}` |
| `Server.java` | Sửa lớn | `ServerSocket` → `HttpServer`, `MoveHandler implements HttpHandler`, Gson |
| `Client.java` | Sửa vừa | `Socket` → `HttpClient`, Gson, `send()` trả `MoveResponse` |
| `Board.java` | ❌ Không đổi | — |
| `ComputerPlayer.java` | ❌ Không đổi | — |
| `HumanPlayer.java` | ❌ Không đổi | — |

---

## 5. Lệnh chạy

```powershell
# Build
cd tttbasic
mvn clean package -q

# Terminal 1 — Server
java -cp target\tttbasic-1.0-SNAPSHOT.jar vgu.pe2026.ttt.basis.Server
# Output: HTTP Server started on port 9090

# Terminal 2 — Client
java -cp target\tttbasic-1.0-SNAPSHOT.jar vgu.pe2026.ttt.basis.Client
```

## 6. Test bằng PowerShell (không cần Client)

```powershell
# Nước đi hợp lệ
Invoke-RestMethod -Uri "http://localhost:9090/move" -Method POST `
    -Body '{"board":"000000000","move":5}' -ContentType "application/json"
# → status: ongoing, board: 200010000

# Nước đi không hợp lệ
Invoke-RestMethod -Uri "http://localhost:9090/move" -Method POST `
    -Body '{"board":"100000000","move":1}' -ContentType "application/json"
# → status: invalid

# Dùng GET → phải nhận 405
Invoke-RestMethod -Uri "http://localhost:9090/move" -Method GET
# → HTTP 405
```
