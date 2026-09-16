# DATABASE_SCHEMA.md — Thiết kế Cơ sở Dữ liệu

> Toàn bộ bảng bên dưới tương ứng với migration `V1`–`V6` trong `src/main/resources/db/migration/`, chia theo module phụ trách (xem `TASK_ASSIGNMENT.md`).

## ERD tổng quan

```
[roles] >──< [user_roles] >──< [users] ──< [branches]
                                  │
        ┌─────────────────────────┼──────────────────────────────┐
        │                         │                                │
        ▼                         ▼                                ▼
 [member_packages] >──[packages]  [check_ins]                 [pt_bookings] >──[pt_schedules]
        │                              │                            │
        ├──< [freeze_history]          │                       [commissions]
        │                              │
        └──< [payment_transactions]    │
                                        │
 [group_x_classes] >──< [class_bookings]
 [products] ──< [pos_order_items] >──[pos_orders]
 [leads] ──(convert)──> [users]
 [notifications]
```

## Chi tiết bảng

### `roles` (Agent 1)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| name | VARCHAR(50) UNIQUE | SUPER_ADMIN, RECEPTIONIST, TRAINER, SALES, MEMBER |
| description | TEXT | |

### `users` (Agent 1)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| full_name | VARCHAR(150) | |
| email | VARCHAR(150) UNIQUE | |
| phone | VARCHAR(20) UNIQUE | |
| password_hash | VARCHAR(255) | BCrypt |
| status | VARCHAR(20) | ACTIVE, INACTIVE, BANNED |
| avatar_url | VARCHAR(255) | nullable |
| created_at / updated_at | TIMESTAMP | |

### `user_roles` (Agent 1)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| user_id | UUID FK → users | Composite PK |
| role_id | UUID FK → roles | Composite PK |

### `branches` (Agent 1)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| name | VARCHAR(150) | |
| address | VARCHAR(255) | |
| phone | VARCHAR(20) | |
| status | VARCHAR(20) | ACTIVE, CLOSED |

---

### `packages` (Agent 2)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| name | VARCHAR(150) | |
| description | TEXT | |
| price | DECIMAL(12,2) | |
| duration_days | INT | nullable (null nếu SESSION_BASED thuần) |
| session_count | INT | nullable (null nếu TIME_BASED thuần) |
| package_type | VARCHAR(20) | TIME_BASED, SESSION_BASED, PT_1ON1 |
| peak_type | VARCHAR(20) | OFF_PEAK, FULL_TIME |
| is_active | BOOLEAN | |

### `member_packages` (Agent 2)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → users | |
| package_id | UUID FK → packages | |
| branch_id | UUID FK → branches | |
| start_date / end_date | DATE | |
| remaining_sessions | INT | nullable |
| status | VARCHAR(20) | ACTIVE, EXPIRED, FROZEN, PENDING |
| total_frozen_days | INT | default 0 |
| created_at / updated_at | TIMESTAMP | |

### `freeze_history` (Agent 2)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| member_package_id | UUID FK | |
| freeze_start / freeze_end | DATE | |
| days | INT | |
| reason | VARCHAR(255) | nullable |
| created_at | TIMESTAMP | |

---

### `check_ins` (Agent 3)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → users | |
| member_package_id | UUID FK → member_packages | nullable nếu bị từ chối trước khi xác định gói |
| branch_id | UUID FK → branches | |
| checkin_time | TIMESTAMP | |
| method | VARCHAR(20) | QR, CARD, FACE |
| status | VARCHAR(20) | SUCCESS, DENIED_EXPIRED, DENIED_TIME, DENIED_NO_SESSION, DENIED_ANTI_PASSBACK |
| device_id | VARCHAR(100) | nullable |

---

### `payment_transactions` (Agent 4)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → users | |
| related_type | VARCHAR(20) | MEMBERSHIP, POS |
| related_id | UUID | id của member_package hoặc pos_order |
| amount | DECIMAL(12,2) | |
| payment_method | VARCHAR(20) | VIETQR, MOMO, CASH |
| transaction_code | VARCHAR(100) UNIQUE | dùng cho idempotency |
| status | VARCHAR(20) | PENDING, SUCCESS, FAILED, EXPIRED |
| gateway_response | JSONB | raw response lưu để đối soát |
| created_at / updated_at | TIMESTAMP | |

