# Tài khoản thử (chỉ dùng database dev)

Mật khẩu chung cho mọi tài khoản bên dưới: **`Gym@12345`**.

Nạp bằng `scripts/dev-seed/00_users.sql` (chạy thủ công trong SSMS trên database dev, kết nối `localhost,1433`). Chạy lại nhiều lần không lỗi. Không đưa file này vào migration Flyway và không dùng các tài khoản này ở môi trường thật.

| Email | Vai trò | Dùng để thử |
|---|---|---|
| `admin@gym.test` | Quản trị viên (SUPER_ADMIN) | Quản trị gói, báo cáo, mọi màn hình quản trị |
| `reception@gym.test` | Lễ tân (RECEPTIONIST) | Xác nhận thanh toán, bán gói hộ, POS, kiosk điểm danh, chấm công |
| `sales@gym.test` | Sales (SALES) | Khách tiềm năng (leads), chấm công |
| `trainer1@gym.test` | Huấn luyện viên (TRAINER) | Lịch dạy, buổi dạy, điểm danh PT |
| `trainer2@gym.test` | Huấn luyện viên (TRAINER) | Thử hai PT, tranh chấp khung giờ |
| `member.active@gym.test` | Hội viên (MEMBER) | Có gói ACTIVE còn buổi (cần `10_packages_memberships.sql` của Agent 2) |
| `member.nopackage@gym.test` | Hội viên (MEMBER) | Chưa có gói: thử bị chặn đặt lịch, `MEMBERSHIP_NOT_FOUND` khi vào phòng tập |
| `member.expired@gym.test` | Hội viên (MEMBER) | Gói đã hết hạn: thử `MEMBERSHIP_EXPIRED` (cần `10_packages_memberships.sql`) |

## Thứ tự nạp dữ liệu mẫu

Chạy từng file trong `scripts/dev-seed/` theo thứ tự tên:

1. `00_users.sql` (Agent 1): tài khoản ở bảng trên.
2. `10_packages_memberships.sql` (Agent 2): gói mẫu, gói ACTIVE cho `member.active`, gói hết hạn cho `member.expired`.
3. `20_bookings.sql` (Agent 3): lịch làm việc PT và booking mẫu.
4. `40_attendance.sql` (Agent 4, nếu có).
5. `50_pos.sql` (Agent 5).
6. `60_leads_reports.sql` (Agent 6).

Quy tắc chung của mọi file seed: chạy lại không lỗi, tham chiếu user theo email, ngày giờ tương đối theo giờ Việt Nam.

## Ghi chú
- Hồ sơ khuôn mặt không có trong seed: phải đăng ký thật bằng camera ở màn hình đăng ký khuôn mặt.
- Hội viên tự đăng ký ở `/register` cũng dùng được để thử (vai trò MEMBER).
- Muốn thử trên bản Jenkins triển khai thì chạy seed trên `gms_db_ci`, rồi trỏ frontend tới `http://localhost:8081`.
