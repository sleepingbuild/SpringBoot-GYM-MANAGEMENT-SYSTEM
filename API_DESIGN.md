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
| `AUTH_FORBIDDEN_ROLE` | Không đủ quyền | 403 |
| `AUTH_EMAIL_ALREADY_EXISTS` | Email đã dùng | 409 |
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
| `BOOKING_NOT_CHECKED_IN` | Chưa điểm danh khuôn mặt, PT không thể xác nhận | 400 |
| `FACE_NOT_RECOGNIZED` | Không nhận diện được khuôn mặt (khoảng cách > ngưỡng) | 401 |
| `FACE_PROFILE_NOT_FOUND` | Chưa đăng ký hồ sơ khuôn mặt | 404 |
| `FACE_DESCRIPTOR_INVALID` | Descriptor gửi lên sai định dạng/độ dài | 400 |
| `PRODUCT_OUT_OF_STOCK` | Sản phẩm hết hàng | 400 |
| `LEAD_NOT_FOUND` | Không tìm thấy lead | 404 |
| `LEAD_ALREADY_CONVERTED` | Lead đã chuyển đổi trước đó | 409 |
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

### Membership, Package & Payment (Agent 2)
| Method | Endpoint | Role |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/v1/packages` | Super Admin |
| POST | `/api/v1/member-packages` | Receptionist (bán gói) hoặc Member (tự mua) |
| GET | `/api/v1/member-packages/me` | Member — dùng `getCurrentMembership` |
| POST | `/api/v1/member-packages/{id}/upgrade` | Receptionist/Member |
| POST | `/api/v1/member-packages/{id}/downgrade` | Receptionist/Member |
| POST | `/api/v1/payments/{transactionId}/confirm` | Receptionist — xác nhận thanh toán tại chỗ |
| GET | `/api/v1/payments/me` | Member — lịch sử thanh toán |
| GET | `/api/v1/payments?branchId=...` | Receptionist/Admin |

### Lịch làm việc PT & Booking (Agent 3)
| Method | Endpoint | Role |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/v1/trainer-schedules` | Trainer (của chính mình) / Admin |
| GET | `/api/v1/trainer-schedules/available?trainerId=...&date=...` | Member — xem slot trống |
| POST | `/api/v1/bookings` | Member (tự đặt) / Receptionist (đặt hộ) |
| PUT | `/api/v1/bookings/{id}` | Member/Receptionist (sửa lịch, áp lại đủ rule) |
| POST | `/api/v1/bookings/{id}/confirm` | Trainer |
| POST | `/api/v1/bookings/{id}/cancel` | Member/Trainer |
| POST | `/api/v1/bookings/{id}/complete` | Trainer — chặn nếu `check_in_time` null |
| GET | `/api/v1/bookings/me` | Member/Trainer |
| GET | `/api/v1/bookings?branchId=...&date=...` | Receptionist/Admin |

### Face Attendance & Chấm công (Agent 4)
| Method | Endpoint | Role |
|---|---|---|
| POST | `/api/v1/face-profiles/{userId}` (multipart ảnh tĩnh, backend tự trích descriptor) | Receptionist/Admin — đăng ký hộ |
| POST | `/api/v1/face-profiles/me` (descriptor JSON, client tự trích) | Member/Trainer — tự đăng ký |
| GET/DELETE | `/api/v1/face-profiles/{userId}` | Receptionist/Admin |
| POST | `/api/v1/face-attendance/kiosk` (descriptor JSON) | Receptionist/Admin — 1:N |
| POST | `/api/v1/face-attendance/self` (descriptor JSON) | Member/Trainer đã đăng nhập — 1:1 |
| GET | `/api/v1/staff-attendance/me` | Trainer/Receptionist |
| GET | `/api/v1/staff-attendance?date=...` | Admin — báo cáo chấm công |

### POS & Commission (Agent 5)
| Method | Endpoint | Role |
|---|---|---|
| GET/POST/PUT | `/api/v1/products` | Receptionist/Admin |
| POST | `/api/v1/pos/orders` | Receptionist |
| GET | `/api/v1/pos/orders?branchId=...` | Receptionist/Admin |
| GET | `/api/v1/commissions/me` | Trainer/Sales |
| GET | `/api/v1/commissions?staffId=...&month=...` | Admin |

### Leads/CRM (Agent 6)
| Method | Endpoint | Role |
|---|---|---|
| GET/POST/PUT | `/api/v1/leads` | Sales |
| POST | `/api/v1/leads/{id}/convert` | Sales |
| GET | `/api/v1/leads/expiring-soon` | Sales — hội viên sắp hết hạn (dựa `getCurrentMembership`) |
| GET | `/api/v1/reports/revenue` | Super Admin |
| GET | `/api/v1/reports/renewal-rate` | Super Admin |
| GET | `/api/v1/reports/checkin-traffic` | Super Admin |
| GET | `/api/v1/reports/top-packages` | Super Admin |
| GET | `/api/v1/reports/commissions-summary` | Super Admin |

> Danh sách sẽ được cập nhật liên tục — mỗi PR mới endpoint phải cập nhật bảng tương ứng trong file này (quy tắc trong `CONTRIBUTING.md`).
