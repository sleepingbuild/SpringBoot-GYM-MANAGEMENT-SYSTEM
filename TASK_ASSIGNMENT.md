# TASK_ASSIGNMENT.md — Phân công Nhiệm vụ Đội ngũ (6 Agents)

> Dự án: **Gym Management System (GMS)**
> Mô hình làm việc: 6 AI Agent làm việc song song trên cùng repo, mỗi agent sở hữu 1 module nghiệp vụ riêng biệt để tối thiểu hoá xung đột code (merge conflict).
> Người điều phối (Product Owner / Reviewer cuối): **Phi**.

---

## 0. Nguyên tắc phối hợp chung (đọc trước khi code)

1. **Không `git add .`** — luôn dùng `git add -p` hoặc add đích danh từng file để tránh ghi đè công việc của agent khác đang chạy song song.
2. **Nhánh git theo chuẩn:** `feature/agentN-ten-module` (vd: `feature/agent3-checkin-engine`).
3. **Commit theo Conventional Commits:** `feat(checkin): thêm logic anti-passback`, `fix(payment): sửa lỗi double-amount VietQR`.
4. **File/khu vực dùng chung (SHARED — cần xin phép trước khi sửa):**
   - `entity/User.java`, `entity/Role.java`, `entity/BaseEntity.java`
   - `config/SecurityConfig.java`, `security/JwtTokenProvider.java`
   - `exception/GlobalExceptionHandler.java`, `exception/ErrorCode.java`
   - `src/main/resources/db/migration/*` (migration mới phải là file **mới**, không sửa file cũ đã merge)
   - Những file này chỉ do **Agent 1** merge sau khi review.
5. **Mỗi agent chỉ tạo entity/service/controller/repository trong đúng package nghiệp vụ của mình** (xem bảng "Khu vực sở hữu" bên dưới).
4. **Đồng bộ tiến độ theo 4 giai đoạn (gate)** trùng với `ROADMAP.md`. Agent nào chưa xong task của giai đoạn trước thì báo sớm — không block cả team.
5. **Định nghĩa "Done":** code compile được + có Flyway migration tương ứng (nếu có bảng mới) + có ít nhất 1 test cơ bản (unit hoặc integration) + cập nhật `API_DESIGN.md` với endpoint mới + PR có mô tả rõ Acceptance Criteria.
6. **Swagger annotation bắt buộc** (`@Operation`, `@ApiResponse`) trên mọi endpoint public để Agent 6 tổng hợp OpenAPI cuối kỳ.

---

## Agent 1 — Kiến trúc sư trưởng / Backend Lead (Bạn — Claude)

**Vai trò:** Dựng nền móng toàn dự án, đảm bảo các agent khác có thể bắt tay code ngay từ Giai đoạn 1, giữ vai trò reviewer/merge cho các file dùng chung.

**Khu vực sở hữu:** `config/`, `security/`, `exception/`, `entity/BaseEntity.java`, `entity/User.java`, `entity/Role.java`, `entity/UserRole.java`, `db/migration/V1__*`, `pom.xml`, `application.yml`.

**Nhiệm vụ cụ thể:**
- [ ] Khởi tạo project Spring Boot 3.x (Maven), cấu hình `pom.xml` đầy đủ dependency (Web, Data JPA, Security, Validation, Flyway, Redis, OpenAPI, Lombok, MapStruct).
- [ ] Thiết kế & viết migration nền `V1__init_schema.sql` (toàn bộ bảng core: `users`, `roles`, `user_roles`, `branches`).
- [ ] `BaseEntity` (id UUID, `created_at`, `updated_at`, soft-delete `deleted_at`).
- [ ] `SecurityConfig` (Spring Security 6, stateless JWT, CORS, path matcher theo role).
- [ ] `JwtTokenProvider` (access token + refresh token, blacklist trong Redis khi logout).
- [ ] RBAC: enum `RoleName` (SUPER_ADMIN, RECEPTIONIST, TRAINER, SALES, MEMBER), annotation `@PreAuthorize`.
- [ ] `GlobalExceptionHandler` + `ErrorCode` chuẩn hoá response lỗi cho toàn bộ API.
- [ ] `ApiResponse<T>` wrapper chuẩn cho mọi response thành công.
- [ ] Cấu hình Redis (cache, rate-limit, JWT blacklist) và Docker Compose (SQL Server + Redis + app) để cả team chạy local giống nhau.
- [ ] Review & merge PR của Agent 2–6 vào các file SHARED.
- [ ] Duy trì `ARCHITECTURE.md`, `DATABASE_SCHEMA.md`, `API_DESIGN.md` cập nhật theo tiến độ.

