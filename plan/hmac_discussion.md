# Thảo luận về Kiến trúc Stateless và Giải pháp Bảo mật HMAC

Tài liệu này ghi chú lại nội dung cuộc trò chuyện về các lỗ hổng bảo mật trong kiến trúc Stateless của dự án Tic-Tac-Toe và cách áp dụng HMAC để khắc phục.

## 1. Lỗ hổng của mô hình Stateless hiện tại
Trong mô hình Stateless hiện tại (Server không lưu trạng thái), Server hoàn toàn tin tưởng vào dữ liệu bàn cờ (`boardLine`) do Client gửi lên. Điều này dẫn đến việc Client có thể can thiệp dữ liệu và gian lận theo các cách sau:

1. **Gửi bàn cờ giả mạo (State Manipulation):** Client lén tự điền các quân cờ có lợi (ví dụ gửi chuỗi `"110000000"`) và đánh nước quyết định để thắng ngay lập tức.
2. **Xóa nước đi của Máy (Erasing Moves):** Client nhận bàn cờ từ Server nhưng cố tình xóa quân của máy tính đi và gửi lại trạng thái cũ kèm nước đi mới.
3. **Thêm quân bất hợp pháp:** Client điền nhiều quân cờ của mình cùng lúc trong một lượt gửi.
4. **Đi lại vô hạn (Undo/Replay):** Client lưu lại các trạng thái cũ và gửi lại nếu bị máy tính dồn vào thế bí.

## 2. Giải pháp: HMAC (Hash-based Message Authentication Code)
Để ngăn chặn gian lận mà vẫn giữ nguyên mô hình Stateless, ta sử dụng HMAC để cấp "chữ ký điện tử" cho trạng thái bàn cờ.

### Cách thức hoạt động:
- **Secret Key:** Server lưu trữ một khóa bí mật (vd: `"Vgu_Tictactoe_Secret"`). Khóa này không bao giờ được gửi qua mạng.
- **Đóng dấu (Signing):** Khi Server tính toán xong bàn cờ mới, nó băm (hash) chuỗi bàn cờ đó cùng với Secret Key để tạo ra một Chữ ký điện tử (Signature).
  `Signature = Hash(Secret_Key + Board_Data)`
- **Xác thực (Verifying):** Lượt tiếp theo, Client bắt buộc gửi kèm bàn cờ và chữ ký. Server tính lại Hash dựa trên bàn cờ được gửi và Secret Key. Nếu Hash tính ra khớp với chữ ký Client gửi, bàn cờ hợp lệ. Nếu Client tự sửa bàn cờ, mã Hash sẽ bị sai và Server sẽ từ chối.

## 3. Quy tắc ngăn chặn "Gian lận ngay từ nước đầu tiên"
Nếu Server tự động chấp nhận nước đi đầu tiên mà không cần chữ ký, Client vẫn có thể gửi một bàn cờ chiến thắng ngay lập tức. Để ngăn việc này:
- **Quy tắc Start Token:** Client không được tự khởi tạo bàn cờ. Client phải gọi lệnh `START` lên Server.
- Server tiếp nhận lệnh `START`, tạo ra bàn cờ trống `"000000000"`, ký HMAC vào đó và trả về cho Client.
- Từ đó trở đi, **mọi** lệnh `MOVE` đều bắt buộc phải có chữ ký hợp lệ. Client chỉ có thể bắt đầu từ `"000000000"`.

## 4. Tại sao hệ thống vẫn Stateless?
Stateless có nghĩa là Server không lưu giữ thông tin (Session, GameID) của Client trên bộ nhớ (RAM/DB) giữa các Request.
Với cơ chế HMAC:
- Mọi dữ liệu cần thiết (trạng thái bàn cờ) và bằng chứng xác thực (chữ ký) đều được Client cung cấp đầy đủ trong mỗi Request.
- Server chỉ đóng vai trò nhận đầu vào, tính toán, và trả kết quả rồi "quên" ngay lập tức. Không tốn bất kỳ bộ nhớ nào để lưu trữ trạng thái người chơi. Do đó, hệ thống hoàn toàn giữ vững tính chất Stateless.
