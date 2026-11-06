# Chạy toàn bộ hệ thống bằng Docker (test và demo)

Một lệnh dựng đủ: SQL Server, Redis, Backend Spring Boot, Frontend Next.js, và dữ liệu mẫu cho mọi vai trò.
Dùng file riêng `docker-compose.demo.yml`; `docker-compose.yml` cũ (chỉ SQL Server + Redis cho dev) giữ nguyên.

## Yêu cầu
- Docker Desktop (Compose v2), còn trống khoảng **5 GB RAM** cho Docker (SQL Server cần tối thiểu 2 GB) và 6 GB ổ đĩa.
- Cổng trống: **3000** (web), **8082** (API), **11433** (SQL Server). Không trùng Jenkins (8081), SQL Server dev (1433).
- Máy có mạng ở lần build đầu (tải image, Maven, npm). Lần đầu mất khoảng 5 đến 10 phút.

## Chạy
```powershell
docker compose -f docker-compose.demo.yml up -d --build
docker compose -f docker-compose.demo.yml ps
```
Thứ tự tự động: SQL Server khỏe, tạo database `gms_demo`, backend lên (Flyway tạo bảng V1 đến V9), nạp dữ liệu mẫu, frontend lên.
Khi `backend` và `frontend` đều `healthy` và `seed` thoát với mã 0 là xong.

| Địa chỉ | Dùng để |
|---|---|
| http://localhost:3000 | Giao diện web |
| http://localhost:8082/swagger-ui.html | Tài liệu và thử API |
| http://localhost:8082/actuator/health | Kiểm tra sống |
| `localhost,11433` (user `sa`) | Mở SSMS xem database `gms_demo` |

Tài khoản: xem `docs/TEST_ACCOUNTS.md` (mật khẩu chung `Gym@12345`, ví dụ `admin@gym.test`, `reception@gym.test`).

## Kiểm thử khói theo vai trò
```powershell
.\scripts\demo-smoke.ps1
```
Đăng nhập 6 tài khoản, kiểm tra quyền truy cập từng endpoint chính (200, 401, 403).

## Lệnh thường dùng
```powershell
docker compose -f docker-compose.demo.yml logs -f backend          # xem log backend
docker compose -f docker-compose.demo.yml run --rm seed            # nạp lại dữ liệu mẫu (chạy lại không lỗi)
docker compose -f docker-compose.demo.yml up -d --build backend    # build lại sau khi sửa code backend
docker compose -f docker-compose.demo.yml down                     # dừng, giữ dữ liệu
docker compose -f docker-compose.demo.yml down -v                  # dừng và XÓA dữ liệu (làm lại từ đầu)
```
Đổi cổng hoặc mật khẩu bằng biến môi trường trước lệnh `up`: `GMS_WEB_PORT`, `GMS_API_PORT`, `GMS_SQL_PORT`, `GMS_DB_PASSWORD`, `GMS_JWT_SECRET`.
Nếu đổi `GMS_API_PORT` phải build lại frontend (`up -d --build frontend`), vì địa chỉ API được nhúng vào mã web lúc build.

## Lưu ý khi demo
- **Camera (điểm danh khuôn mặt)** chỉ chạy trên `http://localhost` hoặc HTTPS. Mở web từ máy khác trong mạng bằng IP sẽ bị trình duyệt chặn camera.
- Hồ sơ khuôn mặt không có trong dữ liệu mẫu: phải đăng ký thật bằng camera ở màn hình đăng ký khuôn mặt.
- Dữ liệu mẫu có ngày giờ tương đối theo giờ Việt Nam; nạp lại bằng lệnh `seed` nếu để lâu ngày.
- Bản demo dùng JWT secret mặc định chỉ để thử, không dùng cho môi trường thật.

## Xử lý sự cố
- `backend` không lên: `docker compose -f docker-compose.demo.yml logs backend` và tìm dòng `APPLICATION FAILED` hoặc `Caused by`.
- Báo trùng cổng: đặt `GMS_WEB_PORT`, `GMS_API_PORT` hoặc `GMS_SQL_PORT` khác.
- `sqlserver` khởi động lại liên tục: Docker Desktop chưa được cấp đủ RAM (Settings, Resources).
