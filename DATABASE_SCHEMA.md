# DATABASE_SCHEMA.md — Thiết kế & Từ điển cơ sở dữ liệu (đầy đủ 19 bảng, thiết kế mục tiêu v2)

> **Vai trò file này:** nguồn thiết kế chính của database (kiểu dữ liệu, độ dài, nullable, khoá, chỉ mục, quy tắc nghiệp vụ). Sơ đồ trực quan và trạng thái triển khai nằm ở `docs/ERD_TONG_QUAT.md`.
> Mỗi bảng thuộc đúng 1 migration trong `src/main/resources/db/migration/`. Không sửa migration đã merge `master` — mọi thay đổi sau này là migration **mới**. Ngoại lệ: `V3`/`V4` chưa merge `master` thì được sửa thẳng.
>
> **Thiết kế mục tiêu v2 (Phi duyệt 2026-10-16):** 19 bảng, 28 khoá ngoại, gồm 5 thay đổi CH1–CH5 so với DB đang chạy (mục 4). Cột đánh dấu 🆕 v2 chưa có trong DB đang chạy.
>
> **Nguồn đối chiếu (cập nhật 2026-10-16):** các bảng `V1`–`V2_2` lấy từ DB thật (v1, sau đó áp dụng CH1–CH5) (xuất bằng `INFORMATION_SCHEMA`/`sys.indexes`/`sys.foreign_keys`) và khớp từng migration; `V3`, `V4` lấy từ file migration trên nhánh của Agent 3 và 4; `V5`, `V6` là thiết kế (chưa có migration).
>
> **Ký hiệu:** ✅ đã merge · ⚠️ migration thật nằm trên nhánh riêng · 📐 chỉ là thiết kế.

## 1. Quy ước chung

- **Engine:** Microsoft SQL Server 2022, T-SQL thuần. `UUID` → `UNIQUEIDENTIFIER` (`NEWID()`), timestamp → `DATETIME2` (`SYSUTCDATETIME()`), mảng JSON → `NVARCHAR(MAX)`, boolean → `BIT`.
- **Text:** có dấu tiếng Việt → `NVARCHAR` (chèn dữ liệu thủ công phải có tiền tố `N'...'`); mã/email/số điện thoại/enum (ASCII) → `VARCHAR`.
- **Khoá chính:** `id UNIQUEIDENTIFIER` cho mọi bảng (trừ `user_roles` dùng khoá kép).
- **Enum trạng thái:** lưu `VARCHAR`, không dùng `CHECK` — dễ mở rộng.
- **`created_at` + `updated_at`:** mọi bảng có đủ cả hai cột vì entity `extends BaseEntity`. Thiếu một cột là lỗi Hibernate `validate` lúc khởi động (đã xảy ra với `payment_transactions` và `staff_attendances`, `mvn verify` không bắt được).
- **Timezone:** xử lý theo `Asia/Ho_Chi_Minh` ở tầng ứng dụng; `BaseEntity` dùng `LocalDateTime` (không dùng `Instant`).
- **`ON DELETE`:** mặc định `NO ACTION`; chỉ `user_roles` và `user_profiles` (theo `users`) và `pos_order_items` (theo `pos_orders`) dùng `CASCADE`. Hệ quả: user đã có gói/thanh toán/booking thì **không xoá cứng được** — dùng xoá mềm qua `users.status`.
- **Quan hệ không có FK cứng:** chỉ còn liên kết logic `trainer_schedules` ↔ `bookings`. Ở DB đang chạy (v1) còn `payment_transactions.related_id`, bỏ ở V7 (CH1).

## 2. Bảng migration

