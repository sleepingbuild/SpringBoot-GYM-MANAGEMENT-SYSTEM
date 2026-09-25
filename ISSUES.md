# ISSUES.md — Milestones & Issues chi tiết

> Chưa có trên GitHub — đây là bản thiết kế đầy đủ để tạo trực tiếp lên GitHub Issues/Milestones
> (thủ công qua UI, hoặc dán từng issue vào `gh issue create`). Mỗi mục "Nhiệm vụ" dưới đây tương
> ứng 1-1 với checklist trong `TASK_ASSIGNMENT.md` — không phát sinh phạm vi mới ngoài đó.

## Quy ước Label

| Label | Ý nghĩa |
|---|---|
| `agent-1` … `agent-6` | Module/người phụ trách (khớp `TASK_ASSIGNMENT.md`) |
| `type:feature` | Tính năng mới |
| `type:bug` | Sửa lỗi |
| `type:docs` | Tài liệu |
| `type:test` | Viết test |
| `priority:high` / `medium` / `low` | Độ ưu tiên trong milestone |
| `shared-file` | Đụng vào file SHARED — bắt buộc Agent 1 review trước khi merge (xem `CONTRIBUTING.md`) |

## Quy ước Milestone (khớp `ROADMAP.md`)

| Milestone | Tuần | Trạng thái |
|---|---|---|
| M1 — Nền tảng & Hồ sơ cá nhân | 1–2 | ✅ Đã đóng |
| M2 — Membership/Payment & Booking Engine | 3–5 | Đang mở |
| M3 — Face Attendance & Chấm công | 6–7 | Chưa mở |
| M4 — POS, Commission & Leads/CRM | 8–9 | Chưa mở |
| M5 — Dashboard, Open API & Hoàn thiện | 10 | Chưa mở |

---

## Milestone 1 — Nền tảng & Hồ sơ cá nhân (Tuần 1–2) ✅ ĐÃ ĐÓNG

*Giữ lại để truy vết tiến độ/chấm điểm — không cần tạo lại trên GitHub trừ khi muốn có lịch sử đầy đủ.*

| # | Issue | Agent | Trạng thái |
|---|---|---|---|
| 1.1 | Khởi tạo Spring Boot + Docker Compose (SQL Server + Redis) | agent-1 | ✅ Done |
| 1.2 | Security/JWT/RBAC 5 role (`SecurityConfig`, `JwtTokenProvider`, `CustomUserDetailsService`) | agent-1 | ✅ Done |
| 1.3 | Chuẩn hoá `ApiResponse<T>` + `ErrorCode` (đủ mã cho mọi module) + `GlobalExceptionHandler` | agent-1 | ✅ Done |
| 1.4 | Redis JWT blacklist khi logout | agent-1 | ✅ Done |
| 1.5 | Module Hồ sơ cá nhân — `UserProfile` CRUD + upload avatar | agent-1 | ✅ Done |
| 1.6 | Migration `V1__init_schema.sql` + `V2_1__user_profiles.sql` | agent-1 | ✅ Done |

---

## Milestone 2 — Membership/Payment & Booking Engine (Tuần 3–5)

### Agent 2 — Membership, Package & Payment

**Issue 2.1 — CRUD Package**
- Labels: `agent-2` `type:feature` `priority:high`
- Mô tả: CRUD `/api/v1/packages` — hỗ trợ đủ 3 loại (`TIME_BASED`/`SESSION_BASED`/`PT_1ON1`), `peak_type`, `max_sessions_per_week`.
- Acceptance Criteria:
  - [ ] Tạo/sửa/xoá/xem gói tập qua API, chỉ `SUPER_ADMIN` được ghi
  - [ ] Validate `price > 0`, `duration_days`/`session_count` hợp lý theo `package_type`
  - [ ] Test CRUD cơ bản

