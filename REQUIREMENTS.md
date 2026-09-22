# REQUIREMENTS.md — Đặc tả đầy đủ (bản làm lại, chỉnh chu)

> Nguồn: (1) `Project-Core.md` gốc, (2) 5 báo cáo tổng hợp công việc từ dự án song song ASP.NET Core (`ironfit-pro-aspnet`), (3) slide "Phân quyền theo vai trò".
> Mục đích file này: **spec độc lập với ngôn ngữ/stack** — dịch lại toàn bộ business rule + bug đã phát hiện bên .NET thành yêu cầu rõ ràng, để bản Spring Boot làm đúng ngay từ đầu, không phải tự khám phá lại cùng những lỗi đó.

---

## 1. Phân quyền theo vai trò (5 role — khớp sẵn với `V1__init_schema.sql`)

| Role | Phạm vi |
|---|---|
| **SUPER_ADMIN** | Cấu hình hệ thống, quản lý nhân sự (mọi role khác), báo cáo tài chính toàn hệ thống |
| **RECEPTIONIST** (Lễ tân/Thu ngân) | Chấm công FaceID (của chính mình), bán gói tập, check-in hội viên, bán lẻ (POS), chốt ca |
| **SALES** (Sales/CSKH) | Quản lý lead, hoa hồng bán gói, chăm sóc hội viên sắp hết hạn |
| **TRAINER** (Huấn luyện viên) | Lịch dạy, xác nhận buổi tập (chỉ khi member đã face check-in), chỉ số hội viên đang huấn luyện |
| **MEMBER** (Hội viên) | Mua gói online, đặt lịch tập, xem lịch sử check-in |

**Quan trọng:** Xác nhận buổi tập của PT **không yêu cầu PT quét mặt** — chỉ member quét mặt, PT xác nhận bằng thao tác tay sau khi thấy `CheckInTime` đã có giá trị.

---

## 2. Module: Membership & Package

### State machine (khác hẳn thiết kế "chỉ có ACTIVE/EXPIRED" ban đầu — đây là bản đã tinh chỉnh qua thực tế .NET)
```
Đăng ký mới  → PENDING (chờ thanh toán)
Thanh toán xong → ACTIVE (start/end tính từ LÚC THANH TOÁN THÀNH CÔNG, không phải lúc bấm đăng ký)
Quá end_date → EXPIRED (tự động, lazy-check khi có người đọc dữ liệu — KHÔNG cần cron/background job)
```

### Nâng cấp / Hạ cấp (2 luồng khác nhau — dễ nhầm nếu không tách rõ)
- **Nâng cấp** (giá gói mới ≥ giá gói đang ACTIVE): hủy gói cũ ngay lập tức, tạo gói mới `PENDING`, active ngay khi thanh toán xong.
- **Hạ cấp** (giá gói mới thấp hơn): **không đụng gói hiện tại** — tạo bản ghi trạng thái `SCHEDULED`, bắt đầu đúng ngày gói hiện tại hết hạn; tự chuyển `SCHEDULED → ACTIVE` khi tới ngày (cùng cơ chế lazy-check).
- Chặn đăng ký gói mới nếu đang có gói `PENDING` (tránh rác); **không** chặn nếu đang có gói `ACTIVE` (bug cũ ở .NET: từng chặn nhầm, đã bỏ).

### Nguồn dữ liệu "gói hiện tại" — PHẢI dùng 1 hàm service DUY NHẤT
⚠️ **Bug đã xảy ra ở .NET:** trang "Gói tập của tôi" và trang "Thẻ tập" từng lấy dữ liệu từ 2 nguồn khác nhau (1 bên định nghĩa "gói hiện tại" = thanh toán gần nhất, 1 bên = `status='ACTIVE'`) → hiển thị lệch nhau. **Quy tắc: viết đúng 1 method `getCurrentMembership(userId)` dùng chung cho MỌI nơi cần hiển thị gói đang dùng, định nghĩa duy nhất = `status = 'ACTIVE'`.**

