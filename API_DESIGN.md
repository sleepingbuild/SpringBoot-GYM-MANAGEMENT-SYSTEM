# API_DESIGN.md — Chuẩn API & Danh sách Endpoint (bản đầy đủ, khớp `REQUIREMENTS.md`)

## 1. Chuẩn Response

**Thành công:**
```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": { },
  "timestamp": "2026-09-22T10:00:00Z"
}
```

**Lỗi:**
```json
{
  "success": false,
  "message": "Gói tập đã hết hạn",
  "errorCode": "MEMBERSHIP_EXPIRED",
  "timestamp": "2026-09-22T10:00:00Z"
}
```

`ApiResponse<T>`/`ErrorCode` do **Agent 1** định nghĩa trong `exception/`. Mọi controller bắt buộc trả qua wrapper này.

## 2. Chuẩn đặt tên & versioning

- Prefix: `/api/v1/...`, danh từ số nhiều (`/api/v1/bookings`, `/api/v1/member-packages`).
- Phân trang: `?page=0&size=20&sort=createdAt,desc` → `PageResponse<T>`.
- Header xác thực: `Authorization: Bearer <access_token>` (JWT tự cấp — xem `REQUIREMENTS.md` mục 8, không còn bolt-on qua .NET).

## 3. Mã lỗi chuẩn hoá (mở rộng theo `ErrorCode.java`)

| Mã | Mô tả | HTTP Status |
|---|---|---|
| `AUTH_INVALID_CREDENTIALS` | Sai email/mật khẩu | 401 |
| `AUTH_TOKEN_EXPIRED` | Token hết hạn | 401 |
| `AUTH_TOKEN_INVALID` | Chưa đăng nhập hoặc token không hợp lệ | 401 |
| `AUTH_FORBIDDEN_ROLE` | Không đủ quyền | 403 |
| `AUTH_EMAIL_ALREADY_EXISTS` | Email đã dùng | 409 |
| `AUTH_PHONE_ALREADY_EXISTS` | Số điện thoại đã dùng | 409 |
| `PROFILE_INVALID_AGE` | Tuổi < 18 (so với ngày đăng ký) | 400 |
| `PACKAGE_NOT_FOUND` | Không tìm thấy gói tập | 404 |
| `MEMBERSHIP_NOT_FOUND` | Không tìm thấy đăng ký gói | 404 |
| `MEMBERSHIP_EXPIRED` | Gói đã hết hạn | 400 |
| `MEMBERSHIP_NO_SESSION` | Hết buổi tập | 400 |
| `MEMBERSHIP_PENDING_EXISTS` | Đang có gói chờ thanh toán, không cho đăng ký thêm | 409 |
| `SCHEDULE_TIME_INVALID` | Ngày/giờ ca làm việc không hợp lệ (quá khứ, giờ < hiện tại) | 400 |
| `BOOKING_SLOT_TAKEN` | Khung giờ đã có người đặt | 409 |
| `BOOKING_OUT_OF_WORKING_HOURS` | Ngoài ca làm việc của PT | 400 |
| `BOOKING_TOO_SOON` | Đặt lịch cách hiện tại < 30 phút | 400 |
| `BOOKING_DOUBLE_BOOKED` | Member đã có lịch trùng giờ với PT khác | 409 |
| `BOOKING_WEEKLY_LIMIT_EXCEEDED` | Vượt giới hạn buổi/tuần theo gói | 400 |
| `BOOKING_PAST_CANNOT_CANCEL` | Không thể hủy buổi đã diễn ra | 400 |
| `BOOKING_NOT_CHECKED_IN` | Học viên chưa điểm danh khuôn mặt, PT không thể hoàn thành buổi | 400 |
| `BOOKING_PT_NOT_CHECKED_IN` | PT chưa quét mặt vào/ra, không thể hoàn thành buổi | 400 |
| `BOOKING_EARLY_LEAVE_REASON_NOT_ALLOWED` | Không thể nộp lý do (PT chưa ra sớm, hoặc buổi đã chốt) | 400 |
| `FACE_NOT_RECOGNIZED` | Không nhận diện được khuôn mặt (khoảng cách > ngưỡng) | 401 |
| `FACE_PROFILE_NOT_FOUND` | Chưa đăng ký hồ sơ khuôn mặt | 404 |
| `FACE_DESCRIPTOR_INVALID` | Descriptor gửi lên sai định dạng/độ dài | 400 |
| `PRODUCT_OUT_OF_STOCK` | Sản phẩm hết hàng | 400 |
| `LEAD_NOT_FOUND` | Không tìm thấy lead | 404 |
| `LEAD_ALREADY_CONVERTED` | Lead đã chuyển đổi trước đó | 409 |
| `LEAD_INVALID_STATUS_TRANSITION` | Chuyển trạng thái lead không hợp lệ (ví dụ từ `CONVERTED` về lại, hoặc `LOST` sang `CONTACTED`) | 400 |
| `VALIDATION_ERROR` | Lỗi validate chung | 400 |
| `RESOURCE_NOT_FOUND` | Không tìm thấy tài nguyên | 404 |
| `INTERNAL_SERVER_ERROR` | Lỗi hệ thống | 500 |

