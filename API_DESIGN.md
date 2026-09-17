# API_DESIGN.md — Chuẩn API & Danh sách Endpoint

## 1. Chuẩn Response

**Thành công:**
```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": { },
  "timestamp": "2026-09-15T10:00:00Z"
}
```

**Lỗi:**
```json
{
  "success": false,
  "message": "Gói tập đã hết hạn",
  "errorCode": "MEMBERSHIP_EXPIRED",
  "timestamp": "2026-09-15T10:00:00Z"
}
```

Wrapper `ApiResponse<T>` và enum `ErrorCode` do **Agent 1** định nghĩa trong `exception/`. Tất cả controller của các agent khác **bắt buộc** trả về qua wrapper này.

## 2. Chuẩn đặt tên & versioning

- Prefix: `/api/v1/...`
- Danh từ số nhiều: `/api/v1/packages`, `/api/v1/member-packages`
- Phân trang: `?page=0&size=20&sort=createdAt,desc` → trả kèm `PageResponse<T>` (`content`, `totalElements`, `totalPages`, `page`, `size`).
- Header bắt buộc cho endpoint cần xác thực: `Authorization: Bearer <access_token>`

## 3. Mã lỗi chuẩn hoá (ví dụ — mở rộng theo module)

| Mã | Mô tả | HTTP Status |
|---|---|---|
| `AUTH_INVALID_CREDENTIALS` | Sai email/mật khẩu | 401 |
| `AUTH_TOKEN_EXPIRED` | Token hết hạn | 401 |
| `AUTH_FORBIDDEN_ROLE` | Không đủ quyền | 403 |
| `MEMBERSHIP_NOT_FOUND` | Không tìm thấy gói tập | 404 |
| `MEMBERSHIP_EXPIRED` | Gói đã hết hạn | 400 |
| `MEMBERSHIP_NO_SESSION` | Hết buổi tập | 400 |
| `CHECKIN_ANTI_PASSBACK` | Check-in quá gần lần trước | 400 |
| `CHECKIN_NO_ACTIVE_PACKAGE` | Không có gói tập nào đang hoạt động | 400 |
| `FACE_NOT_RECOGNIZED` | Không nhận diện được khuôn mặt | 401 |
| `FACE_PROFILE_NOT_FOUND` | Hội viên/PT chưa đăng ký ảnh khuôn mặt | 404 |
| `FACE_IMAGE_INVALID` | File ảnh không hợp lệ/quá dung lượng | 400 |
| `CHECKIN_OUT_OF_TIME_RANGE` | Ngoài khung giờ gói (Off-peak) | 400 |
| `PAYMENT_DUPLICATE_TRANSACTION` | Giao dịch trùng (idempotency) | 409 |
| `PAYMENT_WEBHOOK_INVALID_SIGNATURE` | Chữ ký webhook không hợp lệ | 400 |
| `CLASS_FULL` | Lớp Group X đã đủ chỗ | 409 |
| `PT_SCHEDULE_CONFLICT` | Trùng lịch PT | 409 |

## 4. Danh sách Endpoint theo module

### Auth (Agent 1)
| Method | Endpoint | Role |
|---|---|---|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh` | Public (kèm refresh token) |
| POST | `/api/v1/auth/logout` | Authenticated |

### Membership & Package (Agent 2)
| Method | Endpoint | Role |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/v1/packages` | Super Admin |
| POST | `/api/v1/member-packages` | Receptionist |
| GET | `/api/v1/member-packages/me` | Member |
| POST | `/api/v1/member-packages/{id}/freeze` | Receptionist/Member |
| POST | `/api/v1/member-packages/{id}/upgrade` | Receptionist |
| POST | `/api/v1/member-packages/{id}/renew` | Receptionist |

### Check-in (Agent 3)
| Method | Endpoint | Role |
|---|---|---|
| POST | `/api/v1/checkin/face` | Public (kiosk/camera tại cửa — danh tính xác định qua khuôn mặt, không qua JWT) |
| GET | `/api/v1/checkin/history/me` | Member/Trainer (đã đăng nhập) |
| GET | `/api/v1/checkin/history?branchId=...` | Receptionist/Admin |

### Face Profile — đăng ký ảnh khuôn mặt (Agent 3)
| Method | Endpoint | Role |
|---|---|---|
| POST | `/api/v1/members/{userId}/face-profile` (multipart, field `image`) | Receptionist/Admin |
| GET | `/api/v1/members/{userId}/face-profile` | Receptionist/Admin |
| DELETE | `/api/v1/members/{userId}/face-profile` | Receptionist/Admin |

> ⚠️ Nhận diện hiện tại (`MockFaceMatcher`) là **mock so khớp SHA-256 byte-for-byte**, không phải AI thật — chỉ nhận ra khi ảnh quét giống hệt ảnh đã đăng ký. Xem TODO trong `integration/hardware/MockFaceMatcher.java` để biết cách thay bằng Cloud API/model thật mà không đổi API phía trên.

### Payment & POS (Agent 4)
| Method | Endpoint | Role |
|---|---|---|
| POST | `/api/v1/payments/vietqr` | Authenticated |
| POST | `/api/v1/payments/webhook` | Public (xác thực chữ ký) |
| GET/POST | `/api/v1/products` | Receptionist/Admin |
| POST | `/api/v1/pos/orders` | Receptionist |

### PT & Group X (Agent 5)
| Method | Endpoint | Role |
|---|---|---|
| GET/POST | `/api/v1/pt-schedules` | Trainer |
| GET | `/api/v1/pt-schedules/available` | Member |
| POST | `/api/v1/pt-bookings` | Member |
| POST | `/api/v1/pt-bookings/{id}/confirm` | Trainer/Member |
| GET/POST | `/api/v1/group-x-classes` | Admin/Trainer |
| POST | `/api/v1/group-x-classes/{id}/book` | Member |
| GET | `/api/v1/commissions` | Trainer/Sales/Admin |

### Leads, Notification & Reports (Agent 6)
| Method | Endpoint | Role |
|---|---|---|
| GET/POST/PUT | `/api/v1/leads` | Sales |
| POST | `/api/v1/leads/{id}/convert` | Sales |
| GET | `/api/v1/reports/revenue` | Super Admin |
| GET | `/api/v1/reports/checkin-traffic` | Super Admin |
| GET | `/api/v1/reports/renewal-rate` | Super Admin |

> Danh sách sẽ được cập nhật liên tục khi các agent hoàn thiện module — mỗi PR mới endpoint phải cập nhật bảng tương ứng trong file này.