### Giới hạn buổi/tuần theo gói (`max_sessions_per_week`)
- `NULL` → không giới hạn
- `0` → không cho đặt lịch với PT
- `N` → tối đa N buổi/tuần (đếm theo tuần Thứ 2 → Chủ nhật)
- Áp dụng cho **cả 3 luồng**: member tự đặt, lễ tân/admin đặt hộ, và sửa lịch — thiếu 1 luồng nào là hổng.

### Thanh toán
- Xác nhận **tại chỗ** (không redirect cổng ngoài) — VietQR/MoMo có thể để làm sau nếu cần demo thật, nhưng luồng mặc định là admin/lễ tân xác nhận đã nhận tiền → kích hoạt ngay.

---

## 3. Module: Lịch làm việc PT & Đặt lịch (Booking)

### TrainerSchedule — theo NGÀY CỤ THỂ, không theo "Thứ trong tuần"
⚠️ **Quyết định thiết kế quan trọng từ .NET** (đã đổi model 1 lần vì lý do cụ thể): ban đầu dùng `DayOfWeek` (lặp lại hàng tuần) → phát hiện lỗi 2 tuần khác nhau cùng Thứ 2 cùng giờ bị coi là trùng lịch dù thực tế là 2 ca riêng biệt. **→ Dùng `work_date` (DATE cụ thể) làm nguồn chân lý, kiểm tra trùng lịch theo đúng `work_date`, không theo thứ.**

### Validate tạo/sửa ca làm việc
- Chặn chọn **ngày quá khứ**.
- Nếu ngày = hôm nay → giờ bắt đầu phải **≥ giờ hiện tại** (validate cả client lẫn server).
- Xử lý **trùng giờ trong cùng ngày** bằng thuật toán xếp "lane" khi hiển thị lưới lịch (không cấm trùng giờ giữa 2 PT khác nhau, chỉ cấm trùng của cùng 1 PT).

### Đặt lịch (Booking) — các rule bắt buộc, đã qua thực tế phát hiện thiếu sót nhiều lần
1. **Chặn giờ quá khứ + quá gần hiện tại**: không cho đặt cho hôm nay nếu giờ đã qua hoặc còn dưới **30 phút** nữa. Áp dụng cho member tự đặt VÀ admin/lễ tân đặt hộ.
2. **Giới hạn slot**: tối đa **1 người/khung giờ/PT** (phiên bản .NET có lúc thử 2 người/slot rồi đổi lại 1 người — chốt là 1).
3. **Chặn member đặt trùng giờ với 2 PT khác nhau** cùng lúc — áp dụng cho tự đặt, đặt hộ, và sửa lịch.
4. **Không đặt được ngoài ca làm việc thật của PT** (đối chiếu `TrainerSchedule.work_date` + giờ).
5. **Hủy lịch**: không cho hủy buổi đã diễn ra (giờ bắt đầu đã qua).
6. ⚠️ **Thứ tự validate quan trọng**: 1 bug nghiêm trọng ở .NET là kiểm tra hợp lệ **trước khi** gán `user_id` vào booking, khiến mọi lần đặt lịch fail âm thầm. **→ Luôn gán đầy đủ các trường bắt buộc (đặc biệt FK) TRƯỚC khi chạy validate, không phải ngược lại.**

### Auto-status theo thời gian — lazy-check (không cần Background Service/cron)
Chạy kiểm tra mỗi khi có người đọc danh sách/chi tiết booking (đủ dùng cho quy mô đồ án, không cần job nền):
- `PENDING` còn ≤ 1 tiếng chưa được PT xác nhận → tự động `CANCELLED`.
- `CONFIRMED` quá 30 phút từ giờ hẹn mà PT chưa đánh dấu hoàn thành → tự động `PT_NO_SHOW`.
- Quá giờ kết thúc mà **member chưa face check-in** (`check_in_time IS NULL`) → tự động `NO_SHOW`.
- `NO_SHOW`/`PT_NO_SHOW` **khác** `CANCELLED` (Cancelled chỉ dành cho PT/member **chủ động** hủy trước giờ).

