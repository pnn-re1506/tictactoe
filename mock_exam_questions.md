# Mock Exam Questions — Programming Exercise Final

> Based on your `week9_single` (TCP socket) and `week12_http` (HTTP/JDK HttpServer) branches.

---

## PART A — Architecture & Flow (Cả hai branch)

### Q1. Giải thích luồng hoạt động của một lượt chơi (one move) trong `week9_single`.
**Expected:** Client tạo Board local → HumanPlayer chọn ô → Client mở Socket mới tới Server (port 12345) → gửi `"MOVE <boardLine> <cell>"` → Server parse request trong `handleMove()` → validate move → place HUMAN → check win/draw → ComputerPlayer đánh → check win/draw → trả `"RESULT <status> <boardLine>"` → Client parse response → cập nhật Board local → đóng socket.

### Q2. Giải thích luồng hoạt động của một lượt chơi trong `week12_http`.
**Expected:** Tương tự Q1, nhưng: Client dùng `HttpClient` gửi HTTP POST tới `http://localhost:9090/move` với JSON body `{"board":"...","move":N}`. Server dùng `com.sun.net.httpserver.HttpServer`, `MoveHandler` implements `HttpHandler`. Response là JSON `{"status":"...","board":"..."}`. Sử dụng Gson để serialize/deserialize.

### Q3. Server của bạn có phải "stateless" không? Giải thích.
**Expected:** Có. Server không lưu trạng thái game giữa các request. Mỗi request, client gửi kèm `board` hiện tại. Server chỉ xử lý dựa trên board nhận được, không có session hay game ID. Đây là cách giữ server stateless — toàn bộ state nằm ở client.

### Q4. Tại sao server stateless lại cho phép multi-user?
**Expected:** Vì server không giữ state riêng cho từng user, bất kỳ client nào gửi request với board hợp lệ đều được xử lý. Nhiều client chạy đồng thời, mỗi client giữ board riêng, server xử lý từng request độc lập. Single-threaded nhưng vẫn multi-user vì mỗi request được xử lý tuần tự và nhanh.

### Q5. So sánh giao thức truyền thông giữa `week9_single` và `week12_http`.

| Tiêu chí | week9_single | week12_http |
|---|---|---|
| Protocol | Raw TCP Socket | HTTP (trên TCP) |
| Format | Plain text `"MOVE board cell"` | JSON `{"board":"...","move":N}` |
| Connection | Mỗi move mở Socket mới | Mỗi move gửi HTTP POST mới |
| Parsing | `String.split(" ")` | Gson `fromJson/toJson` |
| Server class | `ServerSocket` + `Socket` | `HttpServer` + `HttpHandler` |
| DTO | Không có | `MoveRequest`, `MoveResponse` |

---

## PART B — Code Comprehension

### Q6. Trong `Board.java`, method `toLine()` và `fromLine()` làm gì? Tại sao cần chúng?
**Expected:** `toLine()` chuyển mảng `cells[9]` thành chuỗi 9 ký tự (vd: `"120010000"`). `fromLine()` làm ngược lại. Cần thiết vì board phải được serialize để gửi qua mạng (client → server và ngược lại). Đây là cách biểu diễn state của board dưới dạng truyền tải được.

### Q7. Tại sao `ComputerPlayer.chooseCell()` chỉ gọi `board.firstEmptyCell()`? Nhược điểm?
**Expected:** Đây là chiến thuật đơn giản nhất — chọn ô trống đầu tiên. Nhược điểm: máy tính rất yếu, không biết chặn, không biết tấn công, dễ thua. Cải thiện: dùng Minimax algorithm hoặc ít nhất check win/block trước.

### Q8. Trong `week9_single` Server.java, tại sao dùng `try (Socket socket = serverSocket.accept())` bên trong vòng `while(true)`?
**Expected:** Mỗi lần lặp accept một connection mới, xử lý xong thì đóng socket (try-with-resources). Vòng lặp vô hạn giúp server liên tục lắng nghe. Đây là mô hình single-threaded iterative server — xử lý tuần tự từng request.

### Q9. Trong `week12_http`, dòng `server.setExecutor(null)` có ý nghĩa gì?
**Expected:** Khi set executor = null, HttpServer sử dụng default executor, tức là xử lý request trên thread gọi (single-threaded). Nếu muốn multi-threaded, có thể truyền `Executors.newFixedThreadPool(N)`.

