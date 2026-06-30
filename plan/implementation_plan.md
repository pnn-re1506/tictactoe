# Browser-Based Tic-Tac-Toe Client (CORS Edition)

Dựa trên phản hồi của bạn, tôi hiểu rằng bạn muốn giữ file `index.html` hoạt động độc lập (mở trực tiếp từ file hoặc Live Server) nhưng vẫn đặt chung thư mục với code Java, đồng thời cấu hình **CORS** cho Java Server. 

## Proposed Changes

### 1. Cấu hình CORS cho Java Server

#### [MODIFY] [Server.java](file:///c:/Users/phucn/OneDrive/Documents/tictactoe/tttbasic/src/main/java/vgu/pe2026/ttt/basis/Server.java)
- Bổ sung các header CORS (`Access-Control-Allow-Origin: *`, `Access-Control-Allow-Methods: POST, OPTIONS`, `Access-Control-Allow-Headers: Content-Type`) vào hàm trả về của `MoveHandler`.
- Xử lý thêm request method `OPTIONS` (Preflight request). Khi dùng `fetch` để gửi JSON, trình duyệt sẽ tự động gửi một request `OPTIONS` lên trước để kiểm tra CORS, ta cần cho phép Server trả lời request này thành công (HTTP 204 No Content).

### 2. Tạo file HTML Client đơn giản

#### [NEW] [index.html](file:///c:/Users/phucn/OneDrive/Documents/tictactoe/tttbasic/src/main/java/vgu/pe2026/ttt/basis/index.html)
- Tạo file `index.html` nằm cùng thư mục package với các file Java (`vgu/pe2026/ttt/basis/`).
- Sử dụng đúng cấu trúc HTML và CSS đơn giản bạn đã cung cấp.
- Bổ sung script xử lý logic:
  - Khởi tạo board state là chuỗi 9 ký tự `"000000000"`.
  - Hàm `startGame()`: Làm sạch board.
  - Hàm `makeMove(cellIndex)`: 
    - Cập nhật UI (hiển thị 'X').
    - Gọi `fetch('http://localhost:9090/move', { method: 'POST', body: JSON.stringify({...}) })`.
    - Phân tích response, hiển thị nước đi của máy tính ('O') và in ra thông báo nếu kết thúc.

## Verification Plan

### Manual Verification
1. Cập nhật và chạy lại `Server.java` trên IDE.
2. Mở trực tiếp file `index.html` từ File Explorer bằng trình duyệt Chrome/Edge (file sẽ chạy dưới dạng `file:///...`).
3. Nhấn "New Game" và thử đánh cờ.
4. Trình duyệt sẽ gửi API thành công do đã có CORS, máy tính sẽ đánh trả lại chữ 'O' trên màn hình.
