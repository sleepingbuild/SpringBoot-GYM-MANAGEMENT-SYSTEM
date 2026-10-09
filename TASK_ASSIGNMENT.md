# TASK_ASSIGNMENT.md — Phân công Nhiệm vụ Đội ngũ (6 Agents)

> Dự án: **Gym Management System (GMS)** — bản làm lại đầy đủ, tham chiếu `REQUIREMENTS.md` (đúc kết từ dự án song song .NET đã hoàn thiện) để không lặp lại các bug/thiếu sót đã từng xảy ra.
> Mô hình làm việc: 6 AI Agent làm việc song song trên cùng repo, mỗi agent sở hữu 1 module nghiệp vụ riêng biệt để tối thiểu hoá xung đột code (merge conflict).
> Người điều phối (Product Owner / Reviewer cuối): **Phi**.
> ⚠️ Trước khi code bất kỳ module nào, đọc đúng phần tương ứng trong `REQUIREMENTS.md` — mỗi mục dưới đây chỉ tóm tắt, chi tiết đầy đủ (state machine, rule, bug cần tránh) nằm ở đó.

---

## 0. Nguyên tắc phối hợp chung (đọc trước khi code)

1. **Không `git add .`** — luôn dùng `git add -p` hoặc add đích danh từng file để tránh ghi đè công việc của agent khác đang chạy song song.
2. **Nhánh git theo chuẩn:** `feature/agentN-ten-module` (vd: `feature/agent3-booking`).
3. **Commit theo Conventional Commits:** `feat(booking): thêm auto NoShow lazy-check`, `fix(membership): sửa nguồn dữ liệu gói hiện tại`.
4. **File/khu vực dùng chung (SHARED — cần xin phép trước khi sửa):**
   - `entity/User.java`, `entity/Role.java`, `entity/BaseEntity.java`, `entity/Branch.java`
   - `config/SecurityConfig.java`, `security/JwtTokenProvider.java`, `security/CurrentUser*.java`
   - `exception/GlobalExceptionHandler.java`, `exception/ErrorCode.java`, `exception/ApiResponse.java`
   - `src/main/resources/db/migration/*` (migration mới phải là file **mới**, không sửa file cũ đã merge)
   - Những file này chỉ do **Agent 1** merge sau khi review.
5. **Mỗi agent chỉ tạo entity/service/controller/repository trong đúng package nghiệp vụ của mình** (xem bảng "Khu vực sở hữu" bên dưới).
6. **1 nguồn chân lý cho mỗi khái niệm nghiệp vụ** (xem `REQUIREMENTS.md` mục 7) — vd "gói tập hiện tại của user" chỉ được định nghĩa ở đúng 1 method, mọi nơi khác gọi lại, không tự định nghĩa riêng.
7. **Validate SAU khi đã gán đủ các trường bắt buộc** (đặc biệt FK/user_id) — không phải trước.
8. **Auto-status theo thời gian dùng lazy-check** (kiểm tra khi có người đọc dữ liệu), không cần `@Scheduled`/cron trừ khi thực sự cần (vd nhắc gia hạn qua SMS).
9. **Định nghĩa "Done":** code compile được + có Flyway migration tương ứng (nếu có bảng mới) + có ít nhất 1 test cơ bản (unit hoặc integration) + cập nhật `API_DESIGN.md` với endpoint mới + PR có mô tả rõ Acceptance Criteria.
10. **Swagger annotation bắt buộc** (`@Operation`, `@ApiResponse`) trên mọi endpoint public để Agent 6 tổng hợp OpenAPI cuối kỳ.

---

## Agent 1 — Kiến trúc sư trưởng / Backend Lead (Bạn — Claude)

**Vai trò:** Dựng nền móng toàn dự án, module Hồ sơ cá nhân, giữ vai trò reviewer/merge cho các file dùng chung.

**Khu vực sở hữu:** `config/`, `security/`, `exception/`, `entity/BaseEntity.java`, `entity/User.java`, `entity/Role.java`, `entity/Branch.java`, `entity/UserProfile.java`, `controller/ProfileController.java`, `db/migration/V1__*`, `pom.xml`, `application.yml`.