## 4. Danh sách Endpoint theo module

### Auth (Agent 1)
| Method | Endpoint | Role |
|---|---|---|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh` | Public |
| POST | `/api/v1/auth/logout` | Authenticated |

### Profile (Agent 1)
| Method | Endpoint | Role |
|---|---|---|
| GET | `/api/v1/profile/me` | Member/Trainer |
| PUT | `/api/v1/profile/me` | Member/Trainer |
| POST | `/api/v1/profile/me/avatar` (multipart) | Member/Trainer |

> **Hồ sơ cá nhân:** `PUT /api/v1/profile/me` nhận `dateOfBirth` (`yyyy-MM-dd`), `weightKg`, `heightCm`, `goal`. Ngày sinh phải cho tuổi từ 18 trở lên tính đến ngày đăng ký của tài khoản và không quá 120 tuổi; vi phạm trả 400 `PROFILE_INVALID_AGE`. Response trả `dateOfBirth` và `age` (tính sẵn từ ngày sinh, `null` nếu chưa có). Ảnh đại diện chỉ lưu ở hồ sơ (`avatarUrl`), không còn ở tài khoản.

### Membership, Package & Payment (Agent 2)
| Method | Endpoint | Role |
|---|---|---|
| GET | `/api/v1/packages` | Công khai, không cần đăng nhập (chỉ gói đang mở bán, phân trang) |
| GET | `/api/v1/packages/{id}` | Công khai (chỉ gói đang mở bán; gói đã ngừng bán trả 404 `PACKAGE_NOT_FOUND`) |
| GET | `/api/v1/packages/admin` | Super Admin — toàn bộ gói kể cả đã ngừng bán, phân trang |
| GET | `/api/v1/packages/admin/{id}` | Super Admin — chi tiết 1 gói bất kể trạng thái |
| POST | `/api/v1/packages` | Super Admin |
| PUT/DELETE | `/api/v1/packages/{id}` | Super Admin (DELETE = ngừng bán, `is_active = false`) |
| POST | `/api/v1/member-packages` | Receptionist (bán gói) hoặc Member (tự mua) |
| GET | `/api/v1/member-packages/me` | Member — dùng `getCurrentMembership` |
| POST | `/api/v1/member-packages/{id}/upgrade` | Receptionist/Member |
| POST | `/api/v1/member-packages/{id}/downgrade` | Receptionist/Member |
| POST | `/api/v1/payments/{transactionId}/confirm` | Receptionist — xác nhận thanh toán tại chỗ |
| GET | `/api/v1/payments/me` | Member — lịch sử thanh toán |
| GET | `/api/v1/payments?from=...&to=...` | Receptionist/Admin |

### Lịch làm việc PT & Booking (Agent 3)
| Method | Endpoint | Role |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/v1/trainer-schedules` | Trainer (của chính mình) / Admin |
| GET | `/api/v1/trainer-schedules/available?trainerId=...&date=...` | Member — xem slot trống |
| POST | `/api/v1/bookings` | Member (tự đặt) / Receptionist (đặt hộ) |
| PUT | `/api/v1/bookings/{id}` | Member/Receptionist (sửa lịch, áp lại đủ rule) |
| POST | `/api/v1/bookings/{id}/confirm` | Trainer |
| POST | `/api/v1/bookings/{id}/cancel` | Member/Trainer/Receptionist — huỷ muộn (dưới 2 giờ, booking `CONFIRMED`) → `LATE_CANCELLED`, mất 1 buổi, PT vẫn có lương |
| POST | `/api/v1/bookings/{id}/complete` | Trainer — chỉ khi `CONFIRMED`, học viên đã check-in, PT đã quét vào và ra |
| PUT | `/api/v1/bookings/{id}/early-leave-reason` | Trainer — nộp lý do ra sớm (`{ "reason": "..." }`), trước khi hoàn thành |
| GET | `/api/v1/bookings/me` | Member/Trainer |
| GET | `/api/v1/bookings?branchId=...&date=...` | Receptionist/Admin |

### Face Attendance & Chấm công (Agent 4)
| Method | Endpoint | Role |
|---|---|---|
| POST | `/api/v1/face-profiles/{userId}` (multipart ảnh tĩnh, backend tự trích descriptor) | Receptionist/Admin — đăng ký hộ |
| POST | `/api/v1/face-profiles/me` (descriptor JSON, client tự trích) | Member/Trainer/Receptionist/Sales — tự đăng ký |
| GET/DELETE | `/api/v1/face-profiles/{userId}` | Receptionist/Admin |
| POST | `/api/v1/face-attendance/kiosk` (descriptor JSON) | Receptionist/Admin — 1:N. Kết quả ghi theo role người được nhận diện (Member/Trainer → booking, Receptionist/Sales → `staff_attendances`) |
| POST | `/api/v1/face-attendance/self` (descriptor JSON) | Member/Trainer/Receptionist/Sales đã đăng nhập — 1:1. Trainer: lượt quét gắn vào booking đang diễn ra; response có `bookingId`, `earlyLeave`, `reasonRequired`, `message` |
| GET | `/api/v1/staff-attendance/me` | Receptionist/Sales |
| GET | `/api/v1/staff-attendance?date=...` | Admin — báo cáo chấm công |