### Q10. Trong `week12_http` Client.java, tại sao `HttpClient` là `static final` còn trong `week9_single` thì mỗi lần gửi tạo Socket mới?
**Expected:** `HttpClient` có thể tái sử dụng cho nhiều request (connection pooling, HTTP keep-alive). Socket trong week9 là raw TCP — mỗi connection riêng biệt, phải tạo mới mỗi lần vì protocol đơn giản, không có multiplexing.

---

## PART C — Feature Implementation

### Q11. Hiện tại Human đi trước. Làm sao để Computer đi trước?
**Expected — week9_single:**
- Trong `Client.java`: trước vòng `while(true)`, gửi một request đặc biệt (vd: `"MOVE 000000000 0"` hoặc tạo command mới `"START"`) để server cho computer đi trước.
- Hoặc: Client gửi board trống với move=0, server nhận ra và chỉ cho computer đi mà không cần human move.
- Server `handleMove()`: thêm logic nếu move==0 thì chỉ cho computer đi và trả về board mới.

### Q12. Làm sao implement "Hỏi user có muốn chơi lại không" (play again)?
**Expected — week9_single:**
- Trong `Client.java`: sau khi in kết quả (`printResult`), thay vì `return`, thêm vòng lặp hỏi user "Play again? (y/n)".
- Nếu yes: reset `board = new Board()` và tiếp tục vòng while bên ngoài.
- Wrap toàn bộ game loop trong một vòng lặp ngoài (outer loop).
- Server không cần thay đổi gì (vì stateless).

**Expected — week12_http (browser):**
- Frontend đã có nút "New Game" với `startGame()` → reset `currentBoard` và `gameOver`.
- Server không cần thay đổi.

### Q13. Làm sao hiển thị X cho Computer, O cho Player thay vì số 1, 2?
**Expected — week9_single:**
- Sửa `Board.printMatrix()`: thay `System.out.print(cells[...])` bằng logic map: `0→"."`, `1→"O"` (Player), `2→"X"` (Computer). Hoặc tạo method `toSymbol(int)`.
- Sửa `Client.java` dòng thông báo: `"You are O. Computer is X."`
- Server không cần thay đổi (chỉ xử lý logic, không hiển thị).

### Q14. Implement deadline 10 giây cho mỗi nước đi (move timeout).
**Expected — week9_single Client-side:**
- Thay vì `scanner.nextLine()` blocking vĩnh viễn, dùng một cách đếm thời gian:
  - **Cách 1 (đơn giản, không thread):** Ghi nhận `System.currentTimeMillis()` trước khi đọc input. Dùng `System.in.available()` trong vòng lặp polling để check xem có input chưa, kết hợp check timeout. Nhưng cách này phức tạp và không hoàn toàn chính xác.
  - **Cách 2 (với thread — nếu được phép):** Dùng `ExecutorService` submit task đọc input, rồi `future.get(10, TimeUnit.SECONDS)`. Nếu `TimeoutException` → hết giờ.
- Nếu hết 10s → client tự động gửi move mặc định hoặc thua lượt.

**Server-side approach (stateless nhưng verify):**
- Server gửi kèm timestamp trong response. Client gửi lại timestamp. Server check nếu `now - timestamp > 10s` → reject move.
- Cần bảo vệ timestamp bằng HMAC để client không giả mạo.

### Q15. Implement tính năng cho phép user chọn ký hiệu (X hoặc O) trước khi chơi.
**Expected:**
- Client: hỏi user chọn X hay O trước game loop.
- Nếu user chọn X → user đi trước (giữ nguyên).
- Nếu user chọn O → computer đi trước (kết hợp Q11).
- Thêm field vào request (hoặc tạo protocol mới) để server biết ai là ai.
- **Hoặc đơn giản:** chỉ thay đổi hiển thị ở client, logic `HUMAN=1, COMPUTER=2` giữ nguyên.

### Q16. Làm sao thêm chức năng "undo last move"?
**Expected:**
- Client: lưu history list các board state trước đó (vd: `List<String> history`).
- Khi user nhập "u" (undo): lấy board từ history (pop 1 cái nếu chỉ undo human, pop 2 nếu undo cả computer response).
- Server: không cần thay đổi (stateless, client gửi board nào thì xử lý board đó).
- **Lưu ý bảo mật:** Server nên validate board gửi đến có hợp lệ không (đúng số quân, đúng lượt).

---

## PART D — Security & Anti-Cheating

