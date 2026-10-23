# Đối chiếu thiết kế DB với tiêu chí chấm (Bước 2), bản v2

Cơ sở: 88 user story trong `USER_STORIES.md`, đối chiếu với 18 bảng của DB đang chạy (v1, theo `DATABASE_SCHEMA.md` 2026-10-12). Kết quả là thiết kế v2 gồm **19 bảng, 28 khoá ngoại**. **Phi đã duyệt ngày 2026-10-16** (xem `DECISIONS_DB_V2.md`): v2 là thiết kế chính thức. **DB đang chạy vẫn là v1**, các thay đổi ở mục 2 sẽ được triển khai dần theo thứ tự ở mục 2.

Hình tương ứng: `gms_v2_0_overview` (tổng quan, chỉ cột khoá), `gms_v2_1_nguoidung_goitap`, `gms_v2_2_lichtap_diemdanh_luong`, `gms_v2_3_thanhtoan_banle` (đủ cột, mỗi sơ đồ một phân hệ), `gms_v2_schema` (một sơ đồ đủ cột).

## 1. Kết quả theo tiêu chí (v2)

| Tiêu chí | Trọng số | Kết quả | Bằng chứng |
|---|---|---|---|
| Thực thể và thuộc tính | 20% | Đạt | 19 bảng; 88 user story, trong đó 87 story thao tác trên dữ liệu trong bảng và đều ánh xạ được vào ít nhất 1 bảng (C1 Đăng xuất chỉ dùng Redis); 19/19 bảng được ít nhất 1 user story dùng (mục 5); mọi cột có kiểu phù hợp |
| Khoá PK và FK | 15% | Đạt | PK 19/19 (`user_roles` dùng PK kép); 28 FK; không còn cột `*_id` nào thiếu khoá ngoại; mặc định `ON DELETE NO ACTION` nên không xoá được cha khi còn con |
| Mối quan hệ | 15% | Đạt | 4 quan hệ 1-1, 24 quan hệ 1-N; N-N tách bằng 4 bảng trung gian (mục 3) |
| Chuẩn hoá | 10% | Đạt 2NF và 3NF | Mục 4; còn một nhóm cột lặp ở `bookings` được giải trình |

## 2. Thay đổi từ v1 sang v2 (đã duyệt)

Phát hiện khi đối chiếu từng user story với từng bảng. Quyết định của Agent 1 và Phi ngày 2026-10-16:

| # | Thay đổi | Cách làm đã chốt | Ai triển khai |
|---|---|---|---|
| CH1 | `payment_transactions`: bỏ `related_type` + `related_id`, thêm `member_package_id` và `pos_order_id` (`CHECK` đúng một cột có giá trị); `user_id` cho phép `NULL` | Giữ nguyên chữ ký `PaymentService` và `RelatedType` ở tầng service; tầng entity ánh xạ `MEMBERSHIP` sang `member_package_id`, `POS` sang `pos_order_id`, nên Agent 5 không phải sửa code. Một migration duy nhất sau khi V5 merge (cần bảng `pos_orders` để tạo FK): thêm hai cột, chuyển dữ liệu cũ, thêm `CHECK`, bỏ cột cũ, `user_id` nullable | Agent 2, một PR riêng (entity, service, test) |
| CH2 | Bảng mới `gym_check_ins` | Chỉ ghi lượt quét không có buổi PT phù hợp; lượt có booking vẫn chỉ cập nhật `bookings` (tránh đếm hai lần). Kiosk kiểm tra hội viên có gói `ACTIVE`, không có thì từ chối (`MEMBERSHIP_EXPIRED`). Báo cáo lưu lượng lấy hợp hai nguồn | Agent 4, sau PR #43; Agent 6 cho báo cáo |
| CH3 | `user_profiles.age` đổi thành `date_of_birth` | Ngày sinh phải cách ngày đăng ký (`users.created_at`) ít nhất 18 năm; `age` cũ đặt `NULL` (DB dev chưa có dữ liệu thật) | Agent 1 |
| CH4 | Bỏ `users.avatar_url` | Nguồn duy nhất là `user_profiles.avatar_url`; Agent 1 kiểm tra code dùng `User.avatarUrl` trước khi bỏ cột | Agent 1 (gộp với CH3 và sửa `users.phone`) |
| CH5 | Bỏ `pos_order_items.subtotal` và `pos_orders.total_amount` | Tổng tính từ `quantity` × `unit_price`; số tiền đã thu nằm ở `payment_transactions.amount` | Agent 5, ngay trong V5 |

