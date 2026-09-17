# ROADMAP.md — Lộ trình Triển khai (8 Tuần / 4 Giai đoạn)

## Giai đoạn 1: Base & Security — Tuần 1–2
**Chủ trì:** Agent 1 | **Hỗ trợ:** Agent 2

- [ ] Khởi tạo project Spring Boot, cấu hình `pom.xml`, Docker Compose (SQL Server + Redis)
- [ ] Thiết kế Database Schema chuẩn (`V1__init_schema.sql`: users, roles, user_roles, branches)
- [ ] Spring Security + JWT (access/refresh token), RBAC
- [ ] Global Exception Handler + chuẩn `ApiResponse<T>`
- [ ] CRUD Quản lý Hội viên (User) và Quản lý Gói tập (Package)
- [ ] **Mốc nghiệm thu:** đăng ký/đăng nhập + RBAC hoạt động qua Postman; CRUD gói tập hoàn chỉnh

## Giai đoạn 2: Check-in Engine & Thanh toán — Tuần 3–4
**Chủ trì:** Agent 3, Agent 4 | **Review:** Agent 1

- [ ] Service xử lý Check-in đầy đủ 6 bước (thời hạn, khung giờ, số buổi, anti-passback)
- [ ] Redis cache cho trạng thái check-in gần nhất
- [ ] Tích hợp VietQR động + xử lý Webhook thanh toán (idempotent)
- [ ] Module POS: sản phẩm, đơn hàng, trừ tồn kho
- [ ] **Mốc nghiệm thu:** check-in trả đúng mã lỗi cho từng trường hợp; thanh toán VietQR sandbox end-to-end kích hoạt gói tự động

## Giai đoạn 3: PT, Group X & Hoa hồng — Tuần 5–6
**Chủ trì:** Agent 5 | **Song song:** Agent 6 (Leads)

- [ ] Đặt lịch PT 1:1 (lịch rảnh, đặt lịch, xác nhận 2 chiều)
- [ ] Tự động tính hoa hồng PT khi buổi tập `COMPLETED`
- [ ] Xếp lịch & đăng ký lớp Group X (transaction-safe, chống double-booking)
- [ ] Quản lý Leads + hoa hồng Sales khi chuyển đổi thành công
- [ ] **Mốc nghiệm thu:** đặt lịch + hoa hồng PT chạy end-to-end; test tải đăng ký Group X không vượt `max_slots`

## Giai đoạn 4: Dashboard & Open API — Tuần 7–8
**Chủ trì:** Agent 6 | **Review:** Agent 1, tất cả agent bổ sung Swagger

- [ ] API báo cáo doanh thu, tỷ lệ gia hạn, lưu lượng check-in
- [ ] Thông báo tự động (Zalo ZNS/SMS) nhắc gia hạn trước 7 ngày
- [ ] Hoàn thiện Swagger UI/OpenAPI cho toàn bộ API, đồng bộ format response
- [ ] Rà soát bảo mật, rate limiting, chuẩn bị bàn giao cho Frontend (React/Vue/Flutter)
- [ ] **Mốc nghiệm thu:** Swagger UI đầy đủ endpoint, có thể generate client SDK cho frontend

---

## Sau v1.0 (đề xuất, chưa lên lịch)
- Multi-branch reporting nâng cao (so sánh hiệu suất giữa các chi nhánh)
- Mobile app (đã loại khỏi scope v1.0, cân nhắc lại sau khi core ổn định)
- Face ID nhận diện thực tế thay mock hardware gateway
- Tách Check-in Engine / Payment thành microservice riêng nếu tải tăng
