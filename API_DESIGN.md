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
| `USER_NOT_FOUND` | Không tìm thấy tài khoản (quản trị) | 404 |
| `USER_SELF_CHANGE_FORBIDDEN` | Super Admin không tự đổi vai trò / tự khoá chính mình | 409 |
| `PROFILE_INVALID_AGE` | Tuổi < 18 (so với ngày đăng ký) | 400 |
| `PACKAGE_NOT_FOUND` | Không tìm thấy gói tập | 404 |
| `MEMBERSHIP_NOT_FOUND` | Không tìm thấy đăng ký gói | 404 |
| `MEMBERSHIP_EXPIRED` | Gói đã hết hạn | 400 |
| `MEMBERSHIP_NO_SESSION` | Hết buổi tập | 400 |
| `MEMBERSHIP_PENDING_EXISTS` | Đang có gói chờ thanh toán, không cho đăng ký thêm | 409 |
| `PAYMENT_NOT_FOUND` | Không tìm thấy giao dịch thanh toán | 404 |
| `PAYMENT_DUPLICATE_TRANSACTION` | Trùng mã giao dịch (rất hiếm, thử lại) | 409 |
| `PAYMENT_NOT_REFUNDABLE` | Không hoàn tiền được: đơn POS chưa có giao dịch, giao dịch chưa thu tiền thành công, hoặc giao dịch không thuộc đơn POS | 409 |
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
| `POS_ORDER_NOT_FOUND` | Không tìm thấy đơn bán lẻ (POS) | 404 |
| `COMMISSION_NOT_FOUND` | Không tìm thấy bản ghi lương theo buổi | 404 |
| `TRAINER_PAY_RATE_NOT_FOUND` | PT chưa được đặt mức lương mỗi buổi | 404 |
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

### Danh mục dùng chung (Agent 1)
| Method | Endpoint | Role |
|---|---|---|
| GET | `/api/v1/users?role=MEMBER&q=&page=&size=` | Receptionist/Super Admin — tìm hội viên đang hoạt động theo tên, số điện thoại hoặc email. Chỉ trả `id`, `fullName`, `phone`, `email`. `role` chỉ nhận `MEMBER` (giá trị khác trả 400 `VALIDATION_ERROR`). Sắp theo tên, `size` tối đa 50 |
| GET | `/api/v1/admin/users?group=CUSTOMER\|STAFF&q=&status=&page=&size=` | Super Admin — danh sách khách hàng (`CUSTOMER`, vai trò MEMBER) hoặc nhân viên (`STAFF`: SUPER_ADMIN, RECEPTIONIST, TRAINER, SALES); tìm theo tên/email/số điện thoại; `status` rỗng = tất cả, hoặc `ACTIVE`/`INACTIVE`. Trả `id`, `fullName`, `email`, `phone`, `status`, `roles`. `size` tối đa 50 |
| POST | `/api/v1/admin/users` | Super Admin — tạo tài khoản nhân viên (`fullName`, `email`, `phone?`, `password`, `roles[]`); không nhận vai trò MEMBER. 409 `AUTH_EMAIL_ALREADY_EXISTS` / `AUTH_PHONE_ALREADY_EXISTS` |
| PUT | `/api/v1/admin/users/{id}` | Super Admin — sửa `fullName`, `phone`. 404 `USER_NOT_FOUND` |
| PUT | `/api/v1/admin/users/{id}/roles` | Super Admin — gán vai trò (`roles[]`). MEMBER không được kết hợp với vai trò nhân viên (400 `VALIDATION_ERROR`); không tự đổi vai trò của mình (409 `USER_SELF_CHANGE_FORBIDDEN`). Có hiệu lực ngay ở request kế tiếp vì quyền được đọc lại từ CSDL mỗi request |
| PUT | `/api/v1/admin/users/{id}/status` | Super Admin — `status` = `ACTIVE` (mở) hoặc `INACTIVE` (khoá); không tự khoá mình. Tài khoản bị khoá không đăng nhập được và token còn hạn cũng bị từ chối ngay |
| GET | `/api/v1/trainers` | Mọi người dùng đã đăng nhập — huấn luyện viên đang hoạt động: `id`, `fullName`, `avatarUrl` |
| GET | `/api/v1/branches` | Mọi người dùng đã đăng nhập — chi nhánh đang hoạt động: `id`, `name` |