| Migration | Agent | Bảng | Trạng thái |
|---|---|---|---|
| `V1__init_schema.sql` | 1 | `roles`, `users`, `user_roles`, `branches` | ✅ Đã merge `master`, đã chạy trên DB (v2, CH4: bỏ `users.avatar_url` ở V9) |
| `V2__membership.sql` | 2 | `packages`, `member_packages` | ✅ Đã merge `master`, đã chạy trên DB |
| `V2_1__user_profiles.sql` | 1 | `user_profiles` | ✅ Đã merge `master`, đã chạy trên DB (v2, CH3: `age` → `date_of_birth` ở V9) |
| `V2_2__payment_transactions.sql` | 2 | `payment_transactions` | ✅ Đã merge `master`, đã chạy trên DB (v2, CH1: tách khoá ngoại ở V7, sau V5) |
| `V3__schedule_booking.sql` | 3 | `trainer_schedules`, `bookings` | ⚠️ Có migration thật trên nhánh `feature/agent3-schedule-booking`, chưa merge `master` |
| `V4__face_attendance.sql` | 4 | `face_profiles`, `staff_attendances` | ⚠️ Có migration thật trên nhánh `feature/agent4-face-attendance`, chưa merge `master` |
| `V5__pos_commission.sql` | 5 | `products`, `pos_orders`, `pos_order_items`, `trainer_pay_rates`, `commissions` | 📐 Chưa có migration — thiết kế để Agent 5 bám theo (đã bỏ `total_amount`, `subtotal` theo CH5) |
| `V6__leads.sql` | 6 | `leads` | 📐 Chưa có migration — thiết kế để Agent 6 bám theo |
| `V8__gym_check_ins.sql` | 4 | `gym_check_ins` | 📐 Thiết kế v2 (CH2), triển khai sau khi PR #43 của Agent 4 merge |

---

## 3. Từ điển cột

### `V1__init_schema.sql` — Agent 1

✅ Đã merge `master`, đã chạy trên DB (v2, CH4: bỏ `users.avatar_url` ở V9)

#### `roles`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `name` | VARCHAR(50) | NO |  | UNIQUE. 5 giá trị seed: `SUPER_ADMIN`, `RECEPTIONIST`, `TRAINER`, `SALES`, `MEMBER` |
| `description` | NVARCHAR(255) | YES |  |  |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

#### `users`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `full_name` | NVARCHAR(150) | NO |  |  |
| `email` | VARCHAR(150) | NO |  | UNIQUE |
| `phone` | VARCHAR(20) | YES |  | UNIQUE ⚠️ cột nullable nhưng UNIQUE kiểu constraint: chỉ cho **một** user có `NULL` (xem mục 5, vấn đề #1) |
| `password_hash` | VARCHAR(255) | NO |  | Mã băm mật khẩu, không bao giờ trả ra API |
| `status` | VARCHAR(20) | NO | 'ACTIVE' | `ACTIVE` / `INACTIVE` / `BANNED` (theo ERD, chưa đối chiếu enum trong code) |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_users_email` trên `(email)`; INDEX `idx_users_status` trên `(status)`.

#### `user_roles`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `user_id` | UNIQUEIDENTIFIER | NO |  | **PK** (khoá kép); FK → `users(id)` ON DELETE CASCADE |
| `role_id` | UNIQUEIDENTIFIER | NO |  | **PK** (khoá kép); FK → `roles(id)` ON DELETE CASCADE |

#### `branches`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `name` | NVARCHAR(150) | NO |  |  |
| `address` | NVARCHAR(255) | YES |  |  |
| `phone` | VARCHAR(20) | YES |  | ⚠️ dữ liệu seed là chữ giữ chỗ (xem mục 5, vấn đề #3) |
| `status` | VARCHAR(20) | NO | 'ACTIVE' | `ACTIVE` / `CLOSED` |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

### `V2__membership.sql` — Agent 2

✅ Đã merge `master`, đã chạy trên DB

#### `packages`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `name` | NVARCHAR(150) | NO |  |  |
| `description` | NVARCHAR(500) | YES |  |  |
| `price` | DECIMAL(12,2) | NO |  |  |
| `duration_days` | INT | YES |  |  |
| `session_count` | INT | YES |  |  |
| `package_type` | VARCHAR(20) | NO |  | `TIME_BASED` / `SESSION_BASED` / `PT_1ON1` |
| `peak_type` | VARCHAR(20) | NO | 'FULL_TIME' | `OFF_PEAK` / `FULL_TIME` |
| `max_sessions_per_week` | INT | YES |  | NULL = không giới hạn; 0 = không cho đặt PT; N = tối đa N buổi/tuần (tuần Thứ 2 → Chủ nhật) |
| `is_active` | BIT | NO | 1 |  |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

#### `member_packages`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `user_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)` |
| `package_id` | UNIQUEIDENTIFIER | NO |  | FK → `packages(id)` |
| `branch_id` | UNIQUEIDENTIFIER | YES |  | FK → `branches(id)` |
| `status` | VARCHAR(20) | NO | 'PENDING' | `PENDING` → `ACTIVE` → `EXPIRED` (lazy-check); `SCHEDULED` cho hạ cấp; `CANCELLED`. "Gói hiện tại" = bản ghi duy nhất `ACTIVE` |
| `start_date` | DATE | NO |  | `PENDING`: dự kiến; `ACTIVE`: ngày thanh toán thật; `SCHEDULED`: ngày ngay sau khi gói hiện tại hết hạn |
| `end_date` | DATE | YES |  |  |
| `remaining_sessions` | INT | YES |  | Trừ 1 khi booking chốt `COMPLETED` / `MEMBER_NO_SHOW` / `LATE_CANCEL` |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_member_packages_status` trên `(status)`; INDEX `idx_member_packages_user` trên `(user_id)`.

