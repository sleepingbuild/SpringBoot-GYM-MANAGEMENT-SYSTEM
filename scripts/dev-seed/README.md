# Dữ liệu mẫu cho môi trường dev / demo

Các file `.sql` ở đây **không phải migration**, chỉ chạy tay trên database dev (không chạy trên CI).
Chạy lại nhiều lần được, mỗi file chỉ dọn đúng dữ liệu do chính nó tạo.

| File | Agent | Nội dung | Phụ thuộc |
|---|---|---|---|
| `00_users.sql` | 1 | Tài khoản mọi vai trò (mật khẩu chung `Gym@12345`) | không |
| `10_packages_memberships.sql` | 2 | Gói tập và gói của hội viên | 00 |
| `20_bookings.sql` | 3 | Lịch làm việc PT và buổi tập mẫu | 00, 10 |
| `40_attendance.sql` | 4 | Chấm công 30 ngày, lượt vào phòng tập, thêm 3 nhân sự và 12 hội viên `@gym.test` | 00 |
| `41_pt_live_session.sql` | 4 | Một buổi PT đang diễn ra (PT và hội viên đã quét vào, chưa quét ra) | 00, 40 |
| `50_pos.sql` | 5 | Sản phẩm POS và mức lương PT | 00 |
| `60_leads_reports.sql` | 6 | Lead, hội viên và giao dịch mẫu cho báo cáo | không |
| `61_expiring_members.sql` | 6 | Hội viên sắp hết hạn cho tab "Hội viên sắp hết hạn" | 60 |

Tài khoản mẫu để đăng nhập: `admin@gym.test`, `sales@gym.test`, `reception@gym.test`, `trainer1@gym.test`,
`member.active@gym.test`... (mật khẩu `Gym@12345`). Dữ liệu của file 60 và 61 dùng email `seed.*@test.local`
(không đăng nhập được) và các gói tên "Seed ...", không trùng với dữ liệu của các seed khác.

## Chạy tất cả trong Docker

Điều kiện: `docker compose up -d` đã chạy xong và backend đã khởi động ít nhất một lần
(Flyway tạo bảng; các seed cần bảng đã có).

```powershell
.\scripts\dev-seed\run-all.ps1          # Windows PowerShell
```

```bash
./scripts/dev-seed/run-all.sh           # Linux, macOS, Git Bash
```

Tham số tuỳ chọn: tên container (`gms-sqlserver`), tên database (`gms_db`), mật khẩu `sa`
(biến `GMS_SQL_CONTAINER`, `GMS_DB`, `MSSQL_SA_PASSWORD` hoặc tham số của `run-all.ps1`).
Script dừng ngay ở file đầu tiên bị lỗi.

## Lưu ý khi đối chiếu số liệu báo cáo

Số liệu mẫu ghi trong chú thích đầu file 60 và 61 (ví dụ tổng doanh thu, số hội viên sắp hết hạn) tính khi
**chỉ chạy file 60 và 61**. Khi chạy đủ tám file, các báo cáo còn cộng thêm dữ liệu của seed 10, 20, 40, 41 và 50
(gói của hội viên mẫu, buổi tập, chấm công và lượt vào phòng tập, đơn POS) nên các con số sẽ lớn hơn.

Chạy lại `60_leads_reports.sql` sẽ xoá luôn hội viên `seed.exp*@test.local` của file 61, khi đó chạy lại file 61.

## Lưu ý riêng của từng file

- `20_bookings.sql`: không chạy trong khoảng 22:30 đến 24:00 giờ Việt Nam, vì giờ kết thúc của buổi "sắp diễn ra" sẽ qua nửa đêm
  (xem chú thích đầu file). Nếu script lỗi ở file này thì chạy lại vào giờ khác.
- `50_pos.sql`: theo chú thích của Agent 5, file chưa được chạy thử trên SQL Server thật khi viết. Nếu lỗi cú pháp thì báo Agent 5.
- `40_attendance.sql`: cần `00_users.sql` trước; dòng chấm công seed có `notes` bắt đầu bằng `[dev-seed]`, lượt vào phòng tập seed có phần lẻ giây `.777`; chạy lại chỉ xóa đúng các dòng đó.
- `41_pt_live_session.sql`: cần `40_attendance.sql` (dùng `member04@gym.test`); chỉ tạo buổi đang diễn ra khi chạy trong khung 06:00 đến 21:59 giờ Việt Nam, ngoài khung thì bỏ qua.
