# ARCHITECTURE.md — Kiến trúc Hệ thống

## 1. Kiến trúc tổng quan

```
                         ┌──────────────────────┐
                         │   Frontend (React/    │
                         │   Vue/Flutter) — v0.4  │
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
        │   Spring        │Integration │   PostgreSQL / MySQL  │
        │   Security/JWT  │(VietQR,Zalo│                        │
        │        │        │ Hardware)  │                        │
        └────────┼────────┴─────┬──────┴────────────────────────┘
                 │               │
                 ▼               ▼
             ┌────────┐   ┌──────────────┐
             │ Redis  │   │ External API │
             │(cache, │   │ VietQR/MoMo, │
             │ JWT    │   │ Zalo ZNS,    │
             │blacklist│  │ Hardware GW  │
             └────────┘   └──────────────┘
```

**Nguyên tắc kiến trúc:**
- Layered architecture chuẩn: Controller → Service (interface + impl) → Repository → Entity.
- Mọi business logic nằm ở `service/`, Controller chỉ nhận request/validate/gọi service/trả response.
- Giao tiếp giữa các module nghiệp vụ (vd: Check-in cần trừ buổi của Membership) **luôn qua Service interface public**, không truy cập trực tiếp Repository của module khác — giữ ranh giới rõ ràng giữa các agent.
- Multi-branch: mọi entity nghiệp vụ chính đều có `branch_id` để hỗ trợ SaaS multi-tenant theo chi nhánh.

## 2. Cấu trúc thư mục dự án

```text
gym-management-system/
├── src/
│   ├── main/
│   │   ├── java/com/gym/management/
│   │   │   ├── GymManagementApplication.java
│   │   │   ├── config/               # SecurityConfig, CorsConfig, RedisConfig, OpenApiConfig
│   │   │   ├── controller/           # REST Controllers (1 file / module nghiệp vụ)
│   │   │   ├── dto/
│   │   │   │   ├── request/          # Request DTOs
│   │   │   │   └── response/         # Response DTOs
│   │   │   ├── entity/               # JPA Entities
│   │   │   ├── repository/           # Spring Data JPA Repositories
│   │   │   ├── service/              # Interfaces
│   │   │   │   └── impl/             # Implementations
│   │   │   ├── security/             # JwtTokenProvider, UserDetailsServiceImpl, JwtAuthFilter
│   │   │   ├── integration/
│   │   │   │   ├── vietqr/           # VietQR/MoMo client + webhook DTO
│   │   │   │   ├── zalo/             # Zalo ZNS / SMS Brandname client
│   │   │   │   └── hardware/         # RFID/QR/FaceID gateway client
│   │   │   └── exception/            # GlobalExceptionHandler, ErrorCode, custom exceptions
│   │   └── resources/
│   │       ├── application.yml.example
│   │       ├── db/migration/         # Flyway: V1__..., V2__..., ...
│   │       └── templates/            # Email / notification templates
│   └── test/java/com/gym/management/
├── docs/                              # ERD, sơ đồ bổ sung
├── docker-compose.yml
├── pom.xml
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

### 3.1 Luồng Check-in & Kiểm soát Ra vào

```
Request Check-in (Card ID / QR / User ID)
   │
   ├──> 1. Kiểm tra User có tồn tại?
   ├──> 2. Tìm MemberPackage có status == ACTIVE
   ├──> 3. Kiểm tra thời hạn: current_date ∈ [start_date, end_date]?
   ├──> 4. Nếu SESSION_BASED: remaining_sessions > 0?
   ├──> 5. Nếu OFF_PEAK: giờ hiện tại ∈ khung giờ cho phép?
   ├──> 6. Anti-passback: khoảng cách 2 lần check-in liên tiếp > 5 phút? (kiểm tra qua Redis)
   │
   └──> [HỢP LỆ] → trừ buổi (nếu có) → ghi log CheckIn → mở cổng
   └──> [KHÔNG HỢP LỆ] → trả mã lỗi chi tiết → ghi log từ chối
```

### 3.2 Luồng thanh toán VietQR động

```
Client tạo đơn (gói tập / POS)
   │
   ├──> Backend sinh mã VietQR động qua VietQR/MoMo API (transaction_code unique)
   ├──> payment_transactions.status = PENDING
   │
   ├──> [Khách quét & chuyển khoản]
   │
   └──> Ngân hàng gọi Webhook → xác thực chữ ký → idempotency check
              │
              ├──> [Hợp lệ] → status = SUCCESS → kích hoạt gói tập / hoàn tất đơn POS
              └──> [Không hợp lệ/trùng] → log & bỏ qua
```

### 3.3 Luồng đặt lịch PT & tính hoa hồng

```
Member đặt lịch (chọn khung giờ trống của PT) → pt_bookings.status = PENDING
   │
   └──> Sau buổi tập: PT xác nhận + Member xác nhận (hoặc quét QR)
              │
              └──> status = COMPLETED → tự động tính commission_amount
                        (theo commission_rate cấu hình trên hồ sơ PT)
                        → ghi vào bảng commissions
```

## 4. Bảo mật

- JWT Access Token (ngắn hạn) + Refresh Token (dài hạn, lưu hash trong DB/Redis).
- Logout → đưa Access Token vào Redis blacklist đến khi hết hạn tự nhiên.
- RBAC bằng `@PreAuthorize("hasRole('...')")` ở tầng Controller/Service.
- Rate limiting theo IP/user cho các endpoint nhạy cảm (login, check-in) qua Redis.
- Webhook thanh toán bắt buộc xác thực chữ ký (HMAC) từ bên thứ 3, không tin request không ký.

## 5. Khả năng mở rộng

- Multi-branch qua `branch_id` trên các bảng nghiệp vụ chính, sẵn sàng cho mô hình chuỗi phòng gym.
- Interface `NotificationSender` và `HardwareGatewayClient` được trừu tượng hoá để dễ đổi nhà cung cấp (Zalo → SMS khác; RFID → FaceID khác) mà không đổi business logic.
- Sẵn sàng tách microservice sau này nếu cần (Check-in Engine và Payment là 2 ứng viên tách trước tiên do đặc thù throughput/latency khác các module còn lại).