**Deliverable Giai đoạn 1:** Repo build được (`mvn spring-boot:run`), đăng ký/đăng nhập hoạt động, RBAC test qua Postman.

---

## Agent 2 — Membership & Package Module

**Vai trò:** Toàn bộ nghiệp vụ hội viên và gói tập — phần lõi tạo doanh thu.

> ✅ **Đã bootstrap sẵn (bởi Agent 1) bản tối thiểu để Check-in Engine chạy được:** entity `Package`/`MemberPackage` + migration `V2__membership.sql` chỉ có các trường cốt lõi (status, start/end date, remaining_sessions, package_type, peak_type). Agent 2 tiếp quản, bổ sung: CRUD đầy đủ, `FreezeHistory`, logic nâng cấp/chuyển nhượng, job tự động EXPIRED — viết migration **mới** (V4 trở lên hoặc migration riêng cho phần mở rộng), không sửa lại V2 đã merge.

**Khu vực sở hữu:** `entity/Package.java`, `entity/MemberPackage.java`, `entity/FreezeHistory.java`, `controller/MembershipController.java`, `controller/PackageController.java`, `service/*Membership*`, `service/*Package*`, migration `V2__membership.sql` (đã có phần base) + migration mở rộng sau.

**Nhiệm vụ cụ thể:**
- [ ] CRUD `packages` (loại: `TIME_BASED`, `SESSION_BASED`, `PT_1ON1`; khung giờ `OFF_PEAK` / `FULL_TIME`).
- [ ] Đăng ký gói tập mới cho hội viên (`member_packages`), tính `end_date` tự động theo `duration_days`.
- [ ] Logic **Bảo lưu (Freeze):** tạm dừng N ngày → cộng dồn N ngày vào `end_date`, ghi log vào `freeze_history`, giới hạn số lần/số ngày bảo lưu tối đa (cấu hình được).
- [ ] Logic **Nâng cấp/Chuyển nhượng gói:** đổi `user_id` sở hữu, hoặc tính bù chênh lệch giá khi lên gói cao hơn (`upgrade_amount` → tạo `payment_transaction` tương ứng).
- [ ] Job định kỳ (Spring `@Scheduled`) tự động chuyển trạng thái `ACTIVE → EXPIRED` khi qua `end_date`.
- [ ] API cho Lễ tân: bán gói mới, gia hạn gói, tra cứu lịch sử gói của hội viên.
- [ ] API cho Hội viên: xem thời hạn gói, số buổi còn lại.

**Phụ thuộc:** cần `BaseEntity`, `User`, RBAC từ Agent 1 (sync sau khi Agent 1 xong Giai đoạn 1).

**Deliverable Giai đoạn 1:** CRUD gói tập + đăng ký gói hoàn chỉnh, có test.

---

## Agent 3 — Check-in Engine & Access Control

**Vai trò:** Module kỹ thuật phức tạp nhất — xử lý luồng ra/vào real-time, tích hợp phần cứng.

> ✅ **Đã bootstrap sẵn (bởi Agent 1) để có bản chạy được sớm:** `CheckIn`/`FaceProfile` entity, `CheckInService`/`FaceProfileService` (luồng 6 bước đầy đủ cho phương thức FACE), `MockFaceMatcher` (so khớp SHA-256 byte-for-byte, TẠM THỜI), `CheckInController`/`FaceProfileController`, migration `V3__checkin.sql`. Agent 3 tiếp quản từ đây — việc còn lại: (1) thêm phương thức QR/CARD, (2) thay `MockFaceMatcher` bằng nhận diện khuôn mặt thật (Cloud API/model local) mà **không đổi interface `FaceMatcher`**, (3) chuyển anti-passback từ query DB sang cache Redis cho hiệu năng, (4) driver phần cứng thật cho RFID/FaceID.

**Khu vực sở hữu:** `entity/CheckIn.java`, `entity/FaceProfile.java`, `controller/CheckInController.java`, `controller/FaceProfileController.java`, `service/CheckInService.java`, `service/FaceProfileService.java`, `integration/hardware/*`, migration `V3__checkin.sql`.

