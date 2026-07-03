# Walkthrough: Refactor JDK HttpServer → Apache Tomcat 10.1

## Mục tiêu

Chuyển ứng dụng Tic-Tac-Toe từ sử dụng **JDK built-in `HttpServer`** (chạy độc lập) sang **Java Servlet (Jakarta EE)** để deploy trên **Apache Tomcat 10.1**, tái sử dụng tối đa business logic hiện có.

---

## Tổng quan thay đổi

| File | Trạng thái | Ghi chú |
|------|-----------|---------|
| `Board.java` | ✅ Giữ nguyên | Pure business logic |
| `ComputerPlayer.java` | ✅ Giữ nguyên | Pure business logic |
| `MoveRequest.java` | ✅ Giữ nguyên | DTO — không liên quan HTTP |
| `MoveResponse.java` | ✅ Giữ nguyên | DTO — không liên quan HTTP |
| `Server.java` | ❌ Xóa | Thay bằng `MoveServlet.java` |
| `MoveServlet.java` | ➕ Tạo mới | Servlet thay thế `Server.java` |
| `src/main/java/.../index.html` | ❌ Xóa | Sai vị trí |
| `src/main/webapp/index.html` | ➕ Tạo mới | Đúng vị trí trong WAR |
| `pom.xml` | 🔧 Sửa | WAR packaging + Jakarta Servlet API |

---

## Chi tiết từng thay đổi

### 1. `pom.xml`

**Lý do:** Phải đổi output từ `.jar` sang `.war` và thêm Jakarta Servlet API để compile.

```diff
- <!-- không có packaging → mặc định jar -->
+ <packaging>war</packaging>

+ <!-- Jakarta Servlet API (Tomcat 10.1 dùng jakarta.*, không phải javax.*) -->
+ <dependency>
+     <groupId>jakarta.servlet</groupId>
+     <artifactId>jakarta.servlet-api</artifactId>
+     <version>6.0.0</version>
+     <scope>provided</scope>  <!-- Tomcat đã có sẵn, không đóng gói vào WAR -->
+ </dependency>

- <!-- maven-jar-plugin -->     ← xóa, không cần mainClass nữa
- <!-- maven-shade-plugin -->   ← xóa, không cần fat-jar nữa
+ <!-- maven-war-plugin 3.4.0 --> ← thêm để build WAR
```

> **`scope=provided`**: Tomcat 10.1 đã tích hợp sẵn `jakarta.servlet-api`. Nếu đóng gói vào WAR sẽ xung đột, nên đánh dấu `provided` để Maven chỉ dùng khi compile, không nhét vào file `.war`.

---

### 2. `Server.java` → Xóa

File cũ có 2 vai trò:
- **Khởi động server** (`main()`, `HttpServer.create(...)`) — Tomcat đảm nhận hoàn toàn
- **Xử lý request** (`MoveHandler`, `handleMove()`) — chuyển sang `MoveServlet`

Vì Tomcat tự quản lý vòng đời server, `main()` không còn ý nghĩa → xóa toàn bộ file.

---

### 3. `MoveServlet.java` → Tạo mới

**Đây là thay đổi cốt lõi.** Thay vì implement `HttpHandler` (JDK), giờ kế thừa `HttpServlet` (Jakarta EE):

```java
// CŨ — Server.java (JDK HttpServer)
static class MoveHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        // ...
        String body = new String(exchange.getRequestBody().readAllBytes(), ...);
        // ...
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
```

```java
// MỚI — MoveServlet.java (Jakarta Servlet)
@WebServlet("/move")   // ← annotation thay cho server.createContext("/move", ...)
public class MoveServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        String body = new String(req.getInputStream().readAllBytes(), ...);
        // ...
        resp.getWriter().write(GSON.toJson(moveResp));
    }
}
```

**Logic game `handleMove()` được copy nguyên vẹn 100%** — không thay đổi một dòng nào.

**CORS headers bị xóa** vì frontend (`index.html`) giờ được serve cùng Tomcat → same-origin → không cần CORS.

---

### 4. `index.html` — Di chuyển + sửa 1 dòng

**Vị trí cũ (sai):**
```
src/main/java/vgu/pe2026/ttt/basis/index.html
```
File nằm trong thư mục Java source — Maven **không** đóng gói vào WAR.

**Vị trí mới (đúng):**
```
src/main/webapp/index.html
```
`src/main/webapp/` là thư mục chuẩn của Maven WAR plugin — mọi file ở đây được đưa thẳng vào root của WAR.

**Sửa duy nhất trong JS (1 dòng):**
```diff
- fetch("http://localhost:9090/move", {   // hardcode host:port → phụ thuộc môi trường
+ fetch("move", {                         // relative URL → tự động đúng trên mọi server
```

---

## Cấu trúc thư mục sau refactor

```
tttbasic/
├── pom.xml                              ← packaging: war
└── src/
    └── main/
        ├── java/
        │   └── vgu/pe2026/ttt/basis/
        │       ├── Board.java           ← KHÔNG ĐỔI
        │       ├── ComputerPlayer.java  ← KHÔNG ĐỔI
        │       ├── MoveRequest.java     ← KHÔNG ĐỔI
        │       ├── MoveResponse.java    ← KHÔNG ĐỔI
        │       └── MoveServlet.java     ← MỚI
        └── webapp/
            └── index.html              ← DI CHUYỂN + sửa 1 dòng fetch URL
```

---

## Hướng dẫn Build & Deploy

### Build

```bash
mvn clean package
# Output: target/tttbasic-1.0-SNAPSHOT.war (285 KB)
```

### Deploy lên Tomcat 10.1

```powershell
# Copy WAR vào thư mục webapps của Tomcat
copy target\tttbasic-1.0-SNAPSHOT.war <TOMCAT_HOME>\webapps\tttbasic.war

# Tomcat tự động deploy khi phát hiện file WAR mới
# (không cần restart nếu autoDeploy=true — mặc định)
```

### Truy cập

```
http://localhost:8080/tttbasic/
```

| URL | Mô tả |
|-----|-------|
| `GET  /tttbasic/` | Tải `index.html` — giao diện game |
| `POST /tttbasic/move` | API xử lý nước đi |

---

## Kết quả Build

```
BUILD SUCCESS
File: target/tttbasic-1.0-SNAPSHOT.war
Size: 285,164 bytes
```