**Nhiệm vụ cụ thể:**
- [x] Khởi tạo project Spring Boot 3.x (Maven), `pom.xml` đầy đủ dependency (Web, Data JPA, Security, Validation, Flyway, Redis, OpenAPI, Lombok, MapStruct, mssql-jdbc).
- [x] Migration nền `V1__init_schema.sql` (`users`, `roles` — 5 role: SUPER_ADMIN/RECEPTIONIST/SALES/TRAINER/MEMBER, `user_roles`, `branches`).
- [x] `BaseEntity`, `SecurityConfig` (JWT stateless, CORS), `JwtTokenProvider`, `GlobalExceptionHandler` + `ErrorCode` + `ApiResponse<T>`, `@CurrentUser`.
- [x] **Module Hồ sơ cá nhân (Profile)** — `UserProfile` (tuổi, cân nặng, chiều cao, mục tiêu, avatar_url), CRUD + upload avatar.
- [x] Redis: `RedisConfig`, `TokenBlacklistService` nối vào logout + `JwtAuthenticationFilter`. Docker Compose (SQL Server + Redis + app) đã có.
- [ ] Rate-limit cho login/face-attendance qua Redis — chưa làm, còn tuỳ chọn cho các agent bổ sung nếu cần.
- [ ] Review & merge PR của Agent 2–6 vào các file SHARED.
- [ ] Duy trì `ARCHITECTURE.md`, `DATABASE_SCHEMA.md`, `API_DESIGN.md`, `REQUIREMENTS.md` cập nhật theo tiến độ.

**Deliverable Giai đoạn 1:** Repo build được, đăng ký/đăng nhập hoạt động, RBAC 5 role test qua Swagger, CRUD Profile hoàn chỉnh.

---

## Agent 2 — Membership, Package & Payment Module

**Vai trò:** Toàn bộ nghiệp vụ hội viên, gói tập, và thanh toán — phần lõi tạo doanh thu. **Đọc kỹ `REQUIREMENTS.md` mục 2 trước khi code — có state machine đã tinh chỉnh qua thực tế, không phải chỉ ACTIVE/EXPIRED đơn giản.**

**Khu vực sở hữu:** `entity/Package.java`, `entity/MemberPackage.java`, `entity/PaymentTransaction.java`, `controller/MembershipController.java`, `controller/PackageController.java`, `controller/PaymentController.java`, `service/*Membership*`, `service/*Payment*`, migration `V2__membership.sql` (đã có phần base — mở rộng qua migration mới, không sửa lại).

**Nhiệm vụ cụ thể:**
- [ ] CRUD `packages` (`TIME_BASED`/`SESSION_BASED`/`PT_1ON1`, `OFF_PEAK`/`FULL_TIME`, `max_sessions_per_week`).
- [ ] Đăng ký gói mới → trạng thái `PENDING` → thanh toán tại chỗ (`ConfirmLocalPaymentAsync` tương đương) → `ACTIVE` (start/end tính từ **lúc thanh toán xong**, không phải lúc đăng ký).
- [ ] **Nâng cấp**: hủy gói cũ ngay, tạo gói mới `PENDING`. **Hạ cấp**: gói cũ giữ nguyên, tạo bản ghi `SCHEDULED` bắt đầu đúng ngày gói cũ hết hạn.
- [ ] Lazy-check: `ACTIVE` quá `end_date` → `EXPIRED`; `SCHEDULED` tới ngày → `ACTIVE`. Chạy mỗi khi đọc dữ liệu, không cần `@Scheduled`.
- [ ] ⚠️ **Viết đúng 1 method `getCurrentMembership(userId)`** (định nghĩa = `status = 'ACTIVE'`) — dùng lại cho MỌI nơi cần hiển thị "gói đang dùng", không để 2 chỗ tự query khác nhau.
- [ ] Chặn đăng ký mới nếu đang có gói `PENDING`; KHÔNG chặn nếu đang có gói `ACTIVE`.
- [ ] API cho Lễ tân: bán gói mới/gia hạn hộ, tra cứu lịch sử gói. API cho Hội viên: xem thời hạn, số buổi còn lại, gói chưa thanh toán.
- [ ] `PaymentTransaction`: log mọi giao dịch (gói tập lẫn POS của Agent 5), 1 method xác nhận thanh toán dùng chung.