**Issue 2.2 — Đăng ký gói mới + xác nhận thanh toán tại chỗ**
- Labels: `agent-2` `type:feature` `priority:high`
- Phụ thuộc: 2.1
- Mô tả: `POST /api/v1/member-packages` tạo bản ghi `PENDING`; `POST /api/v1/payments/{id}/confirm` chuyển `ACTIVE`, `start_date` = lúc xác nhận (không phải lúc đăng ký).
- Acceptance Criteria:
  - [ ] Đăng ký gói → status `PENDING`, chưa trừ gì
  - [ ] Xác nhận thanh toán → `ACTIVE`, `start_date`/`end_date` tính đúng từ thời điểm xác nhận
  - [ ] Ghi `payment_transactions` tương ứng
  - [ ] Test cho cả 2 bước

**Issue 2.3 — Nâng cấp gói (Upgrade)**
- Labels: `agent-2` `type:feature` `priority:medium`
- Phụ thuộc: 2.2
- Mô tả: Gói mới giá ≥ gói đang `ACTIVE` → huỷ gói cũ ngay, tạo gói mới `PENDING`.
- Acceptance Criteria:
  - [ ] Gói cũ chuyển `CANCELLED` ngay khi bấm nâng cấp
  - [ ] Gói mới `ACTIVE` ngay sau khi xác nhận thanh toán
  - [ ] Test kịch bản nâng cấp

**Issue 2.4 — Hạ cấp gói (Downgrade) + lazy-check chuyển trạng thái**
- Labels: `agent-2` `type:feature` `priority:medium`
- Phụ thuộc: 2.2
- Mô tả: Gói mới giá thấp hơn → gói cũ giữ nguyên, tạo bản ghi `SCHEDULED` bắt đầu đúng ngày gói cũ `end_date`. Lazy-check: `ACTIVE` quá hạn → `EXPIRED`; `SCHEDULED` tới ngày → `ACTIVE`.
- Acceptance Criteria:
  - [ ] Hạ cấp không đụng gói đang chạy, chỉ tạo bản ghi `SCHEDULED`
  - [ ] Lazy-check chạy đúng khi có người đọc dữ liệu (không cần `@Scheduled`)
  - [ ] Test: gói hết hạn tự `EXPIRED`; gói `SCHEDULED` tới ngày tự `ACTIVE`

**Issue 2.5 — `getCurrentMembership(userId)` — 1 nguồn chân lý** ⚠️
- Labels: `agent-2` `type:feature` `priority:high`
- Mô tả: Viết đúng 1 method dùng chung toàn hệ thống, định nghĩa "gói hiện tại" = bản ghi `status = 'ACTIVE'` duy nhất. Agent 3/5/6 sẽ gọi lại method này, không tự query riêng.
- Acceptance Criteria:
  - [ ] Method public trong `MembershipService`, có test
  - [ ] `GET /api/v1/member-packages/me` dùng đúng method này
  - [ ] Ghi rõ trong Javadoc: đây là nguồn chân lý duy nhất, không tự định nghĩa lại nơi khác (xem `REQUIREMENTS.md` mục 7)

**Issue 2.6 — Chặn đăng ký mới nếu đang có gói PENDING**
- Labels: `agent-2` `type:feature` `priority:medium`
- Acceptance Criteria:
  - [ ] Đang có gói `PENDING` → đăng ký mới trả lỗi `MEMBERSHIP_PENDING_EXISTS`
  - [ ] Đang có gói `ACTIVE` → KHÔNG chặn (test riêng để tránh regression)

**Issue 2.7 — API Lễ tân bán/gia hạn gói hộ + lịch sử gói cho Member**
- Labels: `agent-2` `type:feature` `priority:medium`
- Acceptance Criteria:
  - [ ] `RECEPTIONIST` bán gói hộ được cho user bất kỳ
  - [ ] Member xem được lịch sử toàn bộ gói đã đăng ký (mọi trạng thái)