### `V2_1__user_profiles.sql` — Agent 1

✅ Đã merge `master`, đã chạy trên DB (v2, CH3: `age` → `date_of_birth` ở V9)

#### `user_profiles`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `user_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)` ON DELETE CASCADE; UNIQUE |
| `date_of_birth` | DATE | YES |  | 🆕 v2 (CH3): thay cho `age`. Ngày sinh phải cách `users.created_at` ít nhất 18 năm |
| `weight_kg` | DECIMAL(5,2) | YES |  |  |
| `height_cm` | DECIMAL(5,2) | YES |  |  |
| `goal` | NVARCHAR(255) | YES |  |  |
| `avatar_url` | VARCHAR(255) | YES |  | Nguồn duy nhất của ảnh đại diện |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

### `V2_2__payment_transactions.sql` — Agent 2

✅ Đã merge `master`, đã chạy trên DB (v2, CH1: tách khoá ngoại ở V7, sau V5)

#### `payment_transactions`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `user_id` | UNIQUEIDENTIFIER | YES |  | FK → `users(id)`; 🆕 v2: nullable, NULL = khách vãng lai mua lẻ tại quầy |
| `member_package_id` | UNIQUEIDENTIFIER | YES |  | FK → `member_packages(id)`; 🆕 v2 (CH1): thanh toán gói tập. **Đúng một** trong hai cột `member_package_id`/`pos_order_id` có giá trị (CHECK) |
| `pos_order_id` | UNIQUEIDENTIFIER | YES |  | FK → `pos_orders(id)`; 🆕 v2 (CH1): thanh toán đơn bán lẻ (FK thêm ở V7, sau khi V5 có `pos_orders`) |
| `amount` | DECIMAL(12,2) | NO |  |  |
| `payment_method` | VARCHAR(20) | NO |  | `CASH` / `LOCAL_CONFIRM` |
| `status` | VARCHAR(20) | NO | 'PENDING' | `PENDING` / `SUCCESS` / `FAILED` |
| `transaction_code` | VARCHAR(100) | NO |  | UNIQUE, NOT NULL — dùng cho idempotency |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_payment_transactions_user` trên `(user_id)`; INDEX `idx_payment_transactions_member_package` trên `(member_package_id)`; INDEX `idx_payment_transactions_pos_order` trên `(pos_order_id)`.

### `V3__schedule_booking.sql` — Agent 3

⚠️ Có migration thật trên nhánh `feature/agent3-schedule-booking`, chưa merge `master`

#### `trainer_schedules`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `trainer_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)` |
| `work_date` | DATE | NO |  | **Ngày cụ thể**, không phải thứ lặp lại hàng tuần |
| `start_time` | TIME | NO |  |  |
| `end_time` | TIME | NO |  |  |
| `is_active` | BIT | NO | 1 | `0` = ca "Tạm nghỉ" |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_trainer_schedule_trainer_date` trên `(trainer_id, work_date)`.