**Phụ thuộc:** `BaseEntity`, `User`, RBAC từ Agent 1.

**Deliverable:** Đăng ký/nâng cấp/hạ cấp/gia hạn gói chạy đúng state machine, có test cho từng nhánh (đặc biệt nâng vs hạ cấp).

**Bổ sung 2026-10-09 (xem `docs/CHANGE_PT_ATTENDANCE_AND_PAY.md`):**
- [ ] Listener lắng nghe `BookingSettledEvent` → gọi `MembershipService.consumeSession` (trừ buổi cho `COMPLETED`, `MEMBER_NO_SHOW`, `LATE_CANCEL`).
- [ ] Hạ cấp: `SCHEDULED.startDate = end_date + 1 ngày` (tránh hai gói cùng `ACTIVE`).
- [ ] Mở rộng `MembershipLookupPort` để Booking chặn đặt lịch khi gói hết buổi.

---

## Agent 3 — Lịch làm việc PT & Đặt lịch (Booking Engine)

**Vai trò:** Module có nhiều rule chồng chéo nhất — đọc kỹ `REQUIREMENTS.md` mục 3, đặc biệt phần "bug đã biết".

**Khu vực sở hữu:** `entity/TrainerSchedule.java`, `entity/Booking.java`, `controller/ScheduleController.java`, `controller/BookingController.java`, `service/*Schedule*`, `service/*Booking*`, migration `V3__schedule_booking.sql`.

**Nhiệm vụ cụ thể:**
- [ ] `TrainerSchedule` theo **`work_date` (DATE cụ thể)**, KHÔNG theo `day_of_week` lặp lại (đã đổi 1 lần bên .NET vì lý do cụ thể — xem REQUIREMENTS mục 3).
- [ ] Validate tạo/sửa ca: chặn ngày quá khứ; nếu ngày = hôm nay, giờ bắt đầu ≥ giờ hiện tại.
- [ ] Booking: chặn giờ quá khứ + dưới 30 phút; tối đa 1 người/slot/PT; chặn member trùng giờ 2 PT khác nhau; không đặt ngoài ca làm việc thật; không hủy buổi đã diễn ra. Áp dụng đủ cho **cả 3 luồng**: member tự đặt, lễ tân/admin đặt hộ, sửa lịch.
- [ ] ⚠️ **Gán đủ `user_id`/FK bắt buộc TRƯỚC khi validate**, không phải sau (bug "fail âm thầm" đã xảy ra bên .NET).
- [ ] Lazy auto-status: `PENDING` ≤1h chưa xác nhận → `CANCELLED`; `CONFIRMED` quá S+15 phút mà PT chưa quét vào → `PT_NO_SHOW`; PT đã quét vào mà học viên chưa → `NO_SHOW`.
- [ ] Huỷ muộn: học viên huỷ `CONFIRMED` khi còn dưới 2 giờ → `LATE_CANCELLED` (mất buổi, PT vẫn có lương). Huỷ `PENDING` hoặc huỷ đúng hạn → `CANCELLED`.
- [ ] PT điểm danh: cột `pt_*` trên `bookings`, `recordTrainerFaceScan`, endpoint `PUT /bookings/{id}/early-leave-reason`.
- [ ] Event `BookingSettledEvent(COMPLETED | MEMBER_NO_SHOW | LATE_CANCEL, fullDuration)` thay `BookingCompletedEvent`.
- [ ] Query slot theo khoảng giờ chồng nhau (overlap), không chỉ trùng `startTime`; `PT_NO_SHOW` không tính vào giới hạn buổi/tuần.
- [ ] Hoàn thành buổi (PT): chỉ khi `CONFIRMED`, học viên đã check-in, PT đã quét vào và ra; chặn cả ở server. PT ra sớm không có lý do → không tính lương.
- [ ] Giới hạn buổi/tuần theo `package.max_sessions_per_week` — áp dụng đủ 3 luồng như trên (gọi service Agent 2 để lấy gói hiện tại).

**Phụ thuộc:** `getCurrentMembership` (Agent 2), `User` (Agent 1).