**Issue 2.8 — `PaymentTransaction` dùng chung cho Membership và POS**
- Labels: `agent-2` `type:feature` `priority:medium`
- Phụ thuộc: sẽ được Agent 5 gọi lại ở M4
- Mô tả: 1 method `confirmPayment(relatedType, relatedId, amount, method)` dùng chung, không viết riêng cho từng module gọi tới.
- Acceptance Criteria:
  - [ ] Method public, test cho cả 2 `related_type` (`MEMBERSHIP`, `POS`)

---

### Agent 3 — Lịch làm việc PT & Booking Engine

**Issue 3.1 — CRUD TrainerSchedule theo `work_date`**
- Labels: `agent-3` `type:feature` `priority:high`
- Mô tả: PT/Admin tạo ca làm việc theo **ngày cụ thể**, không theo `day_of_week` lặp lại (xem `REQUIREMENTS.md` mục 3 — lý do đổi model từ thực tế .NET).
- Acceptance Criteria:
  - [ ] Chặn tạo ca ở ngày quá khứ
  - [ ] Ngày = hôm nay → giờ bắt đầu phải ≥ giờ hiện tại (validate cả server)
  - [ ] `GET /api/v1/trainer-schedules/available` trả đúng slot trống theo ngày

**Issue 3.2 — Tạo Booking + validate đầy đủ rule** ⚠️
- Labels: `agent-3` `type:feature` `priority:high`
- Phụ thuộc: 3.1, Issue 2.5 (Agent 2)
- Mô tả: `POST /api/v1/bookings` — áp đủ rule: chặn giờ quá khứ/<30 phút, tối đa 1 người/slot/PT, chặn trùng giờ 2 PT khác nhau, không đặt ngoài ca làm việc thật, giới hạn buổi/tuần theo `package.max_sessions_per_week` (gọi `getCurrentMembership`). **Gán đủ `user_id` TRƯỚC khi validate** (tránh bug "fail âm thầm" đã ghi trong `REQUIREMENTS.md`).
- Acceptance Criteria:
  - [ ] Đủ rule trên có test riêng từng rule
  - [ ] Áp dụng cho cả member tự đặt và lễ tân/admin đặt hộ
  - [ ] Đặt lại được sau khi sửa lịch (`PUT /bookings/{id}`) vẫn áp lại đủ rule

**Issue 3.3 — Xác nhận/Hoàn thành/Huỷ Booking**
- Labels: `agent-3` `type:feature` `priority:high`
- Phụ thuộc: 3.2
- Mô tả: PT xác nhận (`PENDING→CONFIRMED`); PT đánh dấu hoàn thành (chỉ hiện khi `check_in_time` có giá trị, chặn cả server); huỷ (không cho huỷ buổi đã diễn ra).
- Acceptance Criteria:
  - [ ] `POST /bookings/{id}/complete` trả lỗi `BOOKING_NOT_CHECKED_IN` nếu chưa điểm danh
  - [ ] `POST /bookings/{id}/cancel` trả lỗi `BOOKING_PAST_CANNOT_CANCEL` nếu buổi đã qua

**Issue 3.4 — Lazy auto-status theo thời gian**
- Labels: `agent-3` `type:feature` `priority:high`
- Phụ thuộc: 3.3
- Mô tả: Chạy mỗi khi đọc dữ liệu booking (không cần cron): `PENDING` ≤1h chưa xác nhận → `CANCELLED`; `CONFIRMED` quá 30 phút giờ hẹn chưa `COMPLETED` → `PT_NO_SHOW`; quá giờ kết thúc mà `check_in_time IS NULL` → `NO_SHOW`.
- Acceptance Criteria:
  - [ ] 3 nhánh chuyển trạng thái có test riêng (dùng thời gian giả lập)
  - [ ] Không tạo side-effect ngoài ý muốn khi gọi API đọc nhiều lần liên tiếp (idempotent)

**Issue 3.5 — Migration `V3__schedule_booking.sql`**
- Labels: `agent-3` `type:feature` `priority:high`
- Mô tả: Bảng `trainer_schedules`, `bookings` đúng theo `DATABASE_SCHEMA.md`.
- Acceptance Criteria:
  - [ ] Migration chạy sạch từ DB rỗng
  - [ ] Index `(trainer_id, booking_date)`, `(member_id, booking_date)` như đã ghi trong schema

