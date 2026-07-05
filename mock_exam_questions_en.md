# Mock Exam Questions — Programming Exercise Final (English)

> Based on your `week9_single` (TCP socket) and `week12_http` (HTTP/JDK HttpServer) branches.

---

## PART A — Architecture & Flow (Both branches)

### Q1. Explain the execution flow of a single move in `week9_single`.
**Expected:** Client creates a local Board → HumanPlayer picks a cell → Client opens a new Socket to Server (port 12345) → sends `"MOVE <boardLine> <cell>"` → Server parses the request in `handleMove()` → validates the move → places HUMAN mark → checks win/draw → ComputerPlayer plays → checks win/draw → returns `"RESULT <status> <boardLine>"` → Client parses the response → updates the local Board → socket is closed.

### Q2. Explain the execution flow of a single move in `week12_http`.
**Expected:** Similar to Q1, but: Client uses `HttpClient` to send an HTTP POST to `http://localhost:9090/move` with a JSON body `{"board":"...","move":N}`. Server uses `com.sun.net.httpserver.HttpServer`; `MoveHandler` implements `HttpHandler`. The response is JSON `{"status":"...","board":"..."}`. Gson is used for serialization/deserialization.

### Q3. Is your server "stateless"? Explain.
**Expected:** Yes. The server does not store any game state between requests. Each request carries the current `board` from the client. The server processes each request solely based on the received board — no sessions, no game IDs. This is how the server remains stateless: all state resides on the client side.

### Q4. Why does a stateless server naturally support multi-user?
**Expected:** Because the server holds no per-user state, any client that sends a request with a valid board is served. Multiple clients run simultaneously, each maintaining their own board. The server processes each request independently. Even though it is single-threaded, it is still multi-user because each request is handled sequentially and quickly.

### Q5. Compare the communication protocol between `week9_single` and `week12_http`.

| Criteria | week9_single | week12_http |
|---|---|---|
| Protocol | Raw TCP Socket | HTTP (over TCP) |
| Format | Plain text `"MOVE board cell"` | JSON `{"board":"...","move":N}` |
| Connection | New Socket per move | New HTTP POST per move |
| Parsing | `String.split(" ")` | Gson `fromJson/toJson` |
| Server class | `ServerSocket` + `Socket` | `HttpServer` + `HttpHandler` |
| DTOs | None | `MoveRequest`, `MoveResponse` |

---

## PART B — Code Comprehension

### Q6. In `Board.java`, what do `toLine()` and `fromLine()` do? Why are they needed?
**Expected:** `toLine()` converts the `cells[9]` array into a 9-character string (e.g., `"120010000"`). `fromLine()` does the reverse. They are needed because the board must be serialized to be transmitted over the network (client → server and back). This is the transportable representation of the board state.

### Q7. Why does `ComputerPlayer.chooseCell()` only call `board.firstEmptyCell()`? What are the drawbacks?
**Expected:** This is the simplest possible strategy — pick the first empty cell. Drawbacks: the computer is extremely weak, cannot block, cannot attack, and easily loses. Improvement: use the Minimax algorithm, or at the very least check for winning/blocking moves first.

### Q8. In `week9_single` Server.java, why is `try (Socket socket = serverSocket.accept())` inside a `while(true)` loop?
**Expected:** Each iteration accepts a new connection, processes it, then closes the socket (via try-with-resources). The infinite loop keeps the server listening continuously. This is the single-threaded iterative server model — requests are handled sequentially, one at a time.

### Q9. In `week12_http`, what does the line `server.setExecutor(null)` mean?
**Expected:** When the executor is set to `null`, the HttpServer uses the default executor, meaning it handles requests on the calling thread (single-threaded). To make it multi-threaded, you could pass `Executors.newFixedThreadPool(N)`.

### Q10. In `week12_http` Client.java, why is `HttpClient` declared as `static final`, while in `week9_single` a new Socket is created for every request?
**Expected:** `HttpClient` can be reused across multiple requests (connection pooling, HTTP keep-alive). The Socket in week9 is raw TCP — each connection is separate and must be created fresh because the protocol is simple with no multiplexing.

---

## PART C — Feature Implementation

### Q11. Currently, the Human moves first. How would you make the Computer move first?
**Expected — week9_single:**
- In `Client.java`: before the `while(true)` loop, send a special request (e.g., `"MOVE 000000000 0"` or create a new command `"START"`) so the server lets the computer play first.
- Or: Client sends an empty board with move=0; the server recognizes this and only lets the computer play, returning the updated board.
- In `Server.handleMove()`: add logic — if move==0, only let the computer play and return the new board.