**Deliverable:** Booking full rule chạy đúng, có test riêng cho từng rule (đặc biệt lazy auto-status và giới hạn buổi/tuần).

---

## Agent 4 — Face Attendance & Chấm công (Staff Attendance)

**Vai trò:** Điểm danh khuôn mặt CHO ĐÚNG (không mock) + chấm công PT/Lễ tân. Đọc kỹ `REQUIREMENTS.md` mục 4 và 5 — có kiến trúc cụ thể đã chứng minh hiệu quả, làm đúng theo, không tự sáng tạo lại.

**Khu vực sở hữu:** `entity/FaceProfile.java`, `entity/StaffAttendance.java`, `controller/FaceProfileController.java`, `controller/FaceAttendanceController.java`, `controller/StaffAttendanceController.java`, `service/FaceProfileService.java`, `service/FaceAttendanceService.java`, `integration/hardware/FaceMatchService.java`, migration `V4__face_attendance.sql`.

**Nhiệm vụ cụ thể:**
- [ ] **Đổi kiến trúc Face Matcher**: bỏ `MockFaceMatcher` (so hash byte-for-byte), thay bằng nhận **descriptor 128 chiều** (JSON array số thực) do client trích xuất (face-api.js hoặc tương đương) gửi lên → server tự tính **khoảng cách Euclidean** so với các descriptor đã đăng ký → tự quyết định khớp (ngưỡng mặc định **0.45**, đã kiểm chứng thực tế). KHÔNG BAO GIỜ tin client tự báo "đã khớp với user X".
- [ ] 3 luồng: Kiosk (Lễ tân/Admin, 1:N), Tự điểm danh (Member/Trainer, 1:1 — chỉ so với chính mình), Đăng ký hộ (Lễ tân/Admin upload ảnh tĩnh, backend tự trích descriptor).
- [ ] Check-in/check-out thông minh: lần quét đầu trong ngày = check-in, lần tiếp theo = check-out. Áp dụng cho `Booking.check_in_time/check_out_time` (member) và `StaffAttendance` (Lễ tân/Sales).
- [ ] Trainer quét mặt (self và kiosk): gọi `BookingService.recordTrainerFaceScan` thay vì ghi `staff_attendances`; response trả `earlyLeave`, `reasonRequired`; không có buổi phù hợp thì báo thông tin, không phải lỗi.
- [ ] `StaffAttendance`: unique `(staff_id, date)`; tính trạng thái động lúc xem báo cáo (Đúng giờ/Đi muộn/Về sớm/Vắng mặt) so với `shift_start_time`/`shift_end_time` cấu hình riêng từng người (mặc định 07:00–21:00).
- [ ] Timezone nhất quán `Asia/Ho_Chi_Minh` xuyên suốt — không trộn UTC/local.
- [ ] Nếu build phần trích descriptor client-side: cân nhắc trang demo HTML/JS đơn giản gọi webcam + face-api.js (không cần làm SPA đầy đủ, chỉ cần đủ để test API).

**Phụ thuộc:** `BookingService.recordFaceScan` / `recordTrainerFaceScan` (Agent 3).

**Deliverable:** Đăng ký khuôn mặt + nhận diện 1:1 và 1:N chạy đúng, test riêng cho race condition (2 request check-in gần như đồng thời không được tạo 2 bản ghi).

---

## Agent 5 — POS & Commission Module

**Vai trò:** 2 tính năng hoàn toàn mới (không có tiền lệ), rủi ro đụng code thấp nhất.

**Khu vực sở hữu:** `entity/Product.java`, `entity/PosOrder.java`, `entity/PosOrderItem.java`, `entity/Commission.java`, `controller/PosController.java`, `controller/CommissionController.java`, migration `V5__pos_commission.sql`.