---

## Milestone 3 — Face Attendance & Chấm công (Tuần 6–7)

### Agent 4 — Face Attendance & Staff Attendance

**Issue 4.1 — `FaceMatchService`: descriptor + Euclidean distance** ⚠️
- Labels: `agent-4` `type:feature` `priority:high`
- Mô tả: Interface + implementation nhận mảng descriptor (128 số thực), tính khoảng cách Euclidean với các descriptor đã đăng ký, trả `userId` nếu khoảng cách nhỏ nhất < ngưỡng (`gms.face-attendance.match-threshold`, mặc định 0.45). KHÔNG tin bất kỳ "matchedUserId" nào client tự gửi lên.
- Acceptance Criteria:
  - [ ] Unit test: 2 descriptor giống hệt → khớp; khác biệt lớn → không khớp
  - [ ] Ngưỡng đọc từ config, không hard-code

**Issue 4.2 — Đăng ký hồ sơ khuôn mặt (2 luồng: tự đăng ký + đăng ký hộ)**
- Labels: `agent-4` `type:feature` `priority:high`
- Phụ thuộc: 4.1
- Mô tả: `POST /api/v1/face-profiles/me` (Member/Trainer tự gửi descriptor đã trích ở client); `POST /api/v1/face-profiles/{userId}` (Lễ tân/Admin upload ảnh tĩnh, backend tự trích descriptor — cần thư viện/endpoint trích xuất phía server hoặc gọi 1 service trích descriptor riêng).
- Acceptance Criteria:
  - [ ] Đăng ký lại = ghi đè (unique theo `user_id`)
  - [ ] Trả lỗi `FACE_DESCRIPTOR_INVALID` nếu mảng sai độ dài/định dạng

**Issue 4.3 — Face Attendance Kiosk (1:N)**
- Labels: `agent-4` `type:feature` `priority:high`
- Phụ thuộc: 4.1, 4.2
- Mô tả: `POST /api/v1/face-attendance/kiosk` — chỉ Lễ tân/Admin, so với TOÀN BỘ hồ sơ.
- Acceptance Criteria:
  - [ ] Trả `FACE_NOT_RECOGNIZED` nếu không khớp ai
  - [ ] Test với ≥3 descriptor đã đăng ký, đảm bảo chọn đúng người khớp nhất

**Issue 4.4 — Face Attendance Self check-in (1:1)**
- Labels: `agent-4` `type:feature` `priority:high`
- Phụ thuộc: 4.1, 4.2
- Mô tả: `POST /api/v1/face-attendance/self` — chỉ so với đúng user đang đăng nhập (không phải 1:N).
- Acceptance Criteria:
  - [ ] Không thể tự điểm danh "hộ" người khác dù gửi đúng descriptor người khác (luôn so với `@CurrentUser`)

**Issue 4.5 — Check-in/check-out thông minh + chống race condition**
- Labels: `agent-4` `type:feature` `priority:high`
- Phụ thuộc: 4.3, 4.4
- Mô tả: Lần quét đầu trong ngày = check-in, lần 2 = check-out — ghi vào `bookings.check_in_time/check_out_time` (Member, cần match với đúng booking trong ngày) hoặc `staff_attendances` (Trainer/Lễ tân).
- Acceptance Criteria:
  - [ ] 2 request gần như đồng thời không tạo ra check-in trùng/lệch (transaction-safe)
  - [ ] Test giả lập gọi liên tiếp nhanh (race condition)