### Thứ tự migration

`spring.flyway.out-of-order: true` đã có trong `application.yml`, nên số nhỏ merge sau không làm DB báo lỗi. Ràng buộc còn lại: khoá ngoại phải có bảng đích trước.

| Version | Nội dung | Agent | Điều kiện |
|---|---|---|---|
| V3, V4 | Booking, Face Attendance | 3, 4 | đang chờ merge |
| V5 | POS, lương PT (không còn `subtotal`, `total_amount`) | 5 | sau V3 (FK `commissions` → `bookings`) |
| V6 | Leads | 6 | không phụ thuộc |
| V7 | `payment_transactions` tách FK (CH1) | 2 | sau V5 |
| V8 | `gym_check_ins` (CH2) | 4 | sau V4 |
| V9 | `date_of_birth`, bỏ `users.avatar_url`, filtered unique index `users.phone` (CH3, CH4) | 1 | độc lập |

Ưu tiên khi thiếu thời gian (cắt từ dưới lên): CH1, CH5, rồi CH2 và M17, rồi CH3 và CH4 cùng sửa `phone`, cuối cùng là tám story còn lại. Phần nào chưa kịp triển khai thì ghi rõ trong bài là thiết kế mục tiêu, kèm giải trình.

## 3. Quan hệ

N-N được tách bằng bảng trung gian:

| N-N | Bảng trung gian | Ghi chú |
|---|---|---|
| `users` ↔ `roles` | `user_roles` | PK kép (`user_id`, `role_id`), không có thuộc tính riêng |
| `users` ↔ `packages` | `member_packages` | Thực thể kết hợp: `status`, `start_date`, `end_date`, `remaining_sessions` |
| `pos_orders` ↔ `products` | `pos_order_items` | Thuộc tính: `quantity`, `unit_price` |
| Học viên ↔ PT (cùng là `users`) | `bookings` | Thực thể kết hợp: ngày giờ, `status`, điểm danh |

Quan hệ 1-1 (cột FK có `UNIQUE`): `user_profiles.user_id`, `face_profiles.user_id`, `trainer_pay_rates.trainer_id` đều trỏ tới `users`; `commissions.booking_id` trỏ tới `bookings` (mỗi buổi tối đa một bản ghi trả công). `payment_transactions` có hai FK tuỳ chọn, mỗi giao dịch chỉ gắn với đúng một đối tượng (gói tập hoặc đơn bán lẻ). Còn lại là quan hệ 1-N (24).

## 4. Chuẩn hoá

**2NF:** 18 bảng có PK đơn nên không có phụ thuộc hàm riêng phần; `user_roles` có PK kép nhưng không có thuộc tính ngoài khoá.

**3NF:** sau CH3 đến CH5 không còn cột suy ra được hoặc trùng lặp. Các điểm còn lại là có chủ ý:
- `bookings` có hai nhóm cột cùng cấu trúc (`check_in_*`, `check_out_*` của học viên; `pt_check_in_*`, `pt_check_out_*` của PT). Mỗi buổi tập luôn có đúng hai người tham gia với vai trò cố định, và hai nhóm này có luật nghiệp vụ khác nhau (PT điểm danh để tính công, học viên điểm danh để tránh bị tính vắng). Phương án thay thế là tách bảng `booking_attendances`, chỉ nên làm nếu giảng viên yêu cầu chặt vì Agent 3 và 4 đã code theo cấu trúc hiện tại.
- `unit_price`, `payment_transactions.amount`, `commissions.amount` là giá trị lịch sử tại thời điểm giao dịch, không phải dữ liệu dư thừa.
- `member_packages.end_date` được chốt lúc thanh toán; đổi thời hạn gói sau đó không ảnh hưởng gói đã bán.
- `face_profiles.descriptor` là một vector đặc trưng khuôn mặt, chỉ so sánh nguyên khối, không truy vấn từng phần tử.

