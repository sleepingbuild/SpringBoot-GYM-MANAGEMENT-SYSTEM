# DATABASE_SCHEMA.md — Thiết kế Cơ sở Dữ liệu (bản đầy đủ, khớp `REQUIREMENTS.md`)

> Mỗi bảng dưới đây tương ứng đúng 1 migration trong `src/main/resources/db/migration/`, đặt tên theo agent sở hữu (xem `TASK_ASSIGNMENT.md`). Không sửa lại migration đã merge — mọi thay đổi schema sau này là migration **mới**.

## Quy ước chung

- **Engine:** Microsoft SQL Server 2022. `UUID` (logic) → `UNIQUEIDENTIFIER` (`NEWID()`), `TIMESTAMP` → `DATETIME2` (`SYSUTCDATETIME()`), `TEXT`/mảng JSON → `NVARCHAR(MAX)`, `BOOLEAN` → `BIT`. Text có dấu tiếng Việt → `NVARCHAR`; mã/email/enum trạng thái (ASCII) → `VARCHAR`.
- **Khoá chính:** UUID cho mọi bảng.
- **Timezone:** mọi timestamp lưu và xử lý nhất quán theo `Asia/Ho_Chi_Minh` ở tầng ứng dụng (xem `REQUIREMENTS.md` mục 7).
- **Enum trạng thái:** lưu dạng `VARCHAR`, không dùng CHECK constraint cứng — dễ mở rộng giá trị mà không cần đổi kiểu cột.
- **Numbering migration** (đúng theo `TASK_ASSIGNMENT.md`):

| File | Agent | Nội dung |
|---|---|---|
| `V1__init_schema.sql` | 1 | `roles`, `users`, `user_roles`, `branches` (đã có) |
| `V2__membership.sql` | 2 | `packages`, `member_packages` (đã có bản base, Agent 2 mở rộng CRUD/nghiệp vụ qua service, không cần sửa lại file) |
| `V2b__user_profiles.sql` | 1 | `user_profiles` (đã có) |
| `V3__schedule_booking.sql` | 3 | `trainer_schedules`, `bookings` |
| `V4__face_attendance.sql` | 4 | `face_profiles` (descriptor), `staff_attendances` |
| `V5__pos_commission.sql` | 5 | `products`, `pos_orders`, `pos_order_items`, `commissions` |
| `V6__leads.sql` | 6 | `leads` |

> Điểm danh buổi tập dùng trực tiếp `bookings.check_in_time/check_out_time` (không có bảng `check_ins` riêng) — vì mỗi lượt điểm danh luôn gắn với đúng 1 buổi đã đặt lịch cụ thể (đúng theo cách .NET đã làm, xem `REQUIREMENTS.md` mục 4).

---

## ERD tổng quan

```
[roles] >──< [user_roles] >──< [users] ──< [branches]
                                  │
                    ┌─────────────┼───────────────────────────────┐
                    │             │                                 │
                    ▼             ▼                                 ▼
            [user_profiles]  [member_packages] >──[packages]   [face_profiles]
                                  │
                                  └──< [payment_transactions]

[trainer_schedules] ──(trainer)── [users]
[bookings] ──(member, trainer, branch)── [users]/[branches]
[staff_attendances] ──(staff)── [users]

[products] ──< [pos_order_items] >──[pos_orders] ──(member?, staff)── [users]
[commissions] ──(staff)── [users], nguồn từ [bookings] hoặc [leads]
[leads] ──(assigned_sales, converted_user)── [users]
```

---

## Agent 1 — Core & Profile

### `roles`, `users`, `user_roles`, `branches`
*(Không đổi so với bản trước — xem migration `V1__init_schema.sql` đã có. 5 role: `SUPER_ADMIN`, `RECEPTIONIST`, `SALES`, `TRAINER`, `MEMBER`.)*

### `user_profiles` (mới — `V2b`)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| user_id | UUID FK → users | UNIQUE — 1 user 1 profile |
| age | INT | nullable |
| weight_kg | DECIMAL(5,2) | nullable |
| height_cm | DECIMAL(5,2) | nullable |
| goal | NVARCHAR(255) | nullable — mục tiêu tập luyện |
| avatar_url | VARCHAR(255) | nullable |
| created_at / updated_at | DATETIME2 | |

Validate tuổi ≥18 so với `users.created_at` (ngày đăng ký), không so ngày hiện tại.

---

## Agent 2 — Membership, Package & Payment

### `packages`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| name | NVARCHAR(150) | |
| description | NVARCHAR(500) | nullable |
| price | DECIMAL(12,2) | |
| duration_days | INT | nullable |
| session_count | INT | nullable |
| package_type | VARCHAR(20) | `TIME_BASED` / `SESSION_BASED` / `PT_1ON1` |
| peak_type | VARCHAR(20) | `OFF_PEAK` / `FULL_TIME` |
| max_sessions_per_week | INT | NULL = không giới hạn, 0 = không cho đặt PT, N = tối đa N buổi/tuần |
| is_active | BIT | |

### `member_packages`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| user_id | UUID FK → users | |
| package_id | UUID FK → packages | |
| branch_id | UUID FK → branches | nullable |
| status | VARCHAR(20) | `PENDING` / `ACTIVE` / `EXPIRED` / `SCHEDULED` / `CANCELLED` |
| start_date | DATE | với `PENDING`: dự kiến; với `ACTIVE`: ngày thanh toán thành công thật; với `SCHEDULED`: ngày gói hiện tại hết hạn |
| end_date | DATE | nullable |
| remaining_sessions | INT | nullable |
| created_at / updated_at | DATETIME2 | |

