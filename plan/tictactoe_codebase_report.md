# Báo cáo Chi tiết Codebase Tic-Tac-Toe

Báo cáo này giải thích kiến trúc tổng thể, chức năng của từng file và luồng hoạt động (flow) của dự án Tic-Tac-Toe.

## 1. Kiến trúc Tổng quan (Architecture)

Dự án hiện tại được thiết kế theo mô hình **Client-Server phi trạng thái (Stateless HTTP Client-Server)**.
- **Giao thức giao tiếp**: HTTP (Cụ thể là các HTTP POST request tới endpoint `/move`).
- **Định dạng dữ liệu**: JSON (Sử dụng thư viện `Gson` để chuyển đổi giữa Java Object và JSON).
- **Tính phi trạng thái (Stateless)**: Server hoàn toàn không lưu trữ bộ nhớ về bất kỳ ván cờ nào. Toàn bộ trạng thái của bàn cờ (board) được Client gửi kèm trong mỗi request. Server thực hiện tính toán nước đi tiếp theo và trả về trạng thái bàn cờ mới cho Client. Điều này giúp Server có thể phục vụ nhiều người chơi mà không bị nhầm lẫn hay phải cấp phát tài nguyên cho từng luồng riêng biệt.

## 2. Giải thích chi tiết từng file

### Các file Data Transfer Objects (DTOs)
Đây là các class đóng vai trò làm cấu trúc dữ liệu để đóng gói thông tin khi Client và Server giao tiếp với nhau.
- **`MoveRequest.java`**: Gói tin Client gửi lên Server. Chứa trạng thái hiện tại của bàn cờ (dưới dạng chuỗi 9 ký tự, ví dụ: `"102000000"`) và vị trí mà người chơi muốn đánh (từ 1 đến 9).
- **`MoveResponse.java`**: Gói tin Server trả về cho Client. Bao gồm:
  - `status`: Trạng thái của trận đấu hiện tại (`"ongoing"`, `"win"`, `"lose"`, `"draw"`, `"invalid"`).
  - `board`: Trạng thái bàn cờ mới nhất (chuỗi 9 ký tự) sau khi cả người và máy đã đánh.

### Các thành phần Core Logic
- **`Board.java`**: Trái tim của trò chơi, chứa toàn bộ logic của bàn cờ Tic-Tac-Toe.
  - Quản lý trạng thái bàn cờ bằng mảng `int[] cells` gồm 9 phần tử (0: Trống, 1: Người, 2: Máy).
  - Chứa các hàm kiểm tra logic game: `isValidMove` (bước đi có đè lên ô đã có chưa), `hasWon` (kiểm tra các đường thắng ngang/dọc/chéo), `isFull` (kiểm tra ván cờ hòa).
  - Hỗ trợ Serialize/Deserialize: Cung cấp `toLine()` (biến mảng thành chuỗi) và `fromLine(String line)` (khởi tạo mảng từ chuỗi) để dễ dàng chuyển qua mạng.

- **`HumanPlayer.java`**: Xử lý đầu vào từ bàn phím qua `Scanner`. Kiểm tra tính hợp lệ cơ bản của đầu vào (như phải là số từ 1-9, không được chọn ô đã đánh) và cho phép người chơi gõ `q` để thoát.
- **`ComputerPlayer.java`**: Chứa logic "AI" của hệ thống. Hiện tại logic đang cực kỳ cơ bản: Máy sẽ tự động tìm ô trống đầu tiên trên bàn cờ (`board.firstEmptyCell()`) để đánh.

### Client & Server
- **`Server.java`**: Máy chủ HTTP xử lý logic Game.
  - Sử dụng API có sẵn của JDK (`HttpServer`) lắng nghe trên cổng `9090` với một endpoint là `/move`.
  - Hàm `handleMove` thực hiện toàn bộ logic của một lượt chơi: 
    1. Cập nhật nước đi của người chơi.
    2. Kiểm tra xem người chơi đã thắng hoặc bàn cờ đã hòa chưa -> Nếu có, trả kết quả.
    3. Nếu chưa kết thúc, gọi `ComputerPlayer` lấy nước đi của máy và cập nhật.
    4. Kiểm tra xem máy đã thắng hoặc hòa chưa -> Nếu có, trả kết quả.
    5. Nếu ván cờ vẫn chưa ngã ngũ, trả về trạng thái `"ongoing"` cùng bàn cờ mới.

- **`Client.java`**: Ứng dụng phía người dùng.
  - Khởi tạo một `Board` rỗng. Vòng lặp trò chơi diễn ra liên tục.
  - In bàn cờ ra Console và gọi `HumanPlayer` để yêu cầu nhập nước đi.
  - Đóng gói nước đi thành `MoveRequest` và gửi HTTP POST request đến Server qua `HttpClient`.
  - Phân tích `MoveResponse` từ Server:
    - Nếu `"invalid"`, yêu cầu nhập lại.
    - Cập nhật bàn cờ cục bộ từ chuỗi trả về.
    - Dựa vào `status`, Client quyết định đi tiếp vòng lặp (`"ongoing"`) hay kết thúc game, in ra thông báo thắng/thua/hoà.

## 3. Luồng hoạt động của trò chơi (Execution Flow)

> [!NOTE] 
> Bạn cần chạy file `Server.java` trước để Server mở cổng mạng lắng nghe, sau đó mới chạy file `Client.java`.

1. **Khởi tạo Game**: `Client` chạy lên, tạo ra một bàn cờ trống (được biểu diễn ngầm định qua chuỗi `"000000000"`).
2. **Lượt Người chơi**:
   - Client hiển thị bàn cờ ra giao diện terminal/console.
   - Người chơi nhận thông báo nhập nước đi (chọn một ô số từ 1-9).
3. **Gửi Request**: 
   - Trạng thái của bàn cờ và nước đi của người dùng được Client chuyển thành chuẩn JSON.
   - Gửi sang Server thông qua một HTTP POST Request.
4. **Server Tính toán**:
   - Server phân tích JSON ra object `MoveRequest`, tái tạo lại bàn cờ bằng cách đọc chuỗi.
   - Cập nhật nước đi của người chơi và đánh giá (thắng/hoà).
   - Nếu game chưa kết thúc, Server lấy nước đi của Máy, cập nhật và đánh giá.
5. **Trả Response**:
   - Server xuất trạng thái mới nhất của bàn cờ thành chuỗi.
   - Gói cùng cờ trạng thái trận đấu vào `MoveResponse` (VD: trạng thái `"ongoing"` nếu chưa kết thúc) chuyển thành JSON, trả qua HTTP Response cho Client.
6. **Client Cập nhật & Hiển thị**:
   - Client nhận thông điệp, tái tạo lại trạng thái bàn cờ để đồng bộ với Server.
   - Nếu status là `"ongoing"`, tiến trình lặp lại bước 2.
   - Nếu status biểu thị kết thúc game, Client sẽ hiển thị bảng cờ cuối cùng, in ra dòng chữ thắng/thua/hoà và thoát chương trình.