### Xác nhận "học viên đã tập" (PT)
- Nút chỉ hiện khi `booking.check_in_time` đã có giá trị. Chưa điểm danh → hiện cảnh báo lý do (không ẩn lặng lẽ).
- **Chặn ở server** (không chỉ ẩn UI) — đề phòng gọi API trực tiếp bypass giao diện.

---

## 4. Module: Điểm danh khuôn mặt (Face Attendance) — làm ĐÚNG ngay từ đầu, không mock

⚠️ Đây là điểm khác biệt lớn nhất so với bản Spring Boot cũ (`MockFaceMatcher` so hash byte-for-byte) — bản làm lại phải theo đúng kiến trúc đã chứng minh hiệu quả bên .NET:

### Kiến trúc
- **Trích đặc trưng khuôn mặt ngay trên trình duyệt** (client-side), dùng thư viện JS nhận diện khuôn mặt (face-api.js/TensorFlow.js hoặc tương đương) → ra **vector đặc trưng (descriptor), 128 chiều**.
- **Client CHỈ gửi vector descriptor lên server, KHÔNG gửi ảnh thô mỗi lần quét** (nhanh hơn, đỡ băng thông) — ảnh thô chỉ cần lưu 1 lần lúc đăng ký.
- **Server tự tính khoảng cách Euclidean** giữa descriptor gửi lên và các descriptor đã đăng ký trong DB, **tự quyết định khớp hay không** — ⚠️ KHÔNG bao giờ tin kết quả "đã khớp với user X" do client tự gửi lên (lỗ hổng bảo mật nghiêm trọng đã từng tồn tại ở .NET bản đầu, đã sửa).
- **Ngưỡng khớp (match threshold): 0.45** (bắt đầu từ 0.55 nhưng bị nhận nhầm người → giảm xuống 0.45 — dùng luôn giá trị đã kiểm chứng này làm mặc định, không cần dò lại từ đầu).

### Dữ liệu
- `face_profiles`: `user_id` (UNIQUE — 1 người 1 hồ sơ, đăng ký lại = ghi đè), `descriptor` (JSON array 128 số thực), `registered_by`.

### 3 luồng
| Luồng | Ai dùng | Kiểu so khớp | Ghi chú |
|---|---|---|---|
| Kiosk | Lễ tân/Admin | 1:N (so với toàn bộ hồ sơ) | Camera cố định tại quầy/cửa |
| Tự điểm danh | Member + Trainer | 1:1 (chỉ so với chính tài khoản đang đăng nhập) | Trên máy cá nhân/điện thoại |
| Đăng ký hộ | Lễ tân/Admin | — | Upload ảnh tĩnh, không cần webcam lúc đăng ký |

### Check-in/Check-out thông minh
- **Không cần chọn trước** là check-in hay check-out — hệ thống tự xác định theo lượt quét trong ngày: **lần đầu = check-in, lần tiếp theo = check-out**. Áp dụng cho cả điểm danh buổi tập (Member/Booking) lẫn chấm công (Trainer/Receptionist).

### Bug đã biết — tránh lặp lại
- **Race condition khi quét liên tục**: nếu dùng polling/interval để liên tục chụp frame, phải set cờ "đang xử lý" (`busy`) **NGAY ĐẦU** vòng lặp/callback, **TRƯỚC** bước nhận diện bất đồng bộ — nếu set sau, 2 lượt xử lý có thể chồng nhau, gây check-in rồi check-out cùng lúc chỉ với 1 lần quét.
- **Múi giờ**: lưu và hiển thị nhất quán cùng 1 timezone (Asia/Ho_Chi_Minh) xuyên suốt — không trộn UTC và giờ local, sẽ gây lệch giờ hiển thị.