⚠️ Method `getCurrentMembership(userId)` (Agent 2, dùng chung toàn hệ thống) định nghĩa "gói hiện tại" = **bản ghi duy nhất có `status = 'ACTIVE'`** của user đó. Không nơi nào khác được tự query định nghĩa khác.

### `payment_transactions`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| user_id | UUID FK → users | |
| related_type | VARCHAR(20) | `MEMBERSHIP` / `POS` |
| related_id | UUID | id của `member_packages` hoặc `pos_orders` |
| amount | DECIMAL(12,2) | |
| payment_method | VARCHAR(20) | `CASH` / `LOCAL_CONFIRM` / (VietQR/MoMo nếu bổ sung sau) |
| status | VARCHAR(20) | `PENDING` / `SUCCESS` / `FAILED` |
| transaction_code | VARCHAR(100) | UNIQUE, dùng cho idempotency nếu có cổng thanh toán ngoài |
| created_at | DATETIME2 | |

---

## Agent 3 — Lịch làm việc PT & Booking

### `trainer_schedules`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| trainer_id | UUID FK → users | |
| work_date | DATE | **ngày cụ thể**, KHÔNG phải day-of-week lặp lại |
| start_time | TIME | |
| end_time | TIME | |
| is_active | BIT | `false` = ca "Tạm nghỉ" |
| created_at | DATETIME2 | |

Unique gợi ý: không bắt buộc unique cứng ở DB (trùng giờ trong cùng ngày được xử lý ở tầng service bằng thuật toán "lane" khi hiển thị, không phải lỗi).

### `bookings`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| member_id | UUID FK → users | |
| trainer_id | UUID FK → users | |
| branch_id | UUID FK → branches | nullable |
| booking_date | DATE | |
| start_time | TIME | |
| end_time | TIME | |
| status | VARCHAR(20) | `PENDING` / `CONFIRMED` / `COMPLETED` / `CANCELLED` / `NO_SHOW` / `PT_NO_SHOW` |
| check_in_time | DATETIME2 | nullable — set khi member face check-in |
| check_in_method | VARCHAR(20) | `FACE` (mở rộng sau nếu cần) |
| check_out_time | DATETIME2 | nullable |
| check_out_method | VARCHAR(20) | nullable |
| notes | NVARCHAR(500) | nullable |
| created_at / updated_at | DATETIME2 | |

Index gợi ý: `(trainer_id, booking_date)`, `(member_id, booking_date)` — phục vụ check trùng lịch/slot nhanh.

---

## Agent 4 — Face Attendance & Chấm công

### `face_profiles` (đổi từ schema mock ban đầu)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| user_id | UUID FK → users | UNIQUE |
| descriptor | NVARCHAR(MAX) | JSON array 128 số thực (vector đặc trưng khuôn mặt) |
| registered_by | UUID FK → users | nullable — ai đã đăng ký/đăng ký lại |
| created_at / updated_at | DATETIME2 | |

> Nếu muốn lưu thêm ảnh gốc làm bằng chứng/audit (không dùng để so khớp), thêm cột `reference_image` riêng trong cùng migration `V4`.

### `staff_attendances`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| staff_id | UUID FK → users | PT hoặc Lễ tân |
| date | DATE | |
| check_in_time | DATETIME2 | nullable |
| check_out_time | DATETIME2 | nullable |
| method | VARCHAR(20) | `MANUAL` / `FACE` |
| notes | NVARCHAR(255) | nullable |
| created_at | DATETIME2 | |

Unique: `(staff_id, date)`. Trạng thái (Đúng giờ/Đi muộn/Về sớm/Vắng mặt) **tính động lúc đọc**, không lưu cột riêng — so với `shift_start_time`/`shift_end_time` (đề xuất: thêm 2 cột nullable này vào `users` hoặc 1 bảng cấu hình riêng `staff_shifts`, mặc định 07:00–21:00 nếu trống — Agent 4 quyết định cụ thể, ghi migration tương ứng).

---

## Agent 5 — POS & Commission

### `products`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| name | NVARCHAR(150) | |
| category | VARCHAR(50) | `DRINK` / `SUPPLEMENT` / `ACCESSORY` |
| price | DECIMAL(12,2) | |
| stock_quantity | INT | |
| unit | VARCHAR(20) | nullable |
| is_active | BIT | |

### `pos_orders` / `pos_order_items`
| Bảng | Cột chính |
|---|---|
| `pos_orders` | id, member_id (UUID FK → users, nullable — khách vãng lai), staff_id (UUID FK → users), branch_id, total_amount, payment_method, status, created_at |
| `pos_order_items` | id, order_id FK, product_id FK, quantity, unit_price, subtotal |

### `commissions`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| staff_id | UUID FK → users | PT hoặc Sales |
| type | VARCHAR(20) | `PT` / `SALES` |
| source_type | VARCHAR(20) | `BOOKING` / `LEAD_CONVERSION` |
| source_id | UUID | id của `bookings` hoặc `leads` |
| amount | DECIMAL(12,2) | |
| status | VARCHAR(20) | `PENDING` / `PAID` |
| paid_at | DATETIME2 | nullable |
| created_at | DATETIME2 | |

---

## Agent 6 — Leads/CRM

### `leads`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID | |
| full_name | NVARCHAR(150) | |
| phone | VARCHAR(20) | |
| source | VARCHAR(50) | nullable |
| status | VARCHAR(20) | `NEW` / `CONTACTED` / `CONVERTED` / `LOST` |
| assigned_sales_id | UUID FK → users | nullable |
| converted_user_id | UUID FK → users | nullable — set khi `CONVERTED` |
| notes | NVARCHAR(500) | nullable |
| created_at / updated_at | DATETIME2 | |