### Q17. Giải thích cách cheating có thể xảy ra với server hiện tại.
**Expected:**
1. **Gửi board giả mạo:** Client gửi board mà player chưa thực sự chơi đến (vd: board gần thắng dù mới bắt đầu).
2. **Bỏ qua nước đi của computer:** Client nhận board có nước computer nhưng gửi lại board cũ (trước khi computer đi).
3. **Thay đổi quân:** Client gửi board có ô computer bị xóa hoặc thêm quân human.
4. Server chỉ validate `isValidMove()` trên board nhận được, không kiểm tra board có phải kết quả của chuỗi nước đi hợp lệ hay không.

### Q18. Đề xuất giải pháp chống cheating, giữ server stateless. Nêu rõ cần thay đổi ở đâu.
**Expected — HMAC approach:**
1. Server giữ một `SECRET_KEY` (chỉ server biết).
2. Mỗi response, server tính `HMAC(secretKey, boardLine)` và gửi kèm signature cho client.
3. Client gửi lại board + signature trong request tiếp theo.
4. Server verify: tính lại HMAC từ board nhận được, so sánh với signature. Nếu không khớp → reject.

**Thay đổi cụ thể (week9_single):**
- `Server.handleMove()`: thêm param signature, verify trước khi xử lý. Response thêm HMAC của board mới.
- Protocol: `"MOVE <board> <cell> <signature>"` → `"RESULT <status> <board> <newSignature>"`
- `Client.java`: lưu signature từ response, gửi kèm trong request tiếp theo. Request đầu tiên: board `"000000000"` không cần signature (hoặc dùng HMAC của board trống).

**Thay đổi cụ thể (week12_http):**
- `MoveRequest`: thêm field `String signature`.
- `MoveResponse`: thêm field `String signature`.
- `Server.handleMove()`: verify + generate HMAC tương tự.

### Q19. HMAC có ngăn được gian lận thời gian (time cheating) không? Giải pháp?
**Expected:** Không. HMAC chỉ verify board integrity. Để chống time cheating:
- Thêm timestamp vào dữ liệu được ký: `HMAC(key, board + "|" + timestamp)`.
- Server gửi kèm timestamp trong response.
- Client gửi lại timestamp + signature.
- Server verify: check HMAC hợp lệ VÀ `currentTime - timestamp <= 10s`.
- Client không thể giả mạo timestamp vì nó được bảo vệ bởi HMAC.

### Q20. Ngoài HMAC, còn giải pháp nào khác để chống cheating?
**Expected:**
1. **Server giữ state (stateful):** Lưu game session trên server (Map<gameId, Board>). Client chỉ gửi move, không gửi board. → Mất tính stateless.
2. **Move counter:** Server yêu cầu client gửi kèm move number. Server verify: board phải có đúng N quân human và N (hoặc N-1) quân computer. → Phần nào hạn chế nhưng không hoàn toàn an toàn.
3. **Board history hash chain:** Mỗi response kèm hash của toàn bộ history. Client gửi lại hash. Server verify chain. → Phức tạp hơn HMAC.
4. **Validate board consistency:** Server kiểm tra số quân Human = số quân Computer hoặc +1 (vì human đi trước). → Thêm vào `handleMove()`.

---

## PART E — Refactoring & Upgrade

### Q21. So sánh `week9_single` Server vs `week12_http` Server. Refactor được gì?
**Expected:**
- Logic `handleMove()` gần như giống nhau → trích xuất thành class `GameLogic` riêng, cả hai Server cùng gọi.
- Week9 parse text, week12 parse JSON → tách parsing logic ra khỏi game logic.
- Tạo interface `GameEngine` với method `MoveResponse processMove(String board, int move)`.

### Q22. Tại sao `HumanPlayer.java` tồn tại ở `week12_http` nhưng chỉ Client dùng? Có nên refactor?
**Expected:** `HumanPlayer` chỉ phục vụ terminal input. Trong week12_http, nó vẫn được Client (terminal) dùng. Nếu chuyển sang browser-based (week14), `HumanPlayer` không cần thiết nữa vì input qua browser. Có thể:
- Giữ cho backward compatibility với terminal client.
- Xóa nếu chỉ dùng browser client.
- Tách thành package `client` vs `server` rõ ràng.