### Nội dung slide: các cột suy ra được nhưng cố ý giữ

| Cột | Vì sao có vẻ suy ra được | Vì sao vẫn giữ |
|---|---|---|
| `member_packages.end_date` | = ngày bắt đầu + thời hạn gói | Ngày bắt đầu chỉ biết lúc thanh toán xong, thời hạn gói có thể đổi sau đó; đây là giá trị đã chốt cho gói đã bán và là căn cứ để hết hạn |
| `member_packages.remaining_sessions` | = số buổi của gói trừ số buổi đã dùng | Là trạng thái theo thời điểm đăng ký, không suy lại được khi gói đổi (nâng, hạ cấp, đổi số buổi); đếm lại từ lịch sử `bookings` dễ sai và chậm |
| `commissions.amount` | = mức lương trong `trainer_pay_rates` | Bản chụp tại thời điểm chốt buổi; đổi mức lương sau đó không được làm đổi công đã tính |
| `pos_order_items.unit_price` | = giá hiện tại trong `products.price` | Bản chụp giá lúc bán; đổi giá sản phẩm sau đó không được làm đổi hoá đơn cũ |
| `bookings.end_time` | = giờ bắt đầu + độ dài buổi | Cho phép các buổi có độ dài khác nhau, không có độ dài cố định để suy ra |

## 5. Mỗi bảng phục vụ những user story nào

| Bảng | Số story | Các story (tối đa 10 ID đầu) |
|---|---|---|
| `roles` | 6 | G1, G2, R22, S4, A18, A19 |
| `users` | 16 | G1, G2, M1, T1, T14, R1, R22, S3, S4, S5, ... |
| `user_roles` | 6 | G1, G2, R22, S4, A18, A19 |
| `branches` | 3 | R10, A13, A21 |
| `user_profiles` | 7 | M1, M2, M3, T1, T2, T14, R22 |
| `packages` | 20 | G3, M4, M5, M6, M7, M8, M11, M12, R1, R2, ... |
| `member_packages` | 20 | M4, M5, M6, M7, M8, M11, M12, M13, T11, R1, ... |
| `payment_transactions` | 13 | M4, M7, M8, M9, R1, R2, R3, R4, R5, R6, ... |
| `trainer_schedules` | 10 | M10, M11, M12, T3, T4, T5, T6, R7, R8, A15 |
| `bookings` | 22 | M10, M11, M12, M13, M14, M16, M17, T6, T7, T8, ... |
| `face_profiles` | 13 | M15, M16, T9, T12, R11, R12, R13, R14, R15, R23, ... |
| `staff_attendances` | 5 | R15, R16, S6, S7, A12 |
| `gym_check_ins` | 4 | M16, M17, R14, A7 |
| `products` | 6 | R17, R18, R19, R20, R21, A17 |
| `pos_orders` | 3 | R5, R20, R21 |
| `pos_order_items` | 2 | R20, R21 |
| `trainer_pay_rates` | 4 | T11, T13, A10, A11 |
| `commissions` | 4 | T11, T13, A9, A11 |
| `leads` | 5 | S1, S2, S3, S4, A22 |

## 6. Ma trận user story → bảng dữ liệu

(*) là story chưa có endpoint tại thời điểm viết, đã đưa vào phạm vi, xem `USER_STORIES.md` mục 3. `gym_check_ins` là bảng mới của v2.

### Mọi người dùng đã đăng nhập

| ID | Use case | Bảng dữ liệu |
|---|---|---|
| C1 | Đăng xuất hệ thống | không dùng bảng (token lưu Redis) |

### Khách

| ID | Use case | Bảng dữ liệu |
|---|---|---|
| G1 | Đăng ký tài khoản | `users`, `roles`, `user_roles` |
| G2 | Đăng nhập hệ thống | `users`, `user_roles`, `roles` |
| G3 | Xem danh sách gói tập (*) | `packages` |

### Hội viên (MEMBER)