### POS & Commission (Agent 5)
| Method | Endpoint | Role |
|---|---|---|
| GET/POST/PUT | `/api/v1/products` | Receptionist/Admin |
| POST | `/api/v1/pos/orders` | Receptionist |
| GET | `/api/v1/pos/orders?branchId=...` | Receptionist/Admin |
| GET | `/api/v1/commissions/me` | Trainer — lương theo buổi của chính mình |
| GET | `/api/v1/commissions?staffId=...&month=...` | Admin |
| GET/PUT | `/api/v1/trainer-pay-rates/{trainerId}` | Super Admin — xem/đặt lương mỗi buổi của PT |

### Leads/CRM (Agent 6)
| Method | Endpoint | Role |
|---|---|---|
| GET | `/api/v1/leads?status=&keyword=&page=&size=&sort=` | Sales, Super Admin. Sales chỉ thấy lead của mình hoặc chưa gán. `sort`: `createdAt`, `updatedAt`, `fullName`, `status` |
| POST | `/api/v1/leads` | Sales, Super Admin. Response có `duplicatePhone` (true nếu số đã có ở lead khác, lead vẫn được tạo) |
| GET | `/api/v1/leads/{id}` | Sales, Super Admin. Lead của Sales khác trả 404 |
| PUT | `/api/v1/leads/{id}` | Sales, Super Admin. Lead `CONVERTED` trả 409 `LEAD_ALREADY_CONVERTED` |
| PATCH | `/api/v1/leads/{id}/status` (body: `status` = `CONTACTED` hoặc `LOST`) | Sales, Super Admin. Chuyển không hợp lệ trả 400 `LEAD_INVALID_STATUS_TRANSITION` |
| POST | `/api/v1/leads/{id}/convert` (body: `email` bắt buộc; `password` tuỳ chọn, tối thiểu 8 ký tự) | Sales phụ trách lead, Super Admin. Tạo user `MEMBER`, không sinh hoa hồng. Bỏ trống `password` thì sinh ngẫu nhiên 12 ký tự và trả `temporaryPassword` đúng một lần (không ghi log, không lưu dạng thô) |
| GET | `/api/v1/leads/expiring-soon?days=N` | Sales, Super Admin — hội viên có gói `ACTIVE` sắp hết hạn. *(chưa triển khai, chờ Agent 2)* |
| GET | `/api/v1/reports/revenue?from=&to=&groupBy=DAY\|MONTH` | Super Admin. `from`, `to` dạng `yyyy-MM-dd`, mặc định từ ngày 1 tháng này đến hôm nay, tối đa 366 ngày |
| GET | `/api/v1/reports/renewal-rate?from=&to=&windowDays=` | Super Admin. `windowDays` 1 đến 365, mặc định 30 |
| GET | `/api/v1/reports/checkin-traffic?from=&to=` | Super Admin. Lượt vào buổi PT theo ngày và giờ. *(tạm thời, nguồn dữ liệu sẽ chốt sau khi V8 `gym_check_ins` vào `master`)* |
| GET | `/api/v1/reports/top-packages?from=&to=&limit=` | Super Admin. `limit` 1 đến 50, mặc định 10 |
| GET | `/api/v1/reports/commissions-summary` | Super Admin — tổng lương theo buổi của PT. *(chưa triển khai, chờ Agent 5)* |

> **Quy tắc trạng thái lead:** `NEW → CONTACTED | LOST | CONVERTED`; `CONTACTED → LOST | CONVERTED`. `LOST` và `CONVERTED` là trạng thái cuối. `CONVERTED` chỉ được đặt qua `POST /leads/{id}/convert`.

**Ghi chú cho mục báo cáo**
- Báo cáo là truy vấn chỉ đọc. Ngày "hôm nay" và khoảng mặc định tính theo `Asia/Ho_Chi_Minh` (dùng bean `Clock`).
- `revenue`: giao dịch `SUCCESS`, ngày ghi nhận là thời điểm chuyển sang thành công; là doanh thu gói tập (gộp), chưa trừ đơn POS đã huỷ.
- `renewal-rate`: tính theo `end_date`, đếm theo gói; `rate` nằm trong [0, 1] và là `null` nếu không có gói hết hạn trong kỳ.
- `checkin-traffic`: hiện chỉ đếm lượt vào buổi PT (`bookings.check_in_time`); chưa có dữ liệu lượt vào phòng chung.
- `top-packages`: mỗi giao dịch `MEMBERSHIP` `SUCCESS` là một lượt đăng ký.

> Danh sách sẽ được cập nhật liên tục — mỗi PR mới endpoint phải cập nhật bảng tương ứng trong file này (quy tắc trong `CONTRIBUTING.md`).