### Q12. How would you implement "Ask the user if they want to play another game" (play again)?
**Expected — week9_single:**
- In `Client.java`: after printing the result (`printResult`), instead of `return`, add a loop asking the user "Play again? (y/n)".
- If yes: reset `board = new Board()` and continue the outer while loop.
- Wrap the entire game loop inside an outer loop.
- The server needs no changes (it is stateless).

**Expected — week12_http (browser):**
- The frontend already has a "New Game" button with `startGame()` → resets `currentBoard` and `gameOver`.
- The server needs no changes.

### Q13. How would you display X for Computer and O for Player instead of the numbers 1 and 2?
**Expected — week9_single:**
- Modify `Board.printMatrix()`: replace `System.out.print(cells[...])` with mapping logic: `0→"."`, `1→"O"` (Player), `2→"X"` (Computer). Or create a helper method `toSymbol(int)`.
- Update `Client.java` message: `"You are O. Computer is X."`.
- The server needs no changes (it only handles logic, not display).

### Q14. Implement a 10-second deadline for each move (move timeout).
**Expected — Client-side (week9_single):**
- Instead of `scanner.nextLine()` blocking indefinitely, implement a timing mechanism:
  - **Approach 1 (simple, no threads):** Record `System.currentTimeMillis()` before reading input. Use `System.in.available()` in a polling loop to check for input while monitoring timeout. However, this is complex and not fully reliable.
  - **Approach 2 (with threads — if allowed):** Use `ExecutorService` to submit an input-reading task, then call `future.get(10, TimeUnit.SECONDS)`. If `TimeoutException` → time is up.
- If 10s elapse → client auto-sends a default move or forfeits the turn.

**Server-side approach (stateless but verifiable):**
- Server includes a timestamp in the response. Client sends back the timestamp. Server checks if `now - timestamp > 10s` → reject the move.
- The timestamp must be protected with HMAC so the client cannot forge it.

### Q15. Implement a feature that lets the user choose their symbol (X or O) before the game starts.
**Expected:**
- Client: ask the user to choose X or O before the game loop.
- If user picks X → user moves first (keep current behavior).
- If user picks O → computer moves first (combine with Q11).
- Add a field to the request (or design a new protocol message) so the server knows who is who.
- **Or simply:** only change the display on the client side; the internal logic `HUMAN=1, COMPUTER=2` remains unchanged.