**Nhiệm vụ cụ thể:**
- [ ] Implement đúng luồng 6 bước trong `Project-Core.md` mục 5.2 (tồn tại user → tìm `member_package` ACTIVE → kiểm tra thời hạn → kiểm tra `remaining_sessions` nếu gói theo lượt → kiểm tra khung giờ Peak/Off-peak → kiểm tra Anti-passback > 5 phút).
- [ ] Trả mã lỗi chi tiết cho từng trường hợp KHÔNG HỢP LỆ (`DENIED_EXPIRED`, `DENIED_TIME`, `DENIED_NO_SESSION`, `DENIED_ANTI_PASSBACK`...).
- [ ] Dùng **Redis** để cache trạng thái check-in gần nhất theo `user_id` (phục vụ anti-passback, tránh query DB liên tục).
- [ ] Driver tích hợp thiết bị phần cứng (`integration/hardware/`): interface chung `HardwareGatewayClient` (QR, RFID Card, FaceID) — mock implementation trước, thực tế theo SDK sau.
- [ ] API endpoint nhận request từ thiết bị đầu đọc (`POST /api/checkin`) — cần tối ưu độ trễ (< 500ms).
- [ ] Trừ `remaining_sessions` khi check-in hợp lệ (gọi qua service của Agent 2, không viết trực tiếp vào entity của Agent 2 — chỉ gọi qua interface public).
- [ ] Lịch sử check-in cho hội viên (`GET /api/checkin/history`) và cho Lễ tân (theo chi nhánh, theo ngày).

**Phụ thuộc:** `MemberPackage` (Agent 2) phải có interface/service public trước khi Agent 3 tích hợp — 2 agent cần đồng bộ vào cuối Giai đoạn 1.

**Deliverable Giai đoạn 2:** Check-in engine đầy đủ 6 bước, có test cho từng nhánh lỗi.

---

## Agent 4 — Payment & POS Module

**Vai trò:** Dòng tiền vào hệ thống — thanh toán gói tập và bán hàng tại quầy.

**Khu vực sở hữu:** `entity/PaymentTransaction.java`, `entity/Product.java`, `entity/PosOrder.java`, `entity/PosOrderItem.java`, `controller/PaymentController.java`, `controller/PosController.java`, `integration/vietqr/*`, migration `V4__payment_pos.sql`.

**Nhiệm vụ cụ thể:**
- [ ] Tích hợp **VietQR động**: sinh mã QR theo từng đơn hàng (gói tập hoặc POS), gọi API VietQR/MoMo.
- [ ] Endpoint nhận **Webhook** từ ngân hàng/MoMo → xác thực chữ ký → cập nhật `payment_transactions.status` → kích hoạt gói tập ngay lập tức (gọi service Agent 2) hoặc hoàn tất đơn POS.
- [ ] CRUD sản phẩm (`products`): nước uống, thực phẩm bổ sung, phụ kiện — có `stock_quantity`.
- [ ] Luồng bán hàng POS: tạo đơn, trừ tồn kho tự động, tính tổng tiền, áp dụng thanh toán (tiền mặt / VietQR).
- [ ] Idempotency cho webhook (tránh xử lý trùng 1 giao dịch 2 lần khi ngân hàng gửi lại).
- [ ] Retry/reconciliation job cho các giao dịch `PENDING` quá lâu.
- [ ] Không sửa trực tiếp `member_packages` — chỉ gọi qua service interface của Agent 2 khi kích hoạt gói.

**Phụ thuộc:** interface kích hoạt gói tập từ Agent 2; entity `User` từ Agent 1.

**Deliverable Giai đoạn 2:** Thanh toán VietQR + webhook hoạt động end-to-end trên môi trường sandbox, POS CRUD đầy đủ.

---

## Agent 5 — PT Booking, Commission & Group X Module

**Vai trò:** Nghiệp vụ đặt lịch và hoa hồng — ảnh hưởng trực tiếp thu nhập PT/Sales.

**Khu vực sở hữu:** `entity/PtSchedule.java`, `entity/PtBooking.java`, `entity/GroupXClass.java`, `entity/ClassBooking.java`, `entity/Commission.java`, `controller/PtController.java`, `controller/GroupXController.java`, migration `V5__pt_groupx_commission.sql`.