**Issue 4.6 — Chấm công PT/Lễ tân + tính trạng thái động**
- Labels: `agent-4` `type:feature` `priority:medium`
- Phụ thuộc: 4.5
- Mô tả: `GET /api/v1/staff-attendance?date=...` — tính Đúng giờ/Đi muộn/Về sớm/Vắng mặt lúc đọc (không lưu cột trạng thái), so với `shift_start_time`/`shift_end_time` (mặc định 07:00–21:00).
- Acceptance Criteria:
  - [ ] "Về sớm" chỉ tính được sau khi đã có `check_out_time`
  - [ ] Test đủ 4-5 trường hợp trạng thái

**Issue 4.7 — Migration `V4__face_attendance.sql`**
- Labels: `agent-4` `type:feature` `priority:high`
- Acceptance Criteria:
  - [ ] Bảng `face_profiles` (descriptor), `staff_attendances` (unique `staff_id,date`) đúng `DATABASE_SCHEMA.md`

**Issue 4.8 — (Tuỳ chọn) Trang demo HTML/JS trích descriptor qua webcam**
- Labels: `agent-4` `type:feature` `priority:low`
- Mô tả: Trang tĩnh đơn giản dùng face-api.js gọi webcam, trích descriptor, gửi thử API — phục vụ test/demo, không cần SPA đầy đủ.

---

## Milestone 4 — POS, Commission & Leads/CRM (Tuần 8–9)

### Agent 5 — POS & Commission

**Issue 5.1 — CRUD Product**
- Labels: `agent-5` `type:feature` `priority:medium`
- Acceptance Criteria:
  - [ ] CRUD `/api/v1/products`, chỉ Lễ tân/Admin ghi

**Issue 5.2 — Tạo đơn POS + trừ tồn kho tự động**
- Labels: `agent-5` `type:feature` `priority:high`
- Phụ thuộc: 5.1, Issue 2.8 (Agent 2)
- Acceptance Criteria:
  - [ ] `POST /api/v1/pos/orders` trừ đúng `stock_quantity`, tính đúng `total_amount`
  - [ ] Không cho tạo đơn nếu sản phẩm hết hàng (`PRODUCT_OUT_OF_STOCK`)
  - [ ] Ghi `payment_transactions` qua method dùng chung của Agent 2

**Issue 5.3 — Commission PT (tự động khi Booking COMPLETED)**
- Labels: `agent-5` `type:feature` `priority:high`
- Phụ thuộc: Issue 3.3/3.4 (Agent 3)
- Acceptance Criteria:
  - [ ] Booking chuyển `COMPLETED` → tự ghi 1 dòng `commissions` (`type=PT`) đúng số tiền theo `commission_rate`
  - [ ] Test với vài mức `commission_rate` khác nhau

**Issue 5.4 — Commission Sales (khi Lead CONVERTED)**
- Labels: `agent-5` `type:feature` `priority:medium`
- Phụ thuộc: Issue 6.2 (Agent 6)
- Acceptance Criteria:
  - [ ] Lead chuyển `CONVERTED` → tự ghi `commissions` (`type=SALES`)

**Issue 5.5 — API tổng hợp hoa hồng theo kỳ**
- Labels: `agent-5` `type:feature` `priority:medium`
- Acceptance Criteria:
  - [ ] `GET /api/v1/commissions?staffId=...&month=...` trả đúng tổng theo tháng

**Issue 5.6 — Migration `V5__pos_commission.sql`**
- Labels: `agent-5` `type:feature` `priority:high`

**Issue 5.7 — (Tuỳ chọn/bonus) Group X — lớp tập nhóm**
- Labels: `agent-5` `type:feature` `priority:low`
- Mô tả: Chỉ làm nếu còn dư thời gian, không bắt buộc theo scope 5-role hiện tại.

---

### Agent 6 (phần 1) — Leads/CRM

**Issue 6.1 — CRUD Lead**
- Labels: `agent-6` `type:feature` `priority:high`
- Acceptance Criteria:
  - [ ] CRUD `/api/v1/leads`, gán `assigned_sales_id`, chỉ role `SALES`/Admin

