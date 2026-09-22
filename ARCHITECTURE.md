# ARCHITECTURE.md — Kiến trúc Hệ thống

## 1. Kiến trúc tổng quan

```
                         ┌──────────────────────┐
                         │   Frontend (React/    │
                         │   Vue/Flutter) — sau   │
                         │   khi backend ổn định  │
                         └──────────┬────────────┘
                                    │ REST/JSON (JWT Bearer)
                                    ▼
        ┌───────────────────────────────────────────────────┐
        │                Spring Boot 3.x API                  │
        │  ┌───────────┐ ┌───────────┐ ┌───────────────────┐ │
        │  │ Controller│→│  Service   │→│    Repository      │ │
        │  └───────────┘ └───────────┘ └─────────┬──────────┘ │
        │        ▲              │                 │            │
        │        │        ┌─────┴──────┐          ▼            │
        │   Spring        │FaceMatch   │   SQL Server 2022     │
        │   Security/JWT  │Service     │                        │
        │        │        │(descriptor │                        │
        │        │        │ + Euclidean)│                       │
        └────────┼────────┴─────┬──────┴────────────────────────┘
                 │               │
                 ▼               ▼
             ┌────────┐   ┌──────────────────────┐
             │ Redis  │   │ (tuỳ chọn, sau v1.0)  │
             │(cache, │   │ Zalo ZNS/SMS nhắc hạn,│
             │ JWT    │   │ VietQR/MoMo nếu cần   │
             │blacklist│  │ demo thanh toán thật  │
             └────────┘   └──────────────────────┘
```

**Nguyên tắc kiến trúc:**
- Layered architecture chuẩn: Controller → Service (interface + impl) → Repository → Entity.
- Mọi business logic nằm ở `service/`, Controller chỉ nhận request/validate/gọi service/trả response.
- Giao tiếp giữa các module nghiệp vụ (vd: Face Attendance cần trừ buổi của Membership, Commission cần đọc trạng thái Booking) **luôn qua Service interface public**, không truy cập trực tiếp Repository của module khác — giữ ranh giới rõ ràng giữa các agent, và là **1 nguồn chân lý duy nhất** cho mỗi khái niệm nghiệp vụ (xem `REQUIREMENTS.md` mục 7).
- Multi-branch: mọi entity nghiệp vụ chính đều có `branch_id` để hỗ trợ SaaS multi-tenant theo chi nhánh.
- Hệ thống là **độc lập hoàn toàn** (không còn phụ thuộc/tích hợp bolt-on vào dự án .NET song song) — JWT tự cấp, tự quản lý toàn bộ vòng đời user.

## 2. Cấu trúc thư mục dự án

```text
gym-management-system/
├── src/
│   ├── main/
│   │   ├── java/com/gym/management/
│   │   │   ├── GymManagementApplication.java
│   │   │   ├── config/               # SecurityConfig, CorsConfig, RedisConfig, OpenApiConfig, WebMvcConfig
│   │   │   ├── controller/           # REST Controllers (1 file / module nghiệp vụ)
│   │   │   ├── dto/
│   │   │   │   ├── request/          # Request DTOs
│   │   │   │   └── response/         # Response DTOs
│   │   │   ├── entity/               # JPA Entities
│   │   │   ├── repository/           # Spring Data JPA Repositories
│   │   │   ├── service/              # Interfaces
│   │   │   │   └── impl/             # Implementations
│   │   │   ├── security/             # JwtTokenProvider, CustomUserDetailsService, JwtAuthenticationFilter, @CurrentUser
│   │   │   ├── integration/
│   │   │   │   ├── hardware/         # FaceMatchService (descriptor + Euclidean distance) — xem REQUIREMENTS.md mục 4
│   │   │   │   ├── vietqr/           # Tuỳ chọn, sau v1.0 nếu cần cổng thanh toán thật
│   │   │   │   └── zalo/             # Tuỳ chọn, sau v1.0 — nhắc gia hạn qua Zalo ZNS/SMS
│   │   │   └── exception/            # GlobalExceptionHandler, ErrorCode, custom exceptions
│   │   └── resources/
│   │       ├── application.yml.example
│   │       ├── db/migration/         # Flyway: V1__..., V2__..., ...
│   │       └── templates/            # Email / notification templates
│   └── test/java/com/gym/management/
├── docs/                              # ERD, sơ đồ bổ sung
├── docker-compose.yml
├── pom.xml
├── REQUIREMENTS.md
├── TASK_ASSIGNMENT.md
├── ARCHITECTURE.md
├── DATABASE_SCHEMA.md
├── API_DESIGN.md
├── ROADMAP.md
├── CONTRIBUTING.md
└── DESIGN_SYSTEM.md
```

**Quy ước sở hữu package theo agent:** xem chi tiết trong `TASK_ASSIGNMENT.md`. Mỗi agent chỉ thêm entity/service/controller trong đúng phạm vi được phân công để build song song không đè code nhau.

## 3. Luồng xử lý cốt lõi

### 3.1 Luồng Face Attendance (điểm danh buổi tập / chấm công)