#### `bookings`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `member_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)` |
| `trainer_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)` |
| `branch_id` | UNIQUEIDENTIFIER | YES |  | FK → `branches(id)` |
| `booking_date` | DATE | NO |  |  |
| `start_time` | TIME | NO |  |  |
| `end_time` | TIME | NO |  |  |
| `status` | VARCHAR(20) | NO | 'PENDING' | `PENDING` / `CONFIRMED` / `COMPLETED` / `CANCELLED` / `NO_SHOW` / `PT_NO_SHOW` / `LATE_CANCELLED` |
| `check_in_time` | DATETIME2 | YES |  | Học viên quét mặt vào |
| `check_in_method` | VARCHAR(20) | YES |  | `FACE` |
| `check_out_time` | DATETIME2 | YES |  | Học viên quét mặt ra |
| `check_out_method` | VARCHAR(20) | YES |  | `FACE` |
| `pt_check_in_time` | DATETIME2 | YES |  | PT quét mặt vào buổi dạy (đã có trong `V3` trên nhánh Agent 3) |
| `pt_check_in_method` | VARCHAR(20) | YES |  | `FACE` |
| `pt_check_out_time` | DATETIME2 | YES |  | PT quét mặt ra |
| `pt_check_out_method` | VARCHAR(20) | YES |  | `FACE` |
| `pt_early_leave_reason` | NVARCHAR(255) | YES |  | Lý do PT ra sớm hơn giờ kết thúc; trống = coi là không dạy đủ giờ → không tính lương |
| `notes` | NVARCHAR(500) | YES |  |  |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_booking_trainer_date` trên `(trainer_id, booking_date)`; INDEX `idx_booking_member_date` trên `(member_id, booking_date)`.

### `V4__face_attendance.sql` — Agent 4

⚠️ Có migration thật trên nhánh `feature/agent4-face-attendance`, chưa merge `master`

#### `face_profiles`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `user_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)`; UNIQUE |
| `descriptor` | NVARCHAR(MAX) | NO |  | JSON array 128 số thực (vector đặc trưng khuôn mặt) |
| `reference_image` | VARCHAR(255) | YES |  | Đường dẫn ảnh gốc làm bằng chứng (không dùng để so khớp) |
| `registered_by` | UNIQUEIDENTIFIER | YES |  | FK → `users(id)`; Ai đã đăng ký/đăng ký lại hồ sơ này |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

#### `staff_attendances`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `staff_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)` |
| `date` | DATE | NO |  | Tên cột trùng từ khoá: viết `[date]` trong SQL thủ công |
| `check_in_time` | DATETIME2 | YES |  |  |
| `check_out_time` | DATETIME2 | YES |  |  |
| `method` | VARCHAR(20) | NO | 'MANUAL' | `MANUAL` / `FACE` |
| `notes` | NVARCHAR(255) | YES |  |  |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

**Ràng buộc/chỉ mục thêm:** UNIQUE `(staff_id, date)`; INDEX `idx_staff_attendances_date` trên `(date)`.

### `V5__pos_commission.sql` — Agent 5

📐 Chưa có migration — thiết kế để Agent 5 bám theo (đã bỏ `total_amount`, `subtotal` theo CH5)

#### `products`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `name` | NVARCHAR(150) | NO |  |  |
| `category` | VARCHAR(50) | NO |  | `DRINK` / `SUPPLEMENT` / `ACCESSORY` |
| `price` | DECIMAL(12,2) | NO |  |  |
| `stock_quantity` | INT | NO | 0 |  |
| `unit` | NVARCHAR(20) | YES |  | NVARCHAR (đơn vị tiếng Việt như "hộp", "chai"); thiết kế gốc ghi VARCHAR là chưa đúng |
| `is_active` | BIT | NO | 1 |  |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_products_category` trên `(category)`.

#### `pos_orders`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `member_id` | UNIQUEIDENTIFIER | YES |  | FK → `users(id)`; NULL = khách vãng lai |
| `staff_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)` |
| `branch_id` | UNIQUEIDENTIFIER | YES |  | FK → `branches(id)` |
| `payment_method` | VARCHAR(20) | NO |  | Giống `payment_transactions.payment_method` |
| `status` | VARCHAR(20) | NO | 'PENDING' | Đề xuất: `PENDING` (chờ thanh toán) → `COMPLETED` / `CANCELLED`. **Agent 5 xác nhận** |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_pos_orders_branch_created` trên `(branch_id, created_at)`.

#### `pos_order_items`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `order_id` | UNIQUEIDENTIFIER | NO |  | FK → `pos_orders(id)` ON DELETE CASCADE |
| `product_id` | UNIQUEIDENTIFIER | NO |  | FK → `products(id)` |
| `quantity` | INT | NO |  | > 0 |
| `unit_price` | DECIMAL(12,2) | NO |  | Giá tại thời điểm bán (không đổi khi `products.price` đổi) |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_pos_order_items_order` trên `(order_id)`; INDEX `idx_pos_order_items_product` trên `(product_id)`.