**Issue 6.2 — Chuyển đổi Lead → User thật**
- Labels: `agent-6` `type:feature` `priority:high`
- Phụ thuộc: 6.1
- Acceptance Criteria:
  - [ ] `POST /api/v1/leads/{id}/convert` tạo `User` thật, set `converted_user_id`, status `CONVERTED`
  - [ ] Không cho convert 2 lần (`LEAD_ALREADY_CONVERTED`)
  - [ ] Trigger gọi Issue 5.4 (Commission Sales)

**Issue 6.3 — Workflow hội viên sắp hết hạn**
- Labels: `agent-6` `type:feature` `priority:medium`
- Phụ thuộc: Issue 2.5 (Agent 2)
- Acceptance Criteria:
  - [ ] `GET /api/v1/leads/expiring-soon` liệt kê member có `end_date` trong N ngày tới (N cấu hình được)

---

## Milestone 5 — Dashboard, Open API & Hoàn thiện (Tuần 10)

### Agent 6 (phần 2) — Dashboard & Open API

**Issue 6.4 — API báo cáo Dashboard**
- Labels: `agent-6` `type:feature` `priority:high`
- Phụ thuộc: hầu hết agent khác đã xong dữ liệu nguồn
- Acceptance Criteria:
  - [ ] `GET /api/v1/reports/revenue`, `/renewal-rate`, `/checkin-traffic`, `/top-packages`, `/commissions-summary` trả đúng số liệu test

**Issue 6.5 — Hoàn thiện Swagger UI/OpenAPI toàn dự án**
- Labels: `agent-6` `type:docs` `priority:high`
- Acceptance Criteria:
  - [ ] Mọi endpoint có `@Operation` mô tả rõ, group theo tag đúng module
  - [ ] Rà soát toàn bộ response đều qua `ApiResponse<T>`

**Issue 6.6 — (Tuỳ chọn) Zalo ZNS/SMS nhắc gia hạn tự động**
- Labels: `agent-6` `type:feature` `priority:low`

### Cross-cutting — Tất cả agent

**Issue X.1 — Buffer fix bug tồn đọng toàn dự án**
- Labels: `type:bug` `priority:high`
- Mô tả: Dành 100% Agent trong tuần cuối để fix bug phát sinh từ demo thử/test chéo giữa các module.

**Issue X.2 — Test tích hợp end-to-end đủ 5 vai trò**
- Labels: `type:test` `priority:high`
- Acceptance Criteria:
  - [ ] Kịch bản demo trọn vẹn: Sales tạo Lead → convert → Lễ tân bán gói → Member đặt lịch PT → Member face check-in → PT hoàn thành buổi → Commission tự tính → Lễ tân bán POS → Admin xem Dashboard

---

## Cách tạo hàng loạt lên GitHub (tuỳ chọn)

Nếu có sẵn [GitHub CLI](https://cli.github.com/) (`gh`) đã đăng nhập:

```bash
# 1. Tạo 5 milestone (M1 đánh dấu closed vì đã xong)
gh api repos/:owner/:repo/milestones -f title="M2 - Membership/Payment & Booking Engine" -f state=open
gh api repos/:owner/:repo/milestones -f title="M3 - Face Attendance & Chấm công" -f state=open
gh api repos/:owner/:repo/milestones -f title="M4 - POS, Commission & Leads/CRM" -f state=open
gh api repos/:owner/:repo/milestones -f title="M5 - Dashboard, Open API & Hoàn thiện" -f state=open

# 2. Tạo issue mẫu (lặp lại cho từng issue ở trên, đổi --title/--body/--label/--milestone)
gh issue create \
  --title "CRUD Package" \
  --body "Xem chi tiết Issue 2.1 trong ISSUES.md" \
  --label "agent-2,type:feature,priority:high" \
  --milestone "M2 - Membership/Payment & Booking Engine"
```

> Báo mình nếu muốn mình viết sẵn 1 script bash lặp qua toàn bộ ~40 issue ở trên tự động — hiện file này để bạn copy tay hoặc dùng làm nội dung tham chiếu khi tạo issue.
