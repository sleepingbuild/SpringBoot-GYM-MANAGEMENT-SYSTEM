# ROADMAP.md — Lộ trình Triển khai (10 Tuần / 5 Giai đoạn)

> Phạm vi đầy đủ xem `REQUIREMENTS.md`. Phân công chi tiết xem `TASK_ASSIGNMENT.md`.

## Giai đoạn 1: Nền tảng & Hồ sơ cá nhân — Tuần 1–2
**Chủ trì:** Agent 1

- [x] Spring Boot + SQL Server + Redis (Docker Compose), Security/JWT/RBAC (5 role)
- [x] `ApiResponse<T>`, `ErrorCode` (đủ mã cho mọi module), `GlobalExceptionHandler` chuẩn hoá
- [x] Redis JWT blacklist khi logout (không còn TODO)
- [x] Module Hồ sơ cá nhân (tuổi/cân nặng/chiều cao/mục tiêu/avatar)
- [ ] **Mốc nghiệm thu:** đăng ký/đăng nhập + RBAC 5 role qua Swagger; CRUD Profile hoàn chỉnh; logout xong thì token cũ không dùng lại được (test qua Swagger) — *code đã xong, còn chờ chạy thử thực tế*

## Giai đoạn 2: Membership/Payment & Booking Engine — Tuần 3–5
**Chủ trì:** Agent 2, Agent 3

- [ ] State machine gói tập đầy đủ (PENDING/ACTIVE/EXPIRED/SCHEDULED), nâng cấp/hạ cấp, `getCurrentMembership` 1 nguồn chân lý
- [ ] `TrainerSchedule` theo `work_date`, `Booking` đủ rule (slot, chặn trùng giờ, giờ quá khứ, ngoài ca làm việc)
- [ ] Lazy auto-status: EXPIRED/SCHEDULED→ACTIVE (membership), CANCELLED/PT_NO_SHOW/NO_SHOW (booking)
- [ ] **Mốc nghiệm thu:** đăng ký→thanh toán→active chạy đúng; đặt lịch PT full rule có test; auto NoShow hoạt động đúng qua lazy-check

## Giai đoạn 3: Face Attendance & Chấm công — Tuần 6–7
**Chủ trì:** Agent 4

- [ ] Kiến trúc descriptor + Euclidean distance (server verify, không tin client), ngưỡng 0.45
- [ ] 3 luồng: Kiosk (1:N), Tự điểm danh (1:1), Đăng ký hộ
- [ ] Check-in/check-out thông minh (lần 1 = in, lần 2 = out) cho cả Booking và StaffAttendance
- [ ] Chấm công PT/Lễ tân: trạng thái động (Đúng giờ/Đi muộn/Về sớm)
- [ ] **Mốc nghiệm thu:** nhận diện đúng người trong bộ test ≥95%, không tạo double check-in/out do race condition

## Giai đoạn 4: POS, Commission & Leads/CRM — Tuần 8–9
**Chủ trì:** Agent 5, Agent 6

- [ ] POS: sản phẩm, đơn hàng, trừ tồn kho tự động
- [ ] Commission PT (khi Booking COMPLETED) + Commission Sales (khi Lead CONVERTED)
- [ ] Leads/CRM: CRUD, chuyển đổi Lead→User, workflow chăm sóc hội viên sắp hết hạn
- [ ] **Mốc nghiệm thu:** bán hàng POS trừ kho đúng; hoa hồng PT/Sales tự tính đúng số tiền qua vài kịch bản test

## Giai đoạn 5: Dashboard, Open API & Hoàn thiện — Tuần 10
**Chủ trì:** Agent 6 | **Tất cả agent:** bổ sung Swagger, fix bug tồn đọng, buffer test

- [ ] API báo cáo: doanh thu, tỷ lệ gia hạn, lưu lượng check-in, top gói, tổng hoa hồng
- [ ] Swagger UI/OpenAPI hoàn thiện toàn bộ, response format thống nhất
- [ ] Rà soát bảo mật, chuẩn bị bàn giao frontend/demo
- [ ] **Mốc nghiệm thu:** demo end-to-end đủ 5 vai trò, Swagger UI đầy đủ endpoint

---

## Sau v1.0 (đề xuất, chưa lên lịch)
- Group X / lớp tập nhóm (đã hạ mức ưu tiên xuống "tuỳ chọn" trong `TASK_ASSIGNMENT.md`)
- Zalo ZNS/SMS nhắc gia hạn tự động qua job định kỳ
- Deploy production (Render backend + Vercel frontend, hoặc Azure — chưa chốt, xem thảo luận trong lịch sử trò chuyện)
- Multi-branch reporting nâng cao
