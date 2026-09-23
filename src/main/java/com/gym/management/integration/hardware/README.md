# integration/hardware

Sở hữu: **Agent 4** (Face Attendance & Chấm công).

Đặt tại đây:
- `FaceMatchService.java` — interface: nhận descriptor (mảng 128 số thực) + danh sách
  descriptor đã đăng ký, trả về `userId` khớp nhất nếu khoảng cách Euclidean < ngưỡng
  (mặc định 0.45), hoặc rỗng nếu không khớp ai. Xem `REQUIREMENTS.md` mục 4.
- Implementation mặc định: tính khoảng cách Euclidean thuần Java (không cần thư viện ML
  nặng — descriptor đã được trích ở client bằng face-api.js).

Nguyên tắc: `service` gọi `FaceMatchService` qua interface, không phụ thuộc thuật toán so
khớp cụ thể — để dễ nâng cấp lên thuật toán/API khác sau này (Cloud API, model riêng...)
mà không đổi API/DB phía trên. Server LUÔN tự tính lại khoảng cách, không tin bất kỳ kết
quả "đã khớp" nào client tự gửi lên.