```
Client trích descriptor khuôn mặt (face-api.js) từ webcam
   │
   ├──> Gửi descriptor (JSON 128 số thực) lên server — KHÔNG gửi kết luận "là ai"
   │
   ├──> Server tính khoảng cách Euclidean với các descriptor đã đăng ký (face_profiles)
   │        ├──> Luồng Kiosk: so với TOÀN BỘ hồ sơ (1:N)
   │        └──> Luồng Self check-in: chỉ so với đúng user đang đăng nhập (1:1)
   │
   ├──> Khoảng cách nhỏ nhất < ngưỡng 0.45? 
   │        ├──> [KHÔNG] → FACE_NOT_RECOGNIZED, không ghi nhận
   │        └──> [CÓ] → xác định được user_id
   │
   ├──> Kiểm tra lượt quét trong ngày của user đó:
   │        ├──> Lần đầu → CHECK-IN (set check_in_time/check_in_method)
   │        └──> Lần 2 → CHECK-OUT (set check_out_time/check_out_method)
   │
   └──> Ghi vào đúng bảng theo vai trò: bookings (Member) hoặc staff_attendances (Trainer/Receptionist)
```

⚠️ Server **luôn tự tính lại** khoảng cách, không tin bất kỳ trường "matchedUserId"/"success" nào client tự gửi lên (bài học bảo mật từ `REQUIREMENTS.md` mục 4).

### 3.2 Luồng Membership — state machine

```
Đăng ký gói mới → member_packages.status = PENDING
   │
   ├──> Lễ tân/Member xác nhận đã thanh toán (tại chỗ, không cổng ngoài)
   │        └──> status = ACTIVE, start_date = NGÀY THANH TOÁN THÀNH CÔNG (không phải lúc đăng ký)
   │
   ├──> [Nâng cấp — giá gói mới ≥ gói đang ACTIVE]
   │        └──> Hủy gói cũ ngay → tạo gói mới PENDING → ACTIVE khi thanh toán xong
   │
   ├──> [Hạ cấp — giá gói mới thấp hơn]
   │        └──> Gói cũ giữ nguyên chạy tới hết hạn → tạo bản ghi SCHEDULED
   │                 (start_date = end_date của gói hiện tại)
   │
   └──> Lazy-check (chạy mỗi khi đọc dữ liệu, KHÔNG cần cron):
            ├──> ACTIVE quá end_date → EXPIRED
            └──> SCHEDULED tới đúng start_date → ACTIVE
```

### 3.3 Luồng Booking & auto-status

```
Member/Lễ tân đặt lịch (chọn slot trống của PT theo trainer_schedules.work_date)
   │
   ├──> Validate: không quá khứ/quá gần <30p, không trùng slot, không trùng giờ 2 PT,
   │              trong ca làm việc thật, chưa vượt max_sessions_per_week
   │              (gán đủ FK/user_id TRƯỚC khi validate — xem REQUIREMENTS.md mục 7)
   │
   └──> bookings.status = PENDING
            │
            ├──> PT xác nhận → CONFIRMED
            ├──> Member face check-in (luồng 3.1) → set check_in_time
            ├──> PT bấm "Hoàn thành" (chỉ hiện khi check_in_time đã có) → COMPLETED
            │        └──> Trigger tính hoa hồng PT (Agent 5) theo commission_rate
            │
            └──> Lazy-check theo thời gian:
                     ├──> PENDING còn ≤1h chưa xác nhận → CANCELLED
                     ├──> CONFIRMED quá 30p giờ hẹn chưa COMPLETED → PT_NO_SHOW
                     └──> Quá giờ kết thúc mà check_in_time NULL → NO_SHOW
```

### 3.4 Luồng Lead → Commission (Sales)

```
Sales tạo/nhận Lead (status = NEW) → chăm sóc (CONTACTED)
   │
   └──> Lead mua gói thành công → convert: tạo user thật, status = CONVERTED
            └──> Trigger tính hoa hồng Sales (Agent 5), ghi vào commissions
```

## 4. Bảo mật

- JWT Access Token (ngắn hạn) + Refresh Token (dài hạn).
- Logout → đưa Access Token vào Redis blacklist đến khi hết hạn tự nhiên.
- RBAC bằng `@PreAuthorize("hasRole('...')")` ở tầng Controller/Service, đủ 5 role.
- Rate limiting theo IP/user cho các endpoint nhạy cảm (login, face-attendance) qua Redis.
- Descriptor khuôn mặt không phải dữ liệu công khai — chỉ Admin/Receptionist (kiosk) và chính chủ (self check-in) mới gọi được endpoint so khớp.
- Timezone cố định `Asia/Ho_Chi_Minh` xuyên suốt tầng ứng dụng, tránh lệch giờ giữa lưu trữ (server) và hiển thị.

## 5. Khả năng mở rộng

- Multi-branch qua `branch_id` trên các bảng nghiệp vụ chính, sẵn sàng cho mô hình chuỗi phòng gym.
- `FaceMatchService` được trừu tượng hoá qua interface — có thể đổi thuật toán so khớp (nâng cấp lên embedding chuẩn hơn, hoặc gọi Cloud API) mà không đổi API/DB phía trên, miễn giữ đúng contract "nhận descriptor, trả userId hoặc rỗng".
- Cổng thanh toán thật (VietQR/MoMo) và thông báo (Zalo ZNS/SMS) để sẵn package `integration/` — bổ sung sau v1.0 nếu cần demo với dữ liệu thật, không bắt buộc cho phạm vi 10 tuần hiện tại.
- Sẵn sàng tách microservice sau này nếu cần (Face Attendance và Booking là 2 ứng viên tách trước tiên do đặc thù throughput/latency khác các module còn lại).
