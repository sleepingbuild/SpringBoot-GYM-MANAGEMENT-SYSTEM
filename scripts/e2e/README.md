# Kịch bản demo end-to-end (Issue X.2)

`demo-flow.ps1` chạy vào API thật theo đúng luồng demo của dự án và kiểm tra kết quả từng bước:

1. **Sales** tạo lead, đổi trạng thái `CONTACTED`, chuyển thành hội viên (`convert`).
2. **Lễ tân** bán gói tập (có buổi PT) và xác nhận thu tiền mặt, gói chuyển `ACTIVE`.
3. **Hội viên** đặt lịch PT, **PT** xác nhận.
4. **Quét khuôn mặt** (máy quét của lễ tân): hội viên vào, PT vào, PT ra sớm và nộp lý do.
5. **PT hoàn thành buổi**, khoản lương theo buổi tự được ghi.
6. **Lễ tân bán POS** cho hội viên đó.
7. **Admin xem báo cáo**: doanh thu gói và POS, lương PT, lưu lượng, gói bán chạy, gia hạn đều phản ánh dữ liệu vừa tạo.

Mỗi bước có kiểm tra rõ ràng, kịch bản dừng ở bước đầu tiên lỗi (mã thoát 1) và in lý do.

## Điều kiện

1. Đã nạp dữ liệu mẫu: `.\scripts\dev-seed\run-all.ps1` (cần ít nhất seed 00, 10, 20, 30: tài khoản `@gym.test`,
   gói tập, lịch làm việc của PT và mức lương theo buổi).
2. **Backend phải chạy với hai biến môi trường rút ngắn thời gian chờ**, nếu không buổi tập phải đặt trước
   30 phút và buổi PENDING còn dưới 60 phút bị tự huỷ, kịch bản không thể chạy trong vài phút:

   ```powershell
   $env:GMS_BOOKING_MIN_LEAD_MINUTES = "0"
   $env:GMS_BOOKING_PENDING_CANCEL_THRESHOLD_MINUTES = "0"
   mvn spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

   Với Docker compose, thêm hai biến này vào phần `environment` của service backend rồi `up -d` lại.
   Chỉ dùng cho môi trường demo và kiểm thử, **không đặt trên môi trường thật**.
3. Chạy trong khung giờ buổi hội phải nằm trọn trong ngày (không chạy sau 23:00 giờ Việt Nam).

## Chạy

```powershell
.\scripts\e2e\demo-flow.ps1 -BaseUrl http://localhost:18080
```

Tham số tuỳ chọn: `-Password` (mật khẩu chung của tài khoản seed, mặc định `Gym@12345`), `-TrainerEmail`
(PT dùng cho buổi tập, mặc định `trainer1@gym.test`), `-StartInMinutes` (buổi bắt đầu sau bao nhiêu phút,
mặc định 3), `-SessionMinutes` (độ dài buổi, mặc định 30).

Kịch bản tốn khoảng 30 giây vì phải chờ 16 giây giữa hai lần quét khuôn mặt của PT (luật chống quét trùng).

## Lưu ý

- Mỗi lần chạy tạo thêm dữ liệu thật (một lead, một hội viên `e2e.<thời gian>@gym.test`, một gói, một buổi tập,
  một đơn POS). Không có bước dọn dẹp, chỉ chạy trên database demo hoặc dev.
- Hội viên vừa tạo đăng nhập được trên giao diện bằng thông tin in ở dòng cuối của kịch bản, để xem tiếp
  các màn hình theo vai trò.
- Khuôn mặt dùng vector 128 chiều sinh ngẫu nhiên từ một hạt giống, đủ để máy quét nhận ra đúng người,
  không cần camera hay mô hình nhận diện.

## Lỗi thường gặp

| Thông báo | Nguyên nhân và cách xử lý |
|---|---|
| `BOOKING_TOO_SOON` | Backend chưa đặt `GMS_BOOKING_MIN_LEAD_MINUTES=0`. |
| `Buoi PENDING bi tu huy` | Backend chưa đặt `GMS_BOOKING_PENDING_CANCEL_THRESHOLD_MINUTES=0`. |
| `Chua thay khoan luong` | PT chưa có mức lương theo buổi (chạy seed 30) hoặc listener lương lỗi, xem log backend. |
| `Dang nhap ... that bai` | Chưa nạp seed 00, hoặc mật khẩu khác `Gym@12345` (dùng `-Password`). |
| `BOOKING_OUT_OF_WORKING_HOURS` hoặc lỗi trùng lịch | Ca làm việc hôm nay của PT không phủ khung giờ, hoặc PT đã có buổi trùng giờ: đổi `-TrainerEmail` sang `trainer2@gym.test` hoặc chờ vài phút rồi chạy lại. |