---

## 5. Module: Chấm công PT/Lễ tân (Staff Attendance)

- Bảng riêng biệt (không chung với `check_ins` của member): `staff_id`, `date`, `check_in_time`, `check_out_time`, `method` (Manual/Face), `notes` — **unique theo `(staff_id, date)`**.
- Trạng thái tính **động lúc xem báo cáo** (không lưu cứng vào DB): so `check_in_time`/`check_out_time` với ca quy định (`shift_start_time`/`shift_end_time` — cấu hình riêng theo từng nhân sự, mặc định 07:00–21:00 nếu để trống) → `Đúng giờ` / `Đi muộn` / `Về sớm` / `Đi muộn & Về sớm` / `Vắng mặt` / `Chưa đặt ca`. Ghi chú nêu rõ **số phút cụ thể**.
- "Về sớm" chỉ xác định được **sau khi đã check-out**.

---

## 6. Module mới hoàn toàn (không có tiền lệ ở .NET — đúng tinh thần "bổ sung")

### POS — Bán hàng tại quầy
- `products` (tên, danh mục, giá, tồn kho), `pos_orders`, `pos_order_items` — trừ tồn kho tự động khi tạo đơn.

### Commission — Hoa hồng tự động
- PT: tính khi buổi tập chuyển `COMPLETED`, theo `commission_rate` cấu hình.
- Sales: tính khi 1 Lead chuyển đổi thành công thành hội viên mua gói.

### Leads/CRM — Sales quản lý khách tiềm năng
- `leads` (tên, SĐT, nguồn, trạng thái NEW/CONTACTED/CONVERTED/LOST, sales phụ trách) + hành động "chuyển đổi thành hội viên" (tạo user thật + trigger hoa hồng).

---

## 7. Bài học chung (áp dụng xuyên suốt, không riêng module nào)

1. **1 nguồn chân lý cho mỗi khái niệm nghiệp vụ** — không để 2 chỗ tự định nghĩa lại cùng 1 thứ (bài học từ vụ "gói hiện tại" lệch dữ liệu ở mục 2).
2. **Validate SAU khi đã gán đủ dữ liệu bắt buộc**, không phải trước — tránh bug "fail âm thầm" ở mục 3.
3. **Lazy-check thay vì cron job** cho các chuyển trạng thái theo thời gian (đơn giản hơn, đủ dùng cho quy mô đồ án, không cần Background Service/Quartz).
4. **Không tin dữ liệu nhạy cảm do client tự gửi** (ai đã khớp mặt, có thanh toán thành công hay chưa...) — server luôn tự verify lại.
5. **Nhất quán timezone** — cố định `Asia/Ho_Chi_Minh` xuyên suốt toàn hệ thống.
6. **Định dạng số ở client/server phải khớp nhau** khi validate (bug từng gặp: input hiển thị `3,000,000` có dấu phẩy nhưng JS validate parse ra `NaN`).

---

## 8. Ghi chú kiến trúc riêng cho bản Spring Boot (khác với hướng bolt-on đã bỏ)

- Vì giờ là **hệ thống độc lập**, không cần cầu nối cookie/API-key với .NET nữa — giữ nguyên JWT tự cấp đã build (`JwtTokenProvider`, `AuthController`) từ bản trước.
- `entity/CheckIn.java`/`face_profiles` hiện tại cần đổi từ so khớp SHA-256 (mock) sang lưu **descriptor** — xem mục 4. Việc trích xuất descriptor vẫn làm ở client (JS), backend chỉ nhận mảng số thực và tính khoảng cách Euclidean (thuật toán đơn giản, không cần thư viện ML nặng ở phía Java).
- Toàn bộ ID vẫn dùng `UNIQUEIDENTIFIER` (UUID) như đã chọn — không còn ràng buộc phải khớp kiểu `string` của ASP.NET Identity nữa.