#### `trainer_pay_rates`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `trainer_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)`; UNIQUE — 1 PT 1 mức lương |
| `amount_per_session` | DECIMAL(12,2) | NO |  | Lương mỗi buổi dạy |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

#### `commissions`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `staff_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)`; PT (hoa hồng Sales đã bỏ, Sales hưởng lương cứng) |
| `booking_id` | UNIQUEIDENTIFIER | NO |  | FK → `bookings(id)`; UNIQUE — mỗi buổi tối đa 1 bản ghi (chống tính hai lần) |
| `outcome` | VARCHAR(20) | NO |  | `COMPLETED` / `MEMBER_NO_SHOW` / `LATE_CANCEL` |
| `amount` | DECIMAL(12,2) | NO |  | Lấy từ `trainer_pay_rates` tại thời điểm chốt buổi |
| `status` | VARCHAR(20) | NO | 'PENDING' | `PENDING` / `PAID` |
| `paid_at` | DATETIME2 | YES |  |  |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_commissions_staff_status` trên `(staff_id, status)`.

### `V6__leads.sql` — Agent 6

📐 Chưa có migration — thiết kế để Agent 6 bám theo

#### `leads`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `full_name` | NVARCHAR(150) | NO |  |  |
| `phone` | VARCHAR(20) | NO |  | Không UNIQUE (cho phép trùng lead); có chỉ mục để tra cứu |
| `source` | NVARCHAR(50) | YES |  | NVARCHAR vì nguồn có thể là chữ tiếng Việt; thiết kế gốc ghi VARCHAR |
| `status` | VARCHAR(20) | NO | 'NEW' | `NEW` / `CONTACTED` / `CONVERTED` / `LOST` |
| `assigned_sales_id` | UNIQUEIDENTIFIER | YES |  | FK → `users(id)` |
| `converted_user_id` | UNIQUEIDENTIFIER | YES |  | FK → `users(id)`; Set khi `CONVERTED` |
| `notes` | NVARCHAR(500) | YES |  |  |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_leads_status` trên `(status)`; INDEX `idx_leads_phone` trên `(phone)`.

### `V8__gym_check_ins.sql` — Agent 4

📐 Thiết kế v2 (CH2), triển khai sau khi PR #43 của Agent 4 merge

#### `gym_check_ins`

| Cột | Kiểu | Null | Mặc định | Khoá / Ghi chú |
|---|---|---|---|---|
| `id` | UNIQUEIDENTIFIER | NO | NEWID() | **PK** |
| `member_id` | UNIQUEIDENTIFIER | NO |  | FK → `users(id)`; Hội viên vào phòng tập |
| `branch_id` | UNIQUEIDENTIFIER | YES |  | FK → `branches(id)`; Chi nhánh (nullable) |
| `check_in_time` | DATETIME2 | NO |  | Lượt quét mặt vào |
| `check_out_time` | DATETIME2 | YES |  | Lượt quét thứ 2 trong ngày = check-out |
| `method` | VARCHAR(20) | NO | 'FACE' | `FACE` / `MANUAL` |
| `created_at` | DATETIME2 | NO | SYSUTCDATETIME() |  |
| `updated_at` | DATETIME2 | YES |  |  |

**Ràng buộc/chỉ mục thêm:** INDEX `idx_gym_check_ins_member_time` trên `(member_id, check_in_time)`.

---

## 4. Thay đổi từ DB đang chạy (v1) sang thiết kế mục tiêu (v2)

Đã được Phi duyệt ngày 2026-10-16, **chưa triển khai**. Chi tiết quyết định: `DECISIONS_DB_V2.md`.

