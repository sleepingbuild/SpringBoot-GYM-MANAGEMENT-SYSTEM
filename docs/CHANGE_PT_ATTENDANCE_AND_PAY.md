# CHANGE REQUEST — PT điểm danh theo buổi, luật no-show mới, lương PT theo buổi

**Từ:** Agent 1 → Agent 2, 3, 4, 5, 6
**Ngày:** 2026-10-07 · **Nguồn:** quyết định của Phi
**Ưu tiên:** Agent 3 làm trước (PR chưa merge nên sửa luôn trong PR hiện tại), Agent 4 và 5 làm theo sau.

---

## 1. Quy tắc mới (thay các mục tương ứng trong `REQUIREMENTS.md` 1, 3, 6)

1. Buổi PT bắt đầu lúc **S**, kết thúc lúc **E**. Thời gian ân hạn = **15 phút**, cấu hình `gms.booking.no-show-grace-minutes: 15` (thay `pt-no-show-grace-minutes: 30`).
2. Sau **S+15** mà PT chưa quét mặt vào → `PT_NO_SHOW`.
3. Sau **S+15** mà PT đã quét vào nhưng học viên chưa → `NO_SHOW` (giữ tên cũ, không đổi `CLIENT_NO_SHOW` để khỏi đổi enum/DB/API).
4. Cả hai cùng vắng → `PT_NO_SHOW` (ưu tiên PT).
5. PT quét mặt ra **trước E** → hệ thống hiện ô nhập lý do. Không nhập = bị coi là **không dạy đủ thời gian**.
6. **Chỉ PT được tính lương theo buổi.** Lễ tân, Sales và các role khác lương cứng → **bỏ hoa hồng Sales**.
7. ⚠️ Đảo ngược quy tắc cũ: `REQUIREMENTS.md` mục 1 ghi "PT không cần quét mặt" — **không còn đúng**.
8. Học viên **huỷ muộn** (dưới 2 tiếng trước giờ bắt đầu) hoặc vắng không báo → coi là vắng: **mất 1 buổi trong gói**.
9. Huỷ muộn (booking `CONFIRMED`): status mới **`LATE_CANCELLED`**. PT **vẫn được tính lương** và **không cần đến/quét mặt**.
10. `NO_SHOW` (PT đã quét mặt vào, học viên không đến): mất 1 buổi, PT được tính lương.

## 2. Giả định tạm (Phi sửa nếu khác)

| # | Giả định | Ảnh hưởng nếu đổi |
|---|---|---|
| G1 | Có lý do → buổi vẫn tính đủ lương, lý do lưu lại để admin xem. Không lý do → `COMPLETED` nhưng **không tính lương** | Agent 5 |
| G2 | ✅ Đã chốt: `NO_SHOW` và `LATE_CANCELLED` đều trừ 1 buổi của học viên, PT vẫn được trả công | Agent 2, 5 |
| G3 | Role `TRAINER` quét mặt → chỉ ghi vào Booking, **không** ghi `staff_attendances` nữa (PT trả theo buổi) | Agent 4 |
| G4 | Hoàn thành buổi: PT phải đã quét vào **và** ra, học viên đã quét vào, status đang `CONFIRMED` | Agent 3 |
| G5 | Học viên chỉ check-in được vào booking `CONFIRMED` (không phải `PENDING`) | Agent 3 |
| G6 | PT chỉ check-in được trong khoảng `[S-30 phút, S+15 phút]` | Agent 3, 4 |
| G7 | Trừ buổi gói (`remaining_sessions`) khi booking `COMPLETED`, Hướng A: `createBooking` chặn trước nếu gói hết buổi | Agent 2, 3 |
| G8 | Học viên huỷ booking `PENDING` (PT chưa xác nhận) luôn miễn phí; quy tắc 2 tiếng chỉ áp dụng cho `CONFIRMED`. PT huỷ → `CANCELLED`, không lương, không trừ buổi | Agent 3 |
| G9 | Lễ tân/admin huỷ hộ học viên cũng áp dụng quy tắc 2 tiếng | Agent 3 |

## 3. Thay đổi theo agent

### Agent 3 (Booking), sửa trong PR hiện tại
- **Migration:** sửa thẳng `V3__schedule_booking.sql` (chưa merge nên được phép), thêm vào `bookings`:
  ```sql
  pt_check_in_time        DATETIME2 NULL,
  pt_check_in_method      VARCHAR(20) NULL,
  pt_check_out_time       DATETIME2 NULL,
  pt_check_out_method     VARCHAR(20) NULL,
  pt_early_leave_reason   NVARCHAR(255) NULL,
  ```
  Agent 3 và Agent 4 reset DB local: `docker compose down -v` rồi chạy lại để Flyway áp V3 mới.