| ID | Use case | Bảng dữ liệu |
|---|---|---|
| M1 | Xem hồ sơ cá nhân | `users`, `user_profiles` |
| M2 | Cập nhật hồ sơ cá nhân | `user_profiles` |
| M3 | Tải lên ảnh đại diện | `user_profiles` |
| M4 | Mua gói tập | `packages`, `member_packages`, `payment_transactions` |
| M5 | Xem gói tập hiện tại | `member_packages`, `packages` |
| M6 | Xem lịch sử gói tập | `member_packages`, `packages` |
| M7 | Nâng cấp gói tập | `member_packages`, `packages`, `payment_transactions` |
| M8 | Hạ cấp gói tập | `member_packages`, `packages`, `payment_transactions` |
| M9 | Xem lịch sử thanh toán | `payment_transactions` |
| M10 | Xem lịch trống của PT | `trainer_schedules`, `bookings` |
| M11 | Đặt lịch tập với PT | `bookings`, `trainer_schedules`, `member_packages`, `packages` |
| M12 | Sửa lịch tập | `bookings`, `trainer_schedules`, `member_packages`, `packages` |
| M13 | Hủy lịch tập | `bookings`, `member_packages` |
| M14 | Xem lịch tập của tôi | `bookings` |
| M15 | Đăng ký khuôn mặt | `face_profiles` |
| M16 | Điểm danh vào phòng tập bằng khuôn mặt | `face_profiles`, `gym_check_ins`, `bookings` |
| M17 | Xem lịch sử check-in (*) | `gym_check_ins`, `bookings` |

### Huấn luyện viên (TRAINER)

| ID | Use case | Bảng dữ liệu |
|---|---|---|
| T1 | Xem hồ sơ cá nhân | `users`, `user_profiles` |
| T2 | Cập nhật hồ sơ cá nhân | `user_profiles` |
| T3 | Tạo ca làm việc | `trainer_schedules` |
| T4 | Sửa ca làm việc | `trainer_schedules` |
| T5 | Xóa ca làm việc | `trainer_schedules` |
| T6 | Xem lịch dạy | `bookings`, `trainer_schedules` |
| T7 | Xác nhận lịch tập | `bookings` |
| T8 | Hủy lịch tập | `bookings` |
| T9 | Điểm danh khuôn mặt buổi dạy | `face_profiles`, `bookings` |
| T10 | Nhập lý do rời buổi sớm | `bookings` |
| T11 | Hoàn thành buổi tập | `bookings`, `member_packages`, `commissions`, `trainer_pay_rates` |
| T12 | Đăng ký khuôn mặt | `face_profiles` |
| T13 | Xem lương theo buổi | `commissions`, `trainer_pay_rates`, `bookings` |
| T14 | Xem chỉ số hội viên đang huấn luyện | `bookings`, `users`, `user_profiles` |

### Lễ tân (RECEPTIONIST)

| ID | Use case | Bảng dữ liệu |
|---|---|---|
| R1 | Bán gói tập cho hội viên | `member_packages`, `packages`, `payment_transactions`, `users` |
| R2 | Gia hạn gói tập | `member_packages`, `packages`, `payment_transactions` |
| R3 | Nâng cấp gói tập hộ hội viên | `member_packages`, `packages`, `payment_transactions` |
| R4 | Hạ cấp gói tập hộ hội viên | `member_packages`, `packages`, `payment_transactions` |
| R5 | Xác nhận thanh toán tại quầy | `payment_transactions`, `member_packages`, `pos_orders` |
| R6 | Xem danh sách giao dịch theo ngày | `payment_transactions` |
| R7 | Đặt lịch tập hộ hội viên | `bookings`, `trainer_schedules`, `member_packages`, `packages` |
| R8 | Sửa lịch tập hộ hội viên | `bookings`, `trainer_schedules`, `member_packages`, `packages` |
| R9 | Hủy lịch tập hộ hội viên | `bookings`, `member_packages` |
| R10 | Xem danh sách lịch tập theo ngày | `bookings`, `branches` |
| R11 | Đăng ký khuôn mặt hộ hội viên | `face_profiles` |
| R12 | Xem hồ sơ khuôn mặt | `face_profiles` |
| R13 | Xóa hồ sơ khuôn mặt | `face_profiles` |
| R14 | Điểm danh hội viên tại kiosk | `face_profiles`, `gym_check_ins`, `bookings` |
| R15 | Chấm công bằng khuôn mặt | `face_profiles`, `staff_attendances` |
| R16 | Xem bảng chấm công của tôi | `staff_attendances` |
| R17 | Thêm sản phẩm bán lẻ | `products` |
| R18 | Cập nhật sản phẩm bán lẻ | `products` |
| R19 | Xem danh sách sản phẩm | `products` |
| R20 | Tạo đơn bán lẻ | `pos_orders`, `pos_order_items`, `products`, `payment_transactions` |
| R21 | Xem danh sách đơn bán lẻ | `pos_orders`, `pos_order_items`, `products` |
| R22 | Tạo tài khoản hội viên tại quầy (*) | `users`, `roles`, `user_roles`, `user_profiles` |
| R23 | Đăng ký khuôn mặt | `face_profiles` |

