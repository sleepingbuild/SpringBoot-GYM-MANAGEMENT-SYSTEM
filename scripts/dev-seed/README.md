# Dữ liệu mẫu cho môi trường dev / demo

Các file `.sql` ở đây **không phải migration**, chỉ chạy tay trên database dev (không chạy trên CI).
Chạy lại nhiều lần được, mỗi file chỉ dọn đúng dữ liệu do chính nó tạo.

| File | Agent | Nội dung | Phụ thuộc |
|---|---|---|---|
| `00_users.sql` | 1 | Tài khoản mọi vai trò (mật khẩu chung `Gym@12345`) | không |
| `10_packages_memberships.sql` | 2 | Gói tập và gói của hội viên | 00 |
| `20_bookings.sql` | 3 | Lịch làm việc PT và buổi tập mẫu | 00, 10 |
| `30_pos.sql` | 5 | Sản phẩm POS và mức lương PT | 00 |
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
**chỉ chạy file 60 và 61**. Khi chạy đủ sáu file, các báo cáo còn cộng thêm dữ liệu của seed 10, 20 và 30
(gói của hội viên mẫu, buổi tập, đơn POS) nên các con số sẽ lớn hơn.

Chạy lại `60_leads_reports.sql` sẽ xoá luôn hội viên `seed.exp*@test.local` của file 61, khi đó chạy lại file 61.

## Lưu ý riêng của từng file

- `20_bookings.sql`: không chạy trong khoảng 22:30 đến 24:00 giờ Việt Nam, vì giờ kết thúc của buổi "sắp diễn ra" sẽ qua nửa đêm
  (xem chú thích đầu file). Nếu script lỗi ở file này thì chạy lại vào giờ khác.
- `30_pos.sql`: theo chú thích của Agent 5, file chưa được chạy thử trên SQL Server thật khi viết. Nếu lỗi cú pháp thì báo Agent 5.