- **Entity `Booking` + `BookingResponse`:** thêm 5 field trên, cộng field tính động `fullDuration`.
- **`applyLazyStatus` viết lại** (xoá nhánh `PT_NO_SHOW` cũ — nó khiến `NO_SHOW` gần như không bao giờ đạt được):
  ```java
  private void applyLazyStatus(Booking booking) {
      BookingStatus status = booking.getStatus();
      if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) return;

      LocalDateTime now = LocalDateTime.now(clock);
      LocalDateTime start = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());

      if (status == BookingStatus.PENDING
              && !now.isBefore(start.minusMinutes(pendingCancelThresholdMinutes))) {
          booking.setStatus(BookingStatus.CANCELLED);
          bookingRepository.save(booking);
          return;
      }
      if (status == BookingStatus.CONFIRMED && now.isAfter(start.plusMinutes(noShowGraceMinutes))) {
          if (booking.getPtCheckInTime() == null) {
              booking.setStatus(BookingStatus.PT_NO_SHOW);       // PT ưu tiên
          } else if (booking.getCheckInTime() == null) {
              booking.setStatus(BookingStatus.NO_SHOW);
          } else {
              return;
          }
          bookingRepository.save(booking);
      }
  }
  ```
- **`completeBooking`:** thêm các chốt chặn (trước đó hàm chỉ kiểm tra `checkInTime`, nên booking `CANCELLED` có `checkInTime` vẫn bị ghi đè thành `COMPLETED`):
  ```java
  if (booking.getStatus() != BookingStatus.CONFIRMED)
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ hoàn thành được booking đang CONFIRMED");
  if (booking.getCheckInTime() == null)
      throw new BusinessException(ErrorCode.BOOKING_NOT_CHECKED_IN, "...");
  if (booking.getPtCheckInTime() == null || booking.getPtCheckOutTime() == null)
      throw new BusinessException(ErrorCode.BOOKING_PT_NOT_CHECKED_IN, "PT phải quét mặt vào và ra trước khi hoàn thành");

  LocalDateTime end = LocalDateTime.of(booking.getBookingDate(), booking.getEndTime());
  boolean fullDuration = !booking.getPtCheckOutTime().isBefore(end)
          || (booking.getPtEarlyLeaveReason() != null && !booking.getPtEarlyLeaveReason().isBlank());
  ```
  Đổi tên event thành `BookingSettledEvent` và gom cả 3 kết cục:
  ```java
  public record BookingSettledEvent(UUID bookingId, UUID trainerId, UUID memberId,
          Outcome outcome, boolean fullDuration, LocalDateTime settledAt) {
      public enum Outcome { COMPLETED, MEMBER_NO_SHOW, LATE_CANCEL }
  }
  ```
  Publish ở 3 chỗ: `completeBooking` (`COMPLETED`, `fullDuration` tính như trên), `applyLazyStatus` khi lật sang `NO_SHOW` (`MEMBER_NO_SHOW`, `fullDuration = true`), `cancelBooking` huỷ muộn (`LATE_CANCEL`, `fullDuration = true`). Các method đọc có gọi `applyLazyStatus` không được để `readOnly = true`, nếu không `save` và event có thể không chạy.
- **Method mới cho Agent 4 gọi:**
  ```java
  Optional<TrainerScanResult> recordTrainerFaceScan(UUID trainerId, LocalDateTime scanTime);
  record TrainerScanResult(BookingResponse booking, String type /* CHECK_IN | CHECK_OUT */, boolean earlyLeave) {}
  ```
  Chọn booking theo thứ tự: (1) `CONFIRMED` hôm nay có `pt_check_in_time != null` và `pt_check_out_time == null` → **check-out**; (2) nếu không có, booking `CONFIRMED` có `pt_check_in_time == null` trong khoảng `[S-30, S+15]` → **check-in**. Gọi `applyLazyStatus` trước khi chọn. `earlyLeave = scanTime < E` khi check-out. Check-out luôn được ghi ngay, lý do nộp sau.
- **Endpoint mới:** `PUT /api/v1/bookings/{id}/early-leave-reason`, body `{ "reason": "..." }` (`@Size(min = 5)`), role `TRAINER`, chỉ booking của chính PT, chỉ khi `pt_check_out_time < E` và status còn `CONFIRMED` (nộp trước khi bấm hoàn thành).
- **`recordFaceScan` (học viên):** gọi `applyLazyStatus` trước khi chọn booking, bỏ qua booking vừa bị đổi trạng thái.
- **`cancelBooking`:** thêm `LATE_CANCELLED` vào enum `BookingStatus` (cột `VARCHAR`, không cần migration). Khi người huỷ là học viên hoặc lễ tân/admin hộ học viên, booking đang `CONFIRMED` và `start - now < 2 giờ` → status `LATE_CANCELLED` + publish `LATE_CANCEL`. Huỷ từ 2 giờ trở lên, hoặc booking `PENDING` → `CANCELLED` như cũ (G8). PT huỷ → `CANCELLED`. Cấu hình `gms.booking.late-cancel-hours: 2`.
- **`BookingRepository`:**
  - `findTodayPendingCheckIn` đổi điều kiện status thành chỉ `CONFIRMED`.
  - Thêm `findTodayConfirmedByTrainer(trainerId, today)`.
  - Hai query slot hiện chỉ so đúng `startTime`, không bắt được trùng khoảng giờ (10:00-11:00 và 10:30-11:30 vẫn lọt). Đổi thành overlap và truyền thêm `endTime`:
    ```java
    "and b.startTime < :endTime and b.endTime > :startTime " +
    "and b.status not in (com.gym.management.entity.BookingStatus.CANCELLED, com.gym.management.entity.BookingStatus.PT_NO_SHOW, com.gym.management.entity.BookingStatus.LATE_CANCELLED) "
    ```
  - `countActiveInWeek` cũng loại `PT_NO_SHOW` (lỗi của PT, không tính vào giới hạn buổi/tuần của học viên).