### Nhân viên Sales/CSKH (SALES)

| ID | Use case | Bảng dữ liệu |
|---|---|---|
| S1 | Thêm khách tiềm năng | `leads` |
| S2 | Cập nhật thông tin khách tiềm năng | `leads` |
| S3 | Xem danh sách khách tiềm năng | `leads`, `users` |
| S4 | Chuyển đổi khách tiềm năng thành hội viên | `leads`, `users`, `user_roles`, `roles` |
| S5 | Xem hội viên sắp hết hạn | `member_packages`, `users`, `packages` |
| S6 | Chấm công bằng khuôn mặt | `face_profiles`, `staff_attendances` |
| S7 | Xem bảng chấm công của tôi | `staff_attendances` |
| S8 | Đăng ký khuôn mặt | `face_profiles` |

### Quản trị viên (SUPER_ADMIN)

| ID | Use case | Bảng dữ liệu |
|---|---|---|
| A1 | Thêm gói tập | `packages` |
| A2 | Sửa gói tập | `packages` |
| A3 | Ngừng bán gói tập | `packages` |
| A4 | Xem toàn bộ gói tập | `packages` |
| A5 | Xem báo cáo doanh thu | `payment_transactions` |
| A6 | Xem báo cáo tỷ lệ gia hạn | `member_packages` |
| A7 | Xem báo cáo lưu lượng check-in | `gym_check_ins`, `bookings` |
| A8 | Xem báo cáo gói bán chạy | `member_packages`, `packages` |
| A9 | Xem báo cáo tổng lương PT | `commissions`, `users` |
| A10 | Cấu hình mức lương theo buổi cho PT | `trainer_pay_rates`, `users` |
| A11 | Xem lương PT theo tháng | `commissions`, `trainer_pay_rates` |
| A12 | Xem báo cáo chấm công nhân viên | `staff_attendances`, `users` |
| A13 | Xem danh sách lịch tập toàn hệ thống | `bookings`, `branches` |
| A14 | Xem danh sách giao dịch toàn hệ thống | `payment_transactions` |
| A15 | Quản lý ca làm việc của PT | `trainer_schedules` |
| A16 | Quản lý hồ sơ khuôn mặt | `face_profiles` |
| A17 | Quản lý sản phẩm bán lẻ | `products` |
| A18 | Tạo tài khoản nhân viên (*) | `users`, `roles`, `user_roles` |
| A19 | Phân quyền tài khoản (*) | `user_roles`, `roles` |
| A20 | Khóa tài khoản (*) | `users` |
| A21 | Quản lý chi nhánh (*) | `branches` |
| A22 | Xem danh sách khách tiềm năng | `leads`, `users` |

## 7. Việc cần làm tiếp

1. **Agent 2:** story G3 (mã và test đã xong, chờ PR) và CH1 sau khi V5 merge.
2. **Agent 1:** V9 (CH3, CH4, `phone`), `createMemberAccount`, module nhân sự và chi nhánh, quy tắc mở `GET /packages` trong `SecurityConfig`, cập nhật `DATABASE_SCHEMA.md` và `docs/ERD_TONG_QUAT.md` thành v2, Activity Diagram cho Bước 1.
3. **Agent 4:** CH2 và M17 sau PR #43. **Agent 5:** V5 không còn `subtotal`, `total_amount`. **Agent 6:** báo cáo lưu lượng lấy hợp `gym_check_ins` và `bookings.check_in_time`.
4. Vẽ lại 5 sơ đồ trên dbdiagram từ các file `gms_v2_*.dbml`.