### Q23. Làm sao chuyển từ `week12_http` (JDK HttpServer) sang Servlet-based (Tomcat)?
**Expected:**
- Thay `HttpServer` + `HttpHandler` bằng `HttpServlet` (extend `HttpServlet`).
- `MoveHandler.handle()` → `doPost(HttpServletRequest, HttpServletResponse)`.
- Thêm `doOptions()` cho CORS nếu cần.
- Dùng `@WebServlet("/move")` annotation.
- Thêm dependency `jakarta.servlet-api` vào `pom.xml`.
- Deploy bằng WAR file trên Tomcat thay vì chạy main class.
- `handleMove()` logic giữ nguyên 100%.

### Q24. Nếu muốn thêm AI mạnh hơn (Minimax), thay đổi ở đâu?
**Expected:**
- Chỉ sửa `ComputerPlayer.chooseCell(Board board)`.
- Implement thuật toán Minimax: duyệt tất cả nước đi có thể, đánh giá hệ quả, chọn nước tối ưu.
- Không cần thay đổi Client, Server, Board, hay protocol.
- Đây là lợi ích của thiết kế tách biệt (separation of concerns).

### Q25. Refactor để hỗ trợ board NxN (không chỉ 3x3)?
**Expected:**
- `Board.java`: thay `cells[9]` bằng `cells[n*n]`, thay hardcoded `winLines` bằng generate động, sửa `isFull()`, `firstEmptyCell()`, `isValidMove()` dùng `n*n` thay vì 9.
- `fromLine()`/`toLine()`: thay check `length() != 9` thành `!= n*n`.
- `printMatrix()`: dùng `n` thay vì 3.
- `HumanPlayer`: sửa prompt `"1-9"` thành `"1-N"`.
- Client/Server protocol: có thể thêm field `size` vào request.

---

## PART F — Design & Comparison (Câu hỏi kiến trúc tổng quát)

### Q26. Đề xuất các giải pháp khác nhau để xây dựng ứng dụng TicTacToe online multi-user.

**Solution 1: Stateless HTTP (như code hiện tại)**
- Arch: Client giữ state, server xử lý từng request độc lập.
- Tech: Java HttpServer/Servlet + JSON.
- ✅ Đơn giản, dễ scale (load balancer), server nhẹ.
- ❌ Khó chống cheating (client kiểm soát state), không hỗ trợ real-time.

**Solution 2: Stateful Server with Sessions**
- Arch: Server giữ game state trong `Map<sessionId, Board>`. Client chỉ gửi move + sessionId.
- Tech: Java Servlet + HttpSession hoặc custom session map.
- ✅ Chống cheating tốt (server kiểm soát state), đơn giản cho client.
- ❌ Server tốn memory, khó scale horizontally (cần sticky sessions hoặc shared state).

**Solution 3: WebSocket Real-time**
- Arch: Persistent connection giữa client-server. Server push update tới client.
- Tech: Java WebSocket API (`javax.websocket`) + JS WebSocket.
- ✅ Real-time, low latency, server có thể push timeout warnings.
- ❌ Phức tạp hơn, stateful connection, khó scale.

**Solution 4: Multi-threaded Server**
- Arch: Mỗi client connection một thread riêng.
- Tech: `ExecutorService` hoặc `Thread` per connection.
- ✅ Xử lý nhiều client đồng thời, responsive hơn.
- ❌ Thread management phức tạp, resource intensive, race conditions.

**Solution 5: Microservices / Cloud**
- Arch: Game logic as REST API, state in Redis/DB, frontend SPA.
- Tech: Spring Boot + Redis + React/Vue.
- ✅ Highly scalable, professional, cloud-ready.
- ❌ Over-engineering cho TicTacToe, phức tạp triển khai.

### Q27. Tại sao dùng single-threaded mà vẫn phục vụ được multi-user? Khi nào cần multi-threaded?
**Expected:**
- Single-threaded hoạt động vì mỗi request xử lý rất nhanh (tính toán TicTacToe nhẹ). Client gửi request → server xử lý → trả về → accept client tiếp theo.
- Cần multi-threaded khi: xử lý mỗi request tốn thời gian (AI phức tạp, DB query), cần giữ persistent connection (WebSocket), hoặc cần đảm bảo low latency khi có nhiều user đồng thời.

### Q28. So sánh terminal-based client vs browser-based client.