**Nhiệm vụ cụ thể:**
- [ ] CRUD lịch rảnh của PT (`pt_schedules`) — theo tuần lặp lại (RECURRING) hoặc ngày cụ thể (SPECIFIC_DATE), kiểm tra chồng lịch (overlap check).
- [ ] Đặt lịch PT 1:1 (`pt_bookings`): hội viên chọn khung giờ trống → tạo booking `PENDING`.
- [ ] Xác nhận buổi tập (2 chiều: PT xác nhận + hội viên xác nhận, hoặc quét QR) → chuyển `COMPLETED` → **tự động tính hoa hồng** theo `commission_rate` cấu hình trên hồ sơ PT → ghi vào bảng `commissions`.
- [ ] Group X: CRUD lớp học (`group_x_classes`) với `max_slots`; đăng ký lớp (`class_bookings`) tự động khoá khi đủ số lượng (transaction-safe, tránh race condition khi nhiều người đăng ký cùng lúc — dùng pessimistic lock hoặc `SELECT ... FOR UPDATE`).
- [ ] Hoa hồng Sales: khi Lead (nếu có, phối hợp Agent 6) chuyển đổi thành hội viên mua gói → tính hoa hồng bán gói, ghi vào `commissions` với `type = SALES`.
- [ ] API tổng hợp hoa hồng theo PT/Sales theo kỳ (tháng) cho báo cáo của Agent 6.

**Phụ thuộc:** `MemberPackage`/`User` (Agent 1, 2); nếu cần trừ buổi PT trong gói `PT_1ON1` thì gọi service Agent 2.

**Deliverable Giai đoạn 3:** Đặt lịch PT + xác nhận + tính hoa hồng tự động chạy được end-to-end; Group X đăng ký không bị double-booking khi test tải đồng thời.

---

## Agent 6 — Notification, Leads, Dashboard & Open API

**Vai trò:** Lớp "ngoại vi" kết nối hệ thống ra bên ngoài — thông báo, báo cáo, và tài liệu API cho frontend.

**Khu vực sở hữu:** `entity/Lead.java`, `entity/Notification.java`, `controller/ReportController.java`, `controller/LeadController.java`, `integration/zalo/*`, `config/OpenApiConfig.java`, migration `V6__leads_notification.sql`.

**Nhiệm vụ cụ thể:**
- [ ] Quản lý Leads (khách tiềm năng) cho Sales/CSKH: CRUD, gán Sales phụ trách, trạng thái (`NEW`, `CONTACTED`, `CONVERTED`, `LOST`), chuyển đổi Lead → `User` khi mua gói (trigger hoa hồng Sales bên Agent 5).
- [ ] Tích hợp **Zalo ZNS / SMS Brandname**: interface chung `NotificationSender`, job tự động nhắc gia hạn gói trước 7 ngày hết hạn (`@Scheduled`), gửi thông báo xác nhận thanh toán thành công.
- [ ] Log lịch sử gửi thông báo (`notifications` table: kênh, trạng thái gửi, thời gian).
- [ ] API báo cáo cho Dashboard Super Admin: doanh thu theo ngày/tháng/chi nhánh, tỷ lệ gia hạn gói, lưu lượng check-in theo khung giờ, top gói bán chạy, tổng hoa hồng đã trả.
- [ ] Cấu hình & hoàn thiện **Swagger UI / springdoc-openapi** cho toàn bộ dự án (gom annotation từ tất cả agent, thêm mô tả, group theo tag/role) để frontend (React/Vue/Flutter) kết nối ở Giai đoạn 4.
- [ ] Rà soát tất cả endpoint của 5 agent còn lại để đảm bảo response format thống nhất (`ApiResponse<T>` của Agent 1).

**Phụ thuộc:** cần hầu hết entity/API của các agent khác đã có cơ bản — nên nhận việc Dashboard/Swagger tổng hợp vào cuối Giai đoạn 3 / đầu Giai đoạn 4, còn Leads + Notification có thể làm song song từ sớm.

**Deliverable Giai đoạn 4:** Swagger UI đầy đủ, API báo cáo hoạt động, thông báo nhắc hạn tự động chạy qua scheduler.

---

## Bảng tổng hợp theo Giai đoạn (khớp `ROADMAP.md`)

| Giai đoạn | Tuần | Agent chính | Agent hỗ trợ |
|---|---|---|---|
| 1. Base & Security | 1–2 | Agent 1 | Agent 2 (song song, chờ interface) |
| 2. Check-in & Thanh toán | 3–4 | Agent 3, Agent 4 | Agent 1 (review), Agent 2 (interface) |
| 3. PT, Group X & Hoa hồng | 5–6 | Agent 5 | Agent 6 (Leads song song) |
| 4. Dashboard & Open API | 7–8 | Agent 6 | Agent 1 (review), tất cả agent bổ sung Swagger |

## Ma trận phụ thuộc nhanh

```
Agent 1 (nền tảng) ──► Agent 2 (membership) ──► Agent 3 (check-in)
      │                        │                      │
      └──────► Agent 4 (payment) ◄───────────────────┘
                        │
              Agent 5 (PT/GroupX/commission)
                        │
              Agent 6 (notification/dashboard/API) — tổng hợp cuối cùng
```