> **Xác thực:** người chưa đăng nhập (không có token, token sai hoặc hết hạn) nhận **401** với `errorCode = AUTH_TOKEN_INVALID`. Đã đăng nhập nhưng không đủ quyền nhận **403** với `errorCode = AUTH_FORBIDDEN_ROLE`. Cả hai đều có body JSON `ApiResponse`.

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
| GET | `/api/v1/member-packages/me` | Member, Trainer, Receptionist, Super Admin — gói ACTIVE của chính mình (dùng `getCurrentMembership`); không có gói ACTIVE trả `MEMBERSHIP_NOT_FOUND` |
| GET | `/api/v1/member-packages/me/history` | Như trên — lịch sử toàn bộ gói của chính mình (mọi trạng thái), phân trang |
| GET | `/api/v1/member-packages/current?userId={id}` | Receptionist, Super Admin — gói ACTIVE của một hội viên (để nâng, hạ cấp hộ); không có gói ACTIVE trả `MEMBERSHIP_NOT_FOUND` |
| GET | `/api/v1/member-packages?userId={id}` | Receptionist, Super Admin — lịch sử gói của một hội viên (mọi trạng thái), phân trang |
| POST | `/api/v1/member-packages/{id}/upgrade` | Receptionist/Member |
| POST | `/api/v1/member-packages/{id}/downgrade` | Receptionist/Member |
| POST | `/api/v1/payments/{transactionId}/confirm` | Receptionist, Super Admin — xác nhận thanh toán tại chỗ. Body tùy chọn `{ "paymentMethod": "..." }` (`CASH` hoặc `LOCAL_CONFIRM`); bỏ trống thì giữ phương thức đã ghi lúc tạo giao dịch (mặc định `CASH`) |
| GET | `/api/v1/payments/me` | Member, Trainer, Receptionist, Super Admin — lịch sử thanh toán của chính mình, phân trang |
| GET | `/api/v1/payments?from=&to=&status=&userId=` | Receptionist, Super Admin — tra cứu giao dịch, phân trang. Tất cả tham số đều tùy chọn: `status` (`PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`; sai giá trị trả `VALIDATION_ERROR`) và `userId` (người mua). Không truyền ngày: không có bộ lọc nào thì lấy hôm nay, có `status` hoặc `userId` thì lấy mọi ngày (để tìm giao dịch chờ xác nhận cũ); truyền `from` hoặc `to` thì lọc theo ngày. Mỗi dòng có `userFullName` (`null` với đơn POS của khách vãng lai) |