### Q16. How would you add an "undo last move" feature?
**Expected:**
- Client: maintain a history list of previous board states (e.g., `List<String> history`).
- When the user types "u" (undo): retrieve the board from history (pop 1 entry to undo only the human move, pop 2 to undo the computer's response as well).
- Server: no changes needed (stateless — it processes whatever board the client sends).
- **Security note:** The server should validate that the received board is consistent (correct number of marks, correct turn order).

---

## PART D — Security & Anti-Cheating

### Q17. Explain how cheating can occur with the current server.
**Expected:**
1. **Sending a forged board:** The client sends a board state that was never actually reached through gameplay (e.g., a nearly-won board despite the game just starting).
2. **Ignoring the computer's move:** The client receives the board with the computer's move but sends back the old board (before the computer played).
3. **Altering marks:** The client sends a board where the computer's marks have been erased or extra human marks have been added.
4. The server only validates `isValidMove()` on the received board; it does not verify whether the board is the result of a legitimate sequence of moves.

### Q18. Propose a solution to prevent cheating while keeping the server stateless. Specify exactly where changes are needed.
**Expected — HMAC approach:**
1. The server holds a `SECRET_KEY` (known only to the server).
2. In each response, the server computes `HMAC(secretKey, boardLine)` and sends the signature to the client.
3. The client sends back the board + signature in the next request.
4. The server verifies: recomputes the HMAC from the received board and compares it with the signature. If they don't match → reject.

**Specific changes (week9_single):**
- `Server.handleMove()`: add a signature parameter, verify it before processing. The response includes the HMAC of the new board.
- Protocol: `"MOVE <board> <cell> <signature>"` → `"RESULT <status> <board> <newSignature>"`
- `Client.java`: store the signature from the response, include it in the next request. First request: board `"000000000"` needs no signature (or use the HMAC of the empty board).

**Specific changes (week12_http):**
- `MoveRequest`: add field `String signature`.
- `MoveResponse`: add field `String signature`.
- `Server.handleMove()`: verify + generate HMAC similarly.

### Q19. Does HMAC prevent time-based cheating (taking too long)? What is the solution?
**Expected:** No. HMAC only verifies board integrity. To prevent time cheating:
- Include a timestamp in the signed data: `HMAC(key, board + "|" + timestamp)`.
- The server includes the timestamp in the response.
- The client sends back the timestamp + signature.
- The server verifies: check that the HMAC is valid AND `currentTime - timestamp <= 10s`.
- The client cannot forge the timestamp because it is protected by the HMAC.

### Q20. Besides HMAC, what other solutions exist to prevent cheating?
**Expected:**
1. **Stateful server:** Store game sessions on the server (`Map<gameId, Board>`). The client only sends the move, not the board. → Loses statelessness.
2. **Move counter:** The server requires the client to send a move number. The server verifies: the board must have exactly N human marks and N (or N-1) computer marks. → Partially effective but not fully secure.
3. **Board history hash chain:** Each response includes a hash of the entire history. The client sends back the hash. The server verifies the chain. → More complex than HMAC.
4. **Validate board consistency:** The server checks that `countHuman == countComputer` or `countHuman == countComputer + 1` (since human moves first). → Add this to `handleMove()`.

---

## PART E — Refactoring & Upgrade

### Q21. Compare the `week9_single` Server vs the `week12_http` Server. What can be refactored?
**Expected:**
- The `handleMove()` logic is nearly identical → extract it into a separate `GameLogic` class that both servers call.
- Week9 parses text, week12 parses JSON → separate parsing logic from game logic.
- Create an interface `GameEngine` with a method `MoveResponse processMove(String board, int move)`.

### Q22. `HumanPlayer.java` exists in `week12_http` but only the Client uses it. Should it be refactored?
**Expected:** `HumanPlayer` only serves terminal input. In week12_http, it is still used by the terminal Client. If switching to a browser-based client (week14), `HumanPlayer` is no longer needed because input comes from the browser. Options:
- Keep it for backward compatibility with the terminal client.
- Remove it if only the browser client is used.
- Separate into `client` vs `server` packages for clarity.

### Q23. How would you migrate from `week12_http` (JDK HttpServer) to Servlet-based (Tomcat)?
**Expected:**
- Replace `HttpServer` + `HttpHandler` with `HttpServlet` (extend `HttpServlet`).
- `MoveHandler.handle()` → `doPost(HttpServletRequest, HttpServletResponse)`.
- Add `doOptions()` for CORS if needed.
- Use `@WebServlet("/move")` annotation.
- Add the `jakarta.servlet-api` dependency to `pom.xml`.
- Deploy as a WAR file on Tomcat instead of running a main class.
- The `handleMove()` logic remains 100% unchanged.

### Q24. If you want to add a stronger AI (Minimax), where would you make changes?
**Expected:**
- Only modify `ComputerPlayer.chooseCell(Board board)`.
- Implement the Minimax algorithm: explore all possible moves, evaluate outcomes, choose the optimal move.
- No changes needed to Client, Server, Board, or the protocol.
- This is the benefit of separation of concerns in the design.

### Q25. Refactor to support an NxN board (not just 3x3)?
**Expected:**
- `Board.java`: replace `cells[9]` with `cells[n*n]`, replace the hardcoded `winLines` with dynamically generated ones, update `isFull()`, `firstEmptyCell()`, `isValidMove()` to use `n*n` instead of 9.
- `fromLine()`/`toLine()`: change the check `length() != 9` to `!= n*n`.
- `printMatrix()`: use `n` instead of 3.
- `HumanPlayer`: update the prompt from `"1-9"` to `"1-N"`.
- Client/Server protocol: optionally add a `size` field to the request.

---

## PART F — Design & Comparison (General Architecture Questions)

### Q26. Propose different solutions for building an online multi-user TicTacToe application.

**Solution 1: Stateless HTTP (like the current code)**
- Architecture: Client holds state; server processes each request independently.
- Technology: Java HttpServer/Servlet + JSON.
- ✅ Simple, easy to scale (load balancer), lightweight server.
- ❌ Hard to prevent cheating (client controls state), no real-time support.

**Solution 2: Stateful Server with Sessions**
- Architecture: Server stores game state in `Map<sessionId, Board>`. Client only sends move + sessionId.
- Technology: Java Servlet + HttpSession or custom session map.
- ✅ Strong anti-cheating (server controls state), simpler client.
- ❌ Server consumes more memory, hard to scale horizontally (requires sticky sessions or shared state).

**Solution 3: WebSocket Real-time**
- Architecture: Persistent connection between client and server. Server pushes updates to the client.
- Technology: Java WebSocket API (`javax.websocket`) + JS WebSocket.
- ✅ Real-time, low latency, server can push timeout warnings.
- ❌ More complex, stateful connections, harder to scale.

**Solution 4: Multi-threaded Server**
- Architecture: Each client connection gets its own thread.
- Technology: `ExecutorService` or `Thread` per connection.
- ✅ Handles many clients concurrently, more responsive.
- ❌ Complex thread management, resource intensive, race conditions.

**Solution 5: Microservices / Cloud**
- Architecture: Game logic as REST API, state in Redis/DB, frontend as SPA.
- Technology: Spring Boot + Redis + React/Vue.
- ✅ Highly scalable, professional, cloud-ready.
- ❌ Over-engineering for TicTacToe, complex deployment.

### Q27. Why can a single-threaded server still serve multiple users? When would you need multi-threading?
**Expected:**
- Single-threaded works because each request is processed very quickly (TicTacToe computation is lightweight). Client sends request → server processes → responds → accepts next client.
- Multi-threading is needed when: processing each request is expensive (complex AI, DB queries), persistent connections are required (WebSocket), or low latency must be guaranteed under high concurrent load.

### Q28. Compare terminal-based client vs browser-based client.

| Criteria | Terminal (week9/12) | Browser (week14) |
|---|---|---|
| UI | Text-based, `printMatrix()` | HTML table + CSS |
| Input | `Scanner` + keyboard | Mouse click via `onclick` |
| Networking | Java Socket / HttpClient | JS `fetch()` API |
| Deployment | Requires Java on client | Only needs a browser |
| UX | Poor, hard to use | Better, intuitive |
| CORS | Not needed | Requires `Access-Control-*` headers |

### Q29. If you wanted to implement Human vs Human (two players against each other), what would change?
**Expected:**
- **Stateful approach:** The server manages game rooms. Player 1 creates a game → receives a gameId. Player 2 joins using the gameId. Server stores `Map<gameId, GameState>` (board + whose turn).
- New APIs: `POST /create` → gameId, `POST /join/{gameId}`, `POST /move/{gameId}`.
- Replace `ComputerPlayer` with logic that waits for the other player's move.
- **Or WebSocket:** Use persistent connections for both players; the server relays moves between them.
- `Board.java`: replace `HUMAN=1, COMPUTER=2` with `PLAYER1=1, PLAYER2=2`.
- Remove `ComputerPlayer`, add turn management logic.

### Q30. Explain CORS. Why is it not needed in week9/week12 but required in week14 (browser)?
**Expected:**
- CORS (Cross-Origin Resource Sharing): a browser security policy that prevents JavaScript from calling APIs on a different origin.
- week9/12: The client is a Java application, not running inside a browser → not subject to CORS restrictions.
- week14: `index.html` opened from `file://` or a different server calls the API on `localhost:9090` → different origin → the browser blocks it. The server must add the header `Access-Control-Allow-Origin: *`.
- `doOptions()` handles the preflight request (the browser sends an OPTIONS request before the actual POST).

---

## PART G — Advanced Synthesis Questions

### Q31. Design a comprehensive anti-cheating system that keeps the server stateless, enforces a deadline, and prevents replay attacks.
**Expected approach:**
1. The server holds a `SECRET_KEY`.
2. Each response contains: `board`, `timestamp`, `moveCount`, `signature = HMAC(key, board|timestamp|moveCount)`.
3. The client sends back: `board`, `move`, `timestamp`, `moveCount`, `signature`.
4. The server verifies:
   - HMAC is valid → the board has not been tampered with.
   - `currentTime - timestamp <= 10s` → the deadline has not expired.
   - `moveCount` is strictly increasing → prevents replay attacks.
   - Board consistency: `countHuman == countComputer` or `countHuman == countComputer + 1`.

### Q32. If the professor asks: "Add a spectator (viewer) feature to the game", what would you do?
**Expected:**
- **Stateless (polling):** The spectator client calls `GET /game/{id}/board` every few seconds to fetch the latest board. The server needs to store game state (or the playing client uploads the board to a separate endpoint).
- **WebSocket:** The spectator connects via WebSocket; the server broadcasts board updates after every move.
- **Changes needed:** Add a new endpoint, introduce a game ID concept, the server needs to store at least the current board per game.

### Q33. Explain why `handleMove()` in `Server.java` (week9) and `MoveServlet.java` (week14) are nearly identical. Propose a refactoring.
**Expected:**
- Both do: parse board → validate move → place human mark → check win → computer moves → check win → return response.
- **Refactoring:** Create a `GameEngine` class with a static method `MoveResponse process(String board, int move)`. Both `Server.handleMove()` and `MoveServlet.handleMove()` call `GameEngine.process()`.
- Benefits: DRY principle, easier to test, easier to maintain, game logic changes in one place only.

### Q34. The current code uses `Board.HUMAN = 1` and `Board.COMPUTER = 2`. If you were to use an enum instead, where would you make changes?
**Expected:**
- Create `enum Mark { EMPTY, HUMAN, COMPUTER }` inside `Board.java` or as a separate file.
- `Board.java`: change `int[] cells` → `Mark[] cells`; update all methods using `int mark` → `Mark mark`.
- `fromLine()`/`toLine()`: map `'0'→EMPTY`, `'1'→HUMAN`, `'2'→COMPUTER`.
- `ComputerPlayer`/Server: replace `Board.HUMAN` → `Mark.HUMAN`.
- Benefits: type-safe, avoids magic numbers, clearer code.
.