- **`createBooking`/`updateBooking`:** thêm check gói còn buổi (G7), cần mở rộng `MembershipLookupPort`.
- Kiểm tra `CurrentUserRole.get()` trả `"RECEPTIONIST"` chứ không phải `"ROLE_RECEPTIONIST"`, nếu sai thì đặt hộ âm thầm thành tự đặt.
- Còn nợ: Swagger annotation trên `BookingController`.

### Agent 4 (Face Attendance)
- Luồng self và kiosk khi người quét là `TRAINER`: gọi `bookingService.recordTrainerFaceScan(...)` thay vì ghi `staff_attendances` (G3).
- Response trả thêm `earlyLeave` và `reasonRequired` để FE hiện ô nhập lý do.
- Trainer không có buổi phù hợp → trả thông báo "không có buổi nào cần điểm danh", không phải lỗi.
- Sửa lại các test `StaffAttendanceServiceImplTest` có dính role TRAINER.
- Lưu ý PT dạy hai buổi liền nhau: quét ở phút 60 là check-out buổi trước, muốn check-in buổi sau thì quét lần nữa. Chống quét đúp qua Redis phải đủ ngắn để không chặn lượt này.

### Agent 5 (POS & Commission)
- **Bỏ hoa hồng Sales.** `commissions` chỉ còn PT, nguồn là booking. Vì V5 chưa viết nên bỏ luôn `type`, `source_type`, `source_id`, thay bằng `booking_id` FK UNIQUE.
- **Chưa có chỗ lưu mức lương mỗi buổi** (`TASK_ASSIGNMENT.md` ghi `commission_rate` trên hồ sơ PT nhưng schema không có cột này). Thêm vào V5, không đụng file SHARED:
  ```sql
  CREATE TABLE trainer_pay_rates (
      id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
      trainer_id UNIQUEIDENTIFIER NOT NULL UNIQUE,
      amount_per_session DECIMAL(12,2) NOT NULL,
      updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
      CONSTRAINT fk_pay_rate_trainer FOREIGN KEY (trainer_id) REFERENCES users(id)
  );
  ```
- Lắng nghe `BookingSettledEvent`: trả công khi `outcome` là `COMPLETED`, `MEMBER_NO_SHOW` hoặc `LATE_CANCEL`; riêng `fullDuration == false` thì bỏ qua (có log). Dùng `commissions.booking_id` là FK thật và **UNIQUE** (thay `source_type`/`source_id`) để chống tính hai lần; thêm cột `outcome`.
- `GET /commissions/me` chỉ còn role `TRAINER`.

### Agent 6 (Leads)
- Chuyển đổi Lead → User **không còn** kích hoạt hoa hồng.
- `GET /reports/commissions-summary` thành "tổng lương theo buổi của PT".

### Agent 2 (Membership)
- Thêm listener (`BookingSettledEvent` là record, trừ buổi cho cả 3 outcome):
  ```java
  @Component
  @RequiredArgsConstructor
  public class BookingSettledMembershipListener {
      private final MembershipService membershipService;

      @EventListener
      public void onBookingSettled(BookingSettledEvent event) {
          membershipService.consumeSession(event.memberId());
      }
  }
  ```
  Đặt trong `service/listener/`. `consumeSession` đã có sẵn nhưng chưa ai gọi.
- Hạ cấp: `startDate = current.getEndDate().plusDays(1)` thay vì `getEndDate()`. Hiện vào đúng ngày `endDate` cả hai gói cùng `ACTIVE`.

## 4. Agent 1 sẽ cập nhật (file SHARED)
`ErrorCode` (thêm `BOOKING_PT_NOT_CHECKED_IN`), `REQUIREMENTS.md` mục 1, 3, 6, `DATABASE_SCHEMA.md` (bookings, commissions, trainer_pay_rates), `API_DESIGN.md` (endpoint mới + 2 điểm Agent 2 yêu cầu), `TASK_ASSIGNMENT.md` (nhiệm vụ Agent 3-6).