### Lịch làm việc PT & Booking (Agent 3)
| Method | Endpoint | Role |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/v1/trainer-schedules` | Trainer (của chính mình) / Admin |
| GET | `/api/v1/trainer-schedules/available?trainerId=...&date=...` | Member / Lễ tân / Admin — xem slot trống |
| GET | `/api/v1/trainer-schedules/trainers?date=...` | Member / Lễ tân / Admin — các PT đang hoạt động còn ít nhất một slot trống trong ngày: `trainerId`, `trainerName`, `availableSlotCount`, xếp theo tên |
| GET | `/api/v1/trainer-schedules/off-hours?trainerId=...&date=...` | Member / Lễ tân / Admin — khung giờ ngoài giờ còn trống của PT (mặc định 21:00 đến 23:59, cấu hình `gms.booking.off-hours-start` / `off-hours-end`; trừ slot nằm trong ca của PT, slot đã có người đặt và slot quá sát giờ). Đặt trong ca làm việc của PT: booking `CONFIRMED` ngay. Đặt trong khung ngoài giờ: booking `PENDING`, chờ PT xác nhận. Giờ khác: `BOOKING_OUT_OF_WORKING_HOURS`. Cuối khung 23:59 hiển thị là 24:00 |
| POST | `/api/v1/bookings` | Member (tự đặt) / Receptionist (đặt hộ) |
| PUT | `/api/v1/bookings/{id}` | Member/Receptionist (sửa lịch, áp lại đủ rule) |
| POST | `/api/v1/bookings/{id}/confirm` | Trainer |
| POST | `/api/v1/bookings/{id}/cancel` | Member/Trainer/Receptionist — huỷ muộn (dưới 2 giờ, booking `CONFIRMED`) → `LATE_CANCELLED`, mất 1 buổi, PT vẫn có lương |
| POST | `/api/v1/bookings/{id}/complete` | Trainer — chỉ khi `CONFIRMED`, học viên đã check-in, PT đã quét vào và ra |
| PUT | `/api/v1/bookings/{id}/early-leave-reason` | Trainer — nộp lý do ra sớm (`{ "reason": "..." }`), trước khi hoàn thành |
| GET | `/api/v1/bookings/me` | Member/Trainer — lọc tuỳ chọn `status`, `from`, `to` (`yyyy-MM-dd`); lọc theo trạng thái sau khi áp lazy-status |
| GET | `/api/v1/bookings/{id}` | Member/Trainer/Receptionist/Admin — chi tiết một buổi; chỉ chủ buổi (học viên hoặc PT) hoặc nhân viên, người khác bị `AUTH_FORBIDDEN_ROLE` |
| GET | `/api/v1/bookings?date=...&branchId=...` | Receptionist/Admin — `branchId` không bắt buộc; bỏ trống thì liệt kê mọi chi nhánh (kể cả buổi chưa gắn chi nhánh) |

### Face Attendance & Chấm công (Agent 4)
| Method | Endpoint | Role |
|---|---|---|
| POST | `/api/v1/face-profiles/{userId}` (JSON descriptor, client chọn ảnh và trích bằng face-api.js) | Receptionist/Admin — đăng ký hộ |
| POST | `/api/v1/face-profiles/me` (descriptor JSON, client tự trích) | Member/Trainer/Receptionist/Sales — tự đăng ký |
| GET | `/api/v1/face-profiles/me` | Member/Trainer/Receptionist/Sales — trạng thái đăng ký của chính mình, trả `registered` và `updatedAt`, không trả descriptor |
| GET/DELETE | `/api/v1/face-profiles/{userId}` | Receptionist/Admin |
| POST | `/api/v1/face-attendance/kiosk` (descriptor JSON) | Receptionist/Admin — 1:N. Kết quả ghi theo role người được nhận diện (Member/Trainer → booking, Receptionist/Sales → `staff_attendances`) |
| POST | `/api/v1/face-attendance/self` (descriptor JSON) | Member/Trainer/Receptionist/Sales đã đăng nhập — 1:1. Trainer: lượt quét gắn vào booking đang diễn ra; response có `bookingId`, `earlyLeave`, `reasonRequired`, `message` |
| GET | `/api/v1/staff-attendance/me` | Receptionist/Sales |
| GET | `/api/v1/staff-attendance?date=...` | Admin — báo cáo chấm công |
| GET | `/api/v1/check-ins/me` | Member — lịch sử vào phòng tập, mới nhất trước, chưa phân trang. Mỗi phần tử: `type` (`GYM_VISIT` = lượt không PT từ `gym_check_ins`, `PT_SESSION` = buổi có PT từ `bookings`), `id`, `checkInTime`, `checkOutTime` (null = chưa quét ra), `method`, `trainerName` (chỉ `PT_SESSION`) |

Ghi chú CH2 (V8): Member quét mặt (kiosk hoặc self) mà không có booking phù hợp thì response có `targetType = GYM_CHECK_IN`, `bookingLinked = false`, `action` là `CHECK_IN` hoặc `CHECK_OUT`. Lượt vào mới yêu cầu gói ACTIVE: không có gói nào trả `MEMBERSHIP_NOT_FOUND`, có gói nhưng hết hạn trả `MEMBERSHIP_EXPIRED` (cả hai HTTP theo `ErrorCode`). Lượt có booking chỉ cập nhật `bookings`, không tạo dòng `gym_check_ins`.
Quét tự phục vụ (`/face-attendance/self`) dùng chung quy tắc với kiosk: cùng ghi `gym_check_ins` và cùng kiểm tra gói. Khi không có gói ACTIVE, thông báo lỗi là "Không có gói tập còn hiệu lực" cho cả hai mã. `GET /check-ins/me` trả toàn bộ lịch sử (chưa phân trang); sẽ thêm lọc theo khoảng ngày khi cần.
### POS & Commission (Agent 5)
| Method | Endpoint | Role |
|---|---|---|
| GET | `/api/v1/products` | Đã đăng nhập — sản phẩm đang bán, phân trang |
| GET | `/api/v1/products/admin` | Receptionist/Admin — gồm cả sản phẩm đã ngừng bán |
| GET | `/api/v1/products/{id}` | Đã đăng nhập |
| POST/PUT | `/api/v1/products` , `/api/v1/products/{id}` | Receptionist/Admin |
| DELETE | `/api/v1/products/{id}` | Receptionist/Admin — xoá mềm (`is_active = false`) |
| POST | `/api/v1/pos/orders` | Receptionist/Admin — giá lấy từ DB, trừ kho + thu tiền ngay trong 1 transaction (qua `PaymentService`, `RelatedType.POS`) |
| GET | `/api/v1/pos/orders/{id}` | Receptionist/Admin |
| GET | `/api/v1/pos/orders?branchId=...&from=...&to=...` | Receptionist/Admin — phân trang, mặc định hôm nay |
| POST | `/api/v1/pos/orders/{id}/cancel` | Receptionist/Admin — huỷ đơn `COMPLETED`: hoàn tiền giao dịch của đơn (`PaymentService.refundByRelated`), rồi hoàn tồn kho; không hoàn được tiền thì không huỷ (`PAYMENT_NOT_REFUNDABLE`); huỷ lại không hoàn thêm |
| GET | `/api/v1/commissions/me` | Trainer — lương theo buổi của chính mình, phân trang |
| GET | `/api/v1/commissions?staffId=...&month=yyyy-MM` | Admin — phân trang |
| POST | `/api/v1/commissions/{id}/mark-paid` | Admin — `PENDING` → `PAID`, ghi `paid_at` |
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
| GET | `/api/v1/leads/expiring-soon?days=N` | Sales, Super Admin — hội viên có gói `ACTIVE` hết hạn từ hôm nay đến hôm nay cộng `days` ngày. `days` 1 đến 90, mặc định 7, sai trả 400 `VALIDATION_ERROR`. Sắp theo ngày hết hạn; bỏ hội viên đã có gói `ACTIVE`/`SCHEDULED` kết thúc muộn hơn (đã gia hạn) và tài khoản không hoạt động. Response mỗi phần tử: `userId`, `fullName`, `phone`, `email`, `packageName`, `endDate`, `daysLeft`, `remainingSessions` (các trường rỗng bị bỏ) |
| GET | `/api/v1/leads/sales-users` | Super Admin — Sales đang hoạt động (`id`, `fullName`, `phone`), sắp theo tên, để chọn người phụ trách lead |
| PATCH | `/api/v1/leads/{id}/assign` (body: `salesId`, để trống = bỏ giao) | Super Admin. `salesId` phải là Sales đang hoạt động (sai trả 400 `VALIDATION_ERROR`). Lead `CONVERTED` trả 409 `LEAD_ALREADY_CONVERTED`, lead `LOST` trả 400. Sau khi giao, Sales khác không còn thấy lead (trả 404) |
| GET | `/api/v1/reports/revenue?from=&to=&groupBy=DAY\|MONTH` | Super Admin. `from`, `to` dạng `yyyy-MM-dd`, mặc định từ ngày 1 tháng này đến hôm nay, tối đa 366 ngày |
| GET | `/api/v1/reports/renewal-rate?from=&to=&windowDays=` | Super Admin. `windowDays` 1 đến 365, mặc định 30 |
| GET | `/api/v1/reports/checkin-traffic?from=&to=` | Super Admin. Lượt hội viên quét mặt vào phòng tập theo ngày và giờ: gộp `bookings.check_in_time` (lượt có buổi PT) và `gym_check_ins` (lượt không gắn buổi PT) |
| GET | `/api/v1/reports/top-packages?from=&to=&limit=` | Super Admin. `limit` 1 đến 50, mặc định 10 |
| GET | `/api/v1/reports/commissions-summary?fromMonth=&toMonth=` | Super Admin — tổng lương theo buổi của PT theo tháng (gọi `CommissionService.summarize(YearMonth)` của Agent 5). `fromMonth`, `toMonth` dạng `yyyy-MM`, mặc định cả hai là tháng hiện tại, tối đa 12 tháng; sai trả 400 `VALIDATION_ERROR` |

> **Quy tắc trạng thái lead:** `NEW → CONTACTED | LOST | CONVERTED`; `CONTACTED → LOST | CONVERTED`. `LOST` và `CONVERTED` là trạng thái cuối. `CONVERTED` chỉ được đặt qua `POST /leads/{id}/convert`.

**Ghi chú cho mục báo cáo**
- Báo cáo là truy vấn chỉ đọc. Ngày "hôm nay" và khoảng mặc định tính theo `Asia/Ho_Chi_Minh` (dùng bean `Clock`).
- `revenue`: giao dịch `SUCCESS`, ngày ghi nhận là thời điểm chuyển sang thành công; là doanh thu gói tập (gộp), chưa trừ đơn POS đã huỷ.
- `renewal-rate`: tính theo `end_date`, đếm theo gói; `rate` nằm trong [0, 1] và là `null` nếu không có gói hết hạn trong kỳ.
- `checkin-traffic`: đếm số dòng `bookings` có `check_in_time` cộng số dòng `gym_check_ins`. Hai nguồn không giao nhau (lượt có buổi PT phù hợp chỉ cập nhật `bookings`, xem V8), quét ra chỉ cập nhật chính dòng đó nên không đếm trùng.
- `commissions-summary`: tháng tính theo `commissions.created_at`, luôn trả đủ mọi tháng trong khoảng (tháng không dữ liệu trả 0). Response: `fromMonth`, `toMonth`, các tổng `totalSessions`, `totalAmount`, `pendingCount`, `pendingAmount`, `paidCount`, `paidAmount`, và `months[]` cùng các trường đó cho từng tháng.
- `top-packages`: mỗi giao dịch `MEMBERSHIP` `SUCCESS` là một lượt đăng ký.

> Danh sách sẽ được cập nhật liên tục — mỗi PR mới endpoint phải cập nhật bảng tương ứng trong file này (quy tắc trong `CONTRIBUTING.md`).