### `products` (Agent 4)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| name | VARCHAR(150) | |
| category | VARCHAR(50) | DRINK, SUPPLEMENT, ACCESSORY |
| price | DECIMAL(12,2) | |
| stock_quantity | INT | |
| unit | VARCHAR(20) | |
| is_active | BOOLEAN | |

### `pos_orders` / `pos_order_items` (Agent 4)
| Bảng | Cột chính |
|---|---|
| pos_orders | id, user_id (nullable — khách vãng lai), staff_id, branch_id, total_amount, payment_method, status, created_at |
| pos_order_items | id, order_id FK, product_id FK, quantity, unit_price, subtotal |

---

### `pt_schedules` (Agent 5)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| trainer_id | UUID FK → users | |
| type | VARCHAR(20) | RECURRING, SPECIFIC_DATE |
| day_of_week | INT | nullable, dùng khi RECURRING |
| specific_date | DATE | nullable, dùng khi SPECIFIC_DATE |
| start_time / end_time | TIME | |
| is_available | BOOLEAN | |

### `pt_bookings` (Agent 5)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| member_id | UUID FK → users | |
| pt_id | UUID FK → users | |
| branch_id | UUID FK → branches | |
| booking_time | TIMESTAMP | |
| duration_minutes | INT | |
| status | VARCHAR(20) | PENDING, COMPLETED, CANCELLED |
| confirmed_by_member / confirmed_by_pt | BOOLEAN | |
| commission_amount | DECIMAL(12,2) | nullable, set khi COMPLETED |

### `group_x_classes` / `class_bookings` (Agent 5)
| Bảng | Cột chính |
|---|---|
| group_x_classes | id, name, trainer_id FK, branch_id FK, room, start_time, end_time, max_slots, current_slots, status |
| class_bookings | id, class_id FK, user_id FK, status (BOOKED/CANCELLED), booked_at |

### `commissions` (Agent 5)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| staff_id | UUID FK → users | PT hoặc Sales |
| type | VARCHAR(20) | PT, SALES |
| source_type | VARCHAR(20) | PT_BOOKING, PACKAGE_SALE |
| source_id | UUID | |
| amount | DECIMAL(12,2) | |
| status | VARCHAR(20) | PENDING, PAID |
| paid_at | TIMESTAMP | nullable |
| created_at | TIMESTAMP | |

---

### `leads` (Agent 6)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| full_name | VARCHAR(150) | |
| phone | VARCHAR(20) | |
| source | VARCHAR(50) | FACEBOOK, WALK_IN, REFERRAL... |
| status | VARCHAR(20) | NEW, CONTACTED, CONVERTED, LOST |
| assigned_sales_id | UUID FK → users | nullable |
| notes | TEXT | nullable |
| created_at | TIMESTAMP | |

### `notifications` (Agent 6)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → users | |
| channel | VARCHAR(20) | SMS, ZALO_ZNS |
| type | VARCHAR(30) | RENEWAL_REMINDER, PAYMENT_SUCCESS, BOOKING_CONFIRM |
| content | TEXT | |
| status | VARCHAR(20) | SENT, FAILED |
| sent_at | TIMESTAMP | |

## Quy ước chung

- Khoá chính: **UUID** cho mọi bảng (tránh lộ số lượng bản ghi, dễ hợp nhất dữ liệu multi-branch sau này).
- Mọi bảng nghiệp vụ chính có `created_at`; bảng có vòng đời cập nhật có thêm `updated_at`.
- Tên bảng/cột: `snake_case`; tên enum lưu dạng `VARCHAR` (không dùng Postgres native enum) để dễ mở rộng giá trị mà không cần migration đổi kiểu.
- Mỗi module migration là 1 file riêng (`V1`…`V6`) đúng theo agent sở hữu — **không sửa migration đã merge**, chỉ thêm migration mới nếu cần đổi schema.
