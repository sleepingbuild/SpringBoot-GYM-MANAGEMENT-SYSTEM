# 🏋️ Gym Management System (GMS)

**Hệ thống Quản lý Phòng Gym Trung cấp** — SaaS Multi-Tenant / Enterprise Multi-Branch, backend RESTful API bằng Spring Boot.

Quản lý vận hành toàn diện: hội viên, gói tập, kiểm soát ra/vào (check-in), đặt lịch PT/Group X, hoa hồng tự động, bán hàng tại quầy (POS), và báo cáo tài chính.

---

## 📌 Trạng thái dự án

| | |
|---|---|
| **Phiên bản** | v0.1.0-dev (đang xây dựng nền tảng) |
| **Giai đoạn hiện tại** | Phase 1 — Base & Security |
| **Team** | 6 agent song song, xem [`TASK_ASSIGNMENT.md`](./TASK_ASSIGNMENT.md) |

## 🧱 Tech Stack

| Phân vùng | Công nghệ |
|---|---|
| Core Framework | Java 17+, Spring Boot 3.x, Spring Data JPA, Spring Security |
| Database | Microsoft SQL Server 2022 (Flyway migration) |
| Cache & Session | Redis (JWT blacklist, rate limiting, check-in cache) |
| Auth & AuthZ | JWT (Access + Refresh Token), RBAC |
| Tích hợp | VietQR / MoMo API, Zalo ZNS / SMS Brandname, Hardware Gateway (RFID/QR/FaceID) |
| API Docs | springdoc-openapi (Swagger UI) |

## 🚀 Bắt đầu nhanh (Local Development)

### Yêu cầu
- JDK 17+
- Maven 3.9+
- Docker & Docker Compose (chạy SQL Server 2022 + Redis)

### Chạy local

```bash
# 1. Khởi động SQL Server + Redis (lần đầu container sqlserver cần ~15-30s để healthy
#    trước khi sqlserver-init tạo database gms_db tự động)
docker compose up -d

# 2. Copy file cấu hình mẫu
cp src/main/resources/application.yml.example src/main/resources/application-local.yml
# → điền các biến môi trường thật (DB, Redis, JWT secret, VietQR key...)
# → mật khẩu sa mặc định trong docker-compose.yml: GmsStrongP@ss123 (đổi lại nếu deploy thật)

# 3. Chạy migration + start app
mvn flyway:migrate
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Swagger UI sau khi chạy: `http://localhost:8080/swagger-ui.html`

## 📂 Tài liệu dự án

| File | Nội dung |
|---|---|
| [`REQUIREMENTS.md`](./REQUIREMENTS.md) | **Đọc trước tiên** — đặc tả đầy đủ, state machine, rule nghiệp vụ, bug cần tránh (đúc kết từ dự án .NET song song) |
| [`TASK_ASSIGNMENT.md`](./TASK_ASSIGNMENT.md) | Phân công chi tiết 6 agent |
| [`ISSUES.md`](./ISSUES.md) | Milestones + Issues chi tiết (sẵn sàng tạo lên GitHub) |
| [`ARCHITECTURE.md`](./ARCHITECTURE.md) | Kiến trúc hệ thống, cấu trúc thư mục, luồng xử lý chính |
| [`DATABASE_SCHEMA.md`](./DATABASE_SCHEMA.md) | Thiết kế CSDL đầy đủ + ERD |
| [`API_DESIGN.md`](./API_DESIGN.md) | Chuẩn response, mã lỗi, danh sách endpoint |
| [`ROADMAP.md`](./ROADMAP.md) | Lộ trình 4 giai đoạn, 8 tuần |
| [`CONTRIBUTING.md`](./CONTRIBUTING.md) | Quy tắc git, commit, PR, coding convention |
| [`DESIGN_SYSTEM.md`](./DESIGN_SYSTEM.md) | Hệ thống thiết kế UI/UX dùng chung cho frontend |

## 👥 Phân quyền (RBAC)

- **Super Admin** — chủ phòng gym, xem báo cáo, quản lý toàn hệ thống
- **Lễ tân (Receptionist)** — check-in, bán/gia hạn gói, POS
- **Huấn luyện viên (PT)** — quản lý lịch dạy, xác nhận buổi tập
- **Sales/CSKH** — quản lý Leads, hoa hồng bán gói
- **Hội viên (Member)** — xem gói, đặt lịch PT, điểm danh khuôn mặt, xem lịch sử check-in

## 📄 License

Nội bộ — Software Engineering Course Project.
