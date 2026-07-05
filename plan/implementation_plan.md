# Refactoring Plan: HMAC Security for Stateless Tic-Tac-Toe

Mục tiêu của kế hoạch này là nâng cấp bảo mật cho kiến trúc Client-Server hiện tại (`week10` branch - raw socket), ngăn chặn tình trạng Client gian lận bằng cách giả mạo trạng thái bàn cờ (State Manipulation) thông qua giải pháp **HMAC SHA-256** và quy tắc **Start Game Token**.

## User Review Required

> [!WARNING]
> Kế hoạch này sẽ thay đổi giao thức (Protocol) truyền tin giữa Client và Server. Do đó, cả hai file `Client.java` và `Server.java` phải được cập nhật đồng thời để tương thích với nhau.
> 
> **Giao thức cũ:**
> - C -> S: `MOVE <board> <cell>`
> - S -> C: `RESULT <status> <board>`
> 
> **Giao thức mới dự kiến:**
> - C -> S: `START` (Lấy vé vào cửa)
> - S -> C: `RESULT start 000000000 <signature>`
> - C -> S: `MOVE <board> <cell> <signature>` (Bắt buộc đính kèm chữ ký)
> - S -> C: `RESULT <status> <new_board> <new_signature>`

## Proposed Changes

### [Server Component]

Sẽ sửa đổi class `Server.java` để thêm chức năng mã hóa và thay đổi luồng xử lý `handleMove`.

#### [MODIFY] Server.java
- **Thêm thư viện:** Import `javax.crypto.Mac`, `javax.crypto.spec.SecretKeySpec` và `java.util.Base64`.
- **Thêm biến hằng số:** Khai báo `private static final String SECRET_KEY = "Vgu_Tictactoe_Secret";`
- **Thêm hàm HMAC:** Viết một hàm `private static String generateHMAC(String data)` để tính chữ ký SHA-256 Base64 từ `data` và `SECRET_KEY`.
- **Sửa đổi logic `handleMove(String request, ...)`**:
  - Nếu `request.equals("START")`: Trả về `RESULT start 000000000 <signature_của_000000000>`.
  - Nếu bắt đầu bằng `MOVE`: Phân tách chuỗi (cần đủ 4 phần: `MOVE`, `board`, `cell`, `signature`).
  - **Xác thực (Verification):** Tính lại HMAC của `board` được gửi lên. Nếu không khớp với `signature` được cung cấp → Trả về `RESULT error 000000000 Invalid_Signature`.
  - **Ký tên response (Signing):** Sau khi tính toán ra bàn cờ mới, tạo `new_signature` cho bàn cờ đó và chèn vào cuối chuỗi `RESULT ...`.

---

### [Client Component]

Sẽ sửa đổi class `Client.java` để yêu cầu bàn cờ gốc từ Server khi bắt đầu, và luôn đính kèm chữ ký trong các nước đi tiếp theo.

#### [MODIFY] Client.java
- **Khởi tạo Game:** Trước vòng lặp `while(true)`, gọi `send("START")` để nhận bàn cờ trống cùng `currentSignature` đầu tiên.
- **Sửa gói tin `MOVE`:** Sửa chuỗi request thành `"MOVE " + board.toLine() + " " + humanMove + " " + currentSignature`.
- **Xử lý Response:** 
  - Sửa chuỗi tách `split(" ")` vì kết quả trả về sẽ có 4 phần (có thêm signature ở cuối).
  - Cập nhật biến `currentSignature` với chữ ký mới do Server trả về để dùng cho nước đi tiếp theo.
  - Xử lý thêm trường hợp `status.equals("error")` để in ra thông báo nếu chữ ký bị từ chối.

## Verification Plan

### Automated/Manual Tests
1. **Kiểm tra luồng chơi bình thường (Happy Path):** Chạy Server và Client, thử chơi một ván cờ hoàn chỉnh từ đầu đến khi Thắng/Hòa/Thua để đảm bảo việc truyền nhận chữ ký hoạt động mượt mà.
2. **Kiểm tra luồng khởi tạo (Start Token):** Đảm bảo Client không thể bắt đầu vòng lặp game nếu Server từ chối lệnh `START`.
3. **Kiểm tra chống gian lận (Anti-Cheating):** Chỉnh sửa mã nguồn `Client.java` cục bộ để ép nó gửi một `boardLine` giả mạo (ví dụ `"111000000"`) với `currentSignature` cũ. Đảm bảo Server phát hiện ra Mismatch và trả về lỗi `error` ngay lập tức, chặn nước đi đó lại.