| Tiêu chí | Terminal (week9/12) | Browser (week14) |
|---|---|---|
| UI | Text-based, `printMatrix()` | HTML table + CSS |
| Input | `Scanner` + keyboard | Mouse click `onclick` |
| Networking | Java Socket / HttpClient | JS `fetch()` API |
| Deployment | Cần cài Java trên client | Chỉ cần browser |
| UX | Kém, khó dùng | Tốt hơn, trực quan |
| CORS | Không cần | Cần `Access-Control-*` headers |

### Q29. Nếu muốn Human vs Human (2 người chơi với nhau), thay đổi gì?
**Expected:**
- **Stateful approach:** Server quản lý game room. Player 1 tạo game → nhận gameId. Player 2 join game bằng gameId. Server lưu `Map<gameId, GameState>` (board + whose turn).
- Thêm API: `POST /create` → gameId, `POST /join/{gameId}`, `POST /move/{gameId}`.
- Thay `ComputerPlayer` bằng logic chờ nước đi từ player kia.
- **Hoặc WebSocket:** Dùng persistent connection cho cả 2 player, server relay moves.
- Board.java: thay `HUMAN=1, COMPUTER=2` bằng `PLAYER1=1, PLAYER2=2`.
- Xóa `ComputerPlayer`, thêm logic turn management.

### Q30. Explain CORS. Tại sao week9/week12 không cần nhưng week14 (browser) cần?
**Expected:**
- CORS (Cross-Origin Resource Sharing): browser security policy ngăn JS gọi API từ domain khác.
- week9/12: Client là Java app, không chạy trong browser → không bị CORS restriction.
- week14: `index.html` mở từ `file://` hoặc server khác gọi API tới `localhost:9090` → khác origin → browser block. Cần server thêm header `Access-Control-Allow-Origin: *`.
- `doOptions()` xử lý preflight request (browser gửi OPTIONS trước POST).

---

## PART G — Câu hỏi tổng hợp nâng cao

### Q31. Thiết kế hệ thống chống cheating toàn diện, giữ server stateless, có deadline, có anti-replay.
**Expected approach:**
1. Server giữ `SECRET_KEY`.
2. Response chứa: `board`, `timestamp`, `moveCount`, `signature = HMAC(key, board|timestamp|moveCount)`.
3. Client gửi lại: `board`, `move`, `timestamp`, `moveCount`, `signature`.
4. Server verify:
   - HMAC hợp lệ → board chưa bị tamper.
   - `currentTime - timestamp <= 10s` → chưa hết deadline.
   - `moveCount` tăng dần → chống replay attack.
   - Board consistency: `countHuman == countComputer` hoặc `+1`.

### Q32. Nếu giáo yêu cầu: "Thêm tính năng spectator (người xem) vào game", bạn sẽ làm gì?
**Expected:**
- **Stateless (polling):** Spectator client gọi `GET /game/{id}/board` mỗi vài giây để lấy board mới nhất. Server cần lưu game state (hoặc client player gửi board lên endpoint riêng).
- **WebSocket:** Spectator connect WebSocket, server broadcast board update mỗi khi có move.
- **Changes:** Thêm endpoint mới, thêm game ID concept, server cần lưu ít nhất board hiện tại per game.

### Q33. Giải thích tại sao `handleMove()` trong `Server.java` (week9) và `MoveServlet.java` (week14) gần như giống nhau. Đề xuất refactor.
**Expected:**
- Cả hai đều: parse board → validate move → place human → check win → computer move → check win → return response.
- **Refactor:** Tạo class `GameEngine` với static method `MoveResponse process(String board, int move)`. Cả `Server.handleMove()` lẫn `MoveServlet.handleMove()` đều gọi `GameEngine.process()`.
- Lợi ích: DRY principle, dễ test, dễ maintain, thay đổi logic game chỉ một chỗ.

### Q34. Code hiện tại dùng `Board.HUMAN = 1` và `Board.COMPUTER = 2`. Nếu dùng enum thì sửa ở đâu?
**Expected:**
- Tạo `enum Mark { EMPTY, HUMAN, COMPUTER }` trong Board.java hoặc file riêng.
- `Board.java`: đổi `int[] cells` → `Mark[] cells`, đổi tất cả method dùng int mark → Mark mark.
- `fromLine()`/`toLine()`: map `'0'→EMPTY`, `'1'→HUMAN`, `'2'→COMPUTER`.
- `ComputerPlayer`/Server: thay `Board.HUMAN` → `Mark.HUMAN`.
- Lợi ích: type-safe, tránh magic numbers, code rõ ràng hơn.
.