| # | Thay đổi | Lý do | Migration | Agent |
|---|---|---|---|---|
| CH1 | `payment_transactions`: bỏ `related_type`, `related_id`; thêm `member_package_id`, `pos_order_id` (FK, đúng một cột có giá trị, `CHECK`); `user_id` nullable | `related_id` trỏ hai bảng nên không có FK; khách vãng lai mua lẻ không có `user_id` | V7 (sau V5) | 2 |
| CH2 | Bảng mới `gym_check_ins` | Lượt vào phòng không có buổi PT chưa có chỗ lưu (lịch sử check-in, báo cáo lưu lượng) | V8 | 4 |
| CH3 | `user_profiles.age` → `date_of_birth` | Tuổi là dữ liệu suy ra, lỗi thời theo thời gian | V9 | 1 |
| CH4 | Bỏ `users.avatar_url` | Trùng `user_profiles.avatar_url` | V9 | 1 |
| CH5 | Bỏ `pos_orders.total_amount`, `pos_order_items.subtotal` | Suy ra từ `quantity × unit_price` | V5 (viết đúng ngay từ đầu) | 5 |

## 5. Vấn đề đã phát hiện khi đối chiếu DB thật (đề xuất xử lý)

1. **`users.phone` — UNIQUE trên cột nullable (đã xác nhận).** `V1` khai báo `phone VARCHAR(20) NULL UNIQUE`; SQL Server chỉ cho phép **một** dòng `NULL` trong cột UNIQUE kiểu constraint. User thứ hai không nhập số điện thoại sẽ bị lỗi vi phạm unique. Hiện DB chỉ có 1 user nên chưa lộ. Cách sửa là migration mới thay constraint bằng filtered unique index. Tên constraint do SQL Server tự sinh và khác nhau giữa các máy nên phải tìm bằng SQL động (**bản nháp, chưa chạy thử**):
   ```sql
   DECLARE @uq sysname;
   SELECT @uq = kc.name FROM sys.key_constraints kc
   JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
   JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
   WHERE kc.parent_object_id = OBJECT_ID('users') AND kc.type = 'UQ' AND c.name = 'phone';
   IF @uq IS NOT NULL EXEC('ALTER TABLE users DROP CONSTRAINT [' + @uq + ']');
   CREATE UNIQUE INDEX uq_users_phone ON users(phone) WHERE phone IS NOT NULL;
   ```
   Migration này là **V9** (cùng CH3, CH4); `spring.flyway.out-of-order: true` đã bật ở môi trường dev nên không lệch thứ tự với `V3`–`V8` đang chờ. `payment_transactions.transaction_code` là `NOT NULL UNIQUE` nên **không** dính lỗi này.
2. **`avatar_url` trùng ở `users` và `user_profiles`.** CH4 đã duyệt: bỏ `users.avatar_url` ở V9; nguồn duy nhất là `user_profiles.avatar_url`.
3. **Dữ liệu seed `branches`:** `address` và `phone` của chi nhánh mặc định là chữ giữ chỗ "Đang cập nhật". Riêng `phone` là `VARCHAR` nên dấu tiếng Việt bị biến dạng. Nên đặt `phone = NULL` bằng cập nhật dữ liệu khi thiết lập chi nhánh thật.
4. **Chỉ mục thừa:** `idx_users_email` trùng với chỉ mục mà constraint UNIQUE của `email` đã tạo, nên có thể bỏ ở migration dọn dẹp sau.
5. **Chỉ mục nên bổ sung:** `member_packages (user_id, status)` (mỗi lần gọi `getCurrentMembership`, hiện chỉ có hai chỉ mục đơn lẻ); `user_roles (role_id)` (khoá kép chỉ dẫn đầu bằng `user_id`). `payment_transactions (related_type, related_id)` **đã có**.
6. **`updated_at` không đồng nhất:** `trainer_schedules`, `bookings` là `NOT NULL DEFAULT SYSUTCDATETIME()`, các bảng còn lại là `NULL`. Không gây lỗi; thống nhất khi tiện (bảng mới nên theo `NULL` như đa số).
7. **Quyết định đã chốt (2026-10-12):** bỏ "chốt ca" thu ngân khỏi phạm vi (không có bảng `cash_shifts`); không tạo bảng `staff_shifts` — ca mặc định lấy từ cấu hình `gms.face-attendance.default-shift-start/end`.
8. **`V4` chưa có comment đầu file** như các migration khác; `V2_1` còn comment cũ ghi tên `V2b`. Chỉ ảnh hưởng đọc hiểu.