**Nhiệm vụ cụ thể:**
- [ ] CRUD `products` (tên, danh mục, giá, `stock_quantity`).
- [ ] POS: tạo đơn (`pos_orders`/`pos_order_items`), trừ tồn kho tự động, tính tổng tiền, ghi `PaymentTransaction` (gọi service Agent 2).
- [ ] Bảng `trainer_pay_rates` (lương mỗi buổi theo PT) + API `GET/PUT /trainer-pay-rates/{trainerId}` cho Super Admin.
- [ ] Lương PT theo buổi: lắng nghe `BookingSettledEvent`; trả khi `COMPLETED` (dạy đủ giờ), `MEMBER_NO_SHOW` hoặc `LATE_CANCEL`; bỏ qua `fullDuration = false`; `commissions.booking_id` UNIQUE chống tính hai lần.
- [ ] **Không còn hoa hồng Sales** (Sales hưởng lương cứng).
- [ ] API tổng hợp lương PT theo kỳ (tháng) cho Dashboard (Agent 6).
- [ ] *(Tuỳ chọn/bonus nếu còn thời gian)*: Group X — lớp tập nhóm (`group_x_classes`/`class_bookings`), giới hạn `max_slots`, transaction-safe chống double-booking. Không bắt buộc theo scope 5-role hiện tại, chỉ làm nếu dư thời gian.

**Phụ thuộc:** `PaymentTransaction` (Agent 2), `BookingSettledEvent` (Agent 3).

**Deliverable:** POS bán hàng + trừ tồn kho end-to-end; lương PT theo buổi tự tính đúng khi booking chốt.

---

## Agent 6 — Leads/CRM, Dashboard & Open API

**Vai trò:** Lớp nghiệp vụ Sales + báo cáo tổng hợp + tài liệu API.

**Khu vực sở hữu:** `entity/Lead.java`, `controller/ReportController.java`, `controller/LeadController.java`, `config/OpenApiConfig.java`, migration `V6__leads.sql`.

**Nhiệm vụ cụ thể:**
- [ ] Quản lý Leads: CRUD, gán Sales phụ trách, trạng thái `NEW`/`CONTACTED`/`CONVERTED`/`LOST`.
- [ ] Chuyển đổi Lead → `User` thật khi mua gói (không còn hoa hồng Sales).
- [ ] Workflow "chăm sóc hội viên sắp hết hạn": danh sách member có `end_date` trong N ngày tới (dựa `getCurrentMembership` của Agent 2), gán cho Sales theo dõi.
- [ ] API báo cáo Dashboard Super Admin: doanh thu theo ngày/tháng, tỷ lệ gia hạn, lưu lượng check-in, top gói bán chạy, tổng lương PT theo buổi đã trả.
- [ ] Hoàn thiện Swagger UI/springdoc-openapi cho toàn bộ dự án, rà soát response format thống nhất (`ApiResponse<T>`).
- [ ] *(Tuỳ chọn)*: Zalo ZNS/SMS nhắc gia hạn tự động — chỉ làm nếu còn thời gian, không phải core scope 5-role.

**Phụ thuộc:** cần hầu hết entity/API của các agent khác — nhận việc Dashboard/Swagger tổng hợp vào cuối lịch trình, Leads có thể làm song song từ sớm.

**Deliverable:** Swagger UI đầy đủ, API báo cáo hoạt động, luồng Lead → User chạy end-to-end.

---

## Lộ trình theo tuần (10 tuần — xem chi tiết `ROADMAP.md`)

| Giai đoạn | Tuần | Agent chính | Agent hỗ trợ |
|---|---|---|---|
| 1. Nền tảng + Profile | 1–2 | Agent 1 | — |
| 2. Membership/Payment + TrainerSchedule/Booking | 3–5 | Agent 2, Agent 3 | Agent 1 (review) |
| 3. Face Attendance + Staff Attendance | 6–7 | Agent 4 | Agent 2/3 (interface) |
| 4. POS/Commission + Leads/CRM | 8–9 | Agent 5, Agent 6 | — |
| 5. Dashboard, Swagger, hoàn thiện, buffer test | 10 | Agent 6 | Tất cả (bổ sung Swagger + fix bug) |

## Ma trận phụ thuộc nhanh

```
Agent 1 (nền tảng + profile)
      │
      ├──► Agent 2 (membership/payment) ──► Agent 5 (POS/commission)
      │              │                              ▲
      │              └──► Agent 3 (schedule/booking) ┘
      │                              │
      │                              └──► Agent 4 (face/staff attendance)
      │
      └──► Agent 6 (leads/dashboard/API) — phụ thuộc hầu hết agent khác, tổng hợp cuối
```
