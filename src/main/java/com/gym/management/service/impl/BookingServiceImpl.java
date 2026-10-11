package com.gym.management.service.impl;

import com.gym.management.dto.request.BookingRequest;
import com.gym.management.dto.request.BookingUpdateRequest;
import com.gym.management.dto.response.BookingResponse;
import com.gym.management.dto.response.TrainerScanResult;
import com.gym.management.entity.Booking;
import com.gym.management.entity.BookingStatus;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.BookingRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.BookingService;
import com.gym.management.service.MembershipLookupPort;
import com.gym.management.service.TrainerScheduleService;
import com.gym.management.service.event.BookingSettledEvent;
import com.gym.management.service.event.BookingSettledEvent.Outcome;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Luật điểm danh PT / no-show / huỷ muộn: xem CHANGE_PT_ATTENDANCE_AND_PAY.md (Agent 1, theo
 * quyết định của Phi). Các quyết định riêng của Agent 3 nằm ở AGENT3_README.md.
 *
 * Vai trò "RECEPTIONIST"/"SUPER_ADMIN"/"TRAINER" là tên role KHÔNG có tiền tố ROLE_
 * (CurrentUserRole.get() trả về dạng đó — Agent 1 đã xác nhận).
 */
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final List<String> STAFF_ROLES_CAN_BOOK_FOR_OTHERS = List.of("RECEPTIONIST", "SUPER_ADMIN");
    private static final String ROLE_TRAINER = "TRAINER";
    private static final String METHOD_FACE = "FACE";
    /** PT được check-in sớm tối đa bao nhiêu phút trước giờ bắt đầu (G6: [S-30, S+ân hạn]). */
    private static final int TRAINER_CHECKIN_EARLY_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TrainerScheduleService trainerScheduleService;
    private final MembershipLookupPort membershipLookupPort;
    private final ApplicationEventPublisher eventPublisher;
    /** Tiêm qua bean BookingClockConfig — cho phép unit test giả lập thời gian bằng Clock.fixed(...). */
    private final Clock clock;

    @Value("${gms.booking.min-lead-minutes:30}")
    private int minLeadMinutes;

    @Value("${gms.booking.pending-cancel-threshold-minutes:60}")
    private int pendingCancelThresholdMinutes;

    /** Ân hạn sau giờ bắt đầu trước khi xét PT_NO_SHOW / NO_SHOW (thay pt-no-show-grace-minutes: 30). */
    @Value("${gms.booking.no-show-grace-minutes:15}")
    private int noShowGraceMinutes;

    /** Học viên huỷ booking CONFIRMED khi còn DƯỚI số giờ này thì bị LATE_CANCELLED. */
    @Value("${gms.booking.late-cancel-hours:2}")
    private int lateCancelHours;

    // ------------------------------------------------------------------
    // Tạo mới / sửa
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public BookingResponse createBooking(UUID currentUserId, String currentUserRole, BookingRequest request) {
        UUID memberId = resolveMemberId(currentUserId, currentUserRole, request.getMemberId());

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy hội viên"));
        User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy PT"));

        // BƯỚC 1: gán ĐỦ các trường bắt buộc (đặc biệt FK) TRƯỚC khi validate.
        // Đây là bug nghiêm trọng đã xảy ra bên .NET (REQUIREMENTS.md mục 3) — không đảo thứ tự.
        Booking booking = Booking.builder()
                .member(member)
                .trainer(trainer)
                .branchId(request.getBranchId())
                .bookingDate(request.getBookingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.PENDING)
                .notes(request.getNotes())
                .build();

        // BƯỚC 2: validate sau khi đã gán đủ.
        validateBookingRules(booking, null);

        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(UUID currentUserId, String currentUserRole, UUID bookingId, BookingUpdateRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy booking"));
        applyLazyStatus(booking);

        // Member chỉ sửa được booking của chính mình; lễ tân/admin sửa hộ được.
        boolean isOwner = booking.getMember().getId().equals(currentUserId);
        if (!isOwner && !STAFF_ROLES_CAN_BOOK_FOR_OTHERS.contains(currentUserRole)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "Không có quyền sửa booking này");
        }
        // Booking đã chốt (huỷ, no-show, hoàn thành...) không sửa được nữa — nếu sửa thì sự kiện
        // đã phát (trừ buổi, trả công) không còn khớp với lịch.
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Chỉ sửa được booking đang PENDING/CONFIRMED (hiện tại: " + booking.getStatus() + ")");
        }

        User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy PT"));

        // Gán đủ trước khi validate, kể cả khi sửa lịch (Issue 3.2 acceptance criteria).
        booking.setTrainer(trainer);
        booking.setBranchId(request.getBranchId());
        booking.setBookingDate(request.getBookingDate());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setNotes(request.getNotes());

        validateBookingRules(booking, bookingId);

        return toResponse(bookingRepository.save(booking));
    }

    // ------------------------------------------------------------------
    // Xác nhận / Hoàn thành / Huỷ
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public BookingResponse confirmBooking(UUID trainerId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndTrainerId(bookingId, trainerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy booking"));
        applyLazyStatus(booking);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Chỉ xác nhận được booking đang ở trạng thái PENDING (hiện tại: " + booking.getStatus() + ")");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    /**
     * ⚠️ BẢN TẠM — còn thiếu chốt chặn "PT phải quét mặt vào VÀ ra" (G4), vì cần mã lỗi
     * BOOKING_PT_NOT_CHECKED_IN, hiện nằm ở nhánh shared/error-code-booking-pt chưa merge master.
     * Khi mã lỗi có trên master sẽ thêm đúng chỗ đánh dấu TODO bên dưới. KHÔNG merge PR khi
     * TODO này còn đó.
     */
    @Override
    @Transactional
    public BookingResponse completeBooking(UUID trainerId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndTrainerId(bookingId, trainerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy booking"));
        applyLazyStatus(booking);

        // Trước đây hàm chỉ kiểm tra checkInTime nên booking đã CANCELLED mà có checkInTime vẫn bị
        // ghi đè thành COMPLETED. Chỉ booking đang CONFIRMED mới hoàn thành được.
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Chỉ hoàn thành được booking đang CONFIRMED (hiện tại: " + booking.getStatus() + ")");
        }
        if (booking.getCheckInTime() == null) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_CHECKED_IN,
                    "Hội viên chưa điểm danh khuôn mặt, không thể đánh dấu hoàn thành");
        }
        // TODO(BOOKING_PT_NOT_CHECKED_IN): nếu ptCheckInTime == null || ptCheckOutTime == null
        //   -> throw BusinessException(ErrorCode.BOOKING_PT_NOT_CHECKED_IN, "PT phải quét mặt vào và ra trước khi hoàn thành")

        boolean fullDuration = isFullDuration(booking);
        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);

        publishSettled(saved, Outcome.COMPLETED, fullDuration);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID currentUserId, String currentUserRole, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy booking"));
        applyLazyStatus(booking);

        boolean isOwner = booking.getMember().getId().equals(currentUserId)
                || booking.getTrainer().getId().equals(currentUserId);
        if (!isOwner && !STAFF_ROLES_CAN_BOOK_FOR_OTHERS.contains(currentUserRole)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "Không có quyền huỷ booking này");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime bookingStart = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        if (now.isAfter(bookingStart)) {
            throw new BusinessException(ErrorCode.BOOKING_PAST_CANNOT_CANCEL, "Không thể huỷ buổi đã diễn ra");
        }
        // Chỉ huỷ được booking còn sống. Nếu không chặn, huỷ lại booking đã LATE_CANCELLED sẽ phát
        // sự kiện lần 2 và học viên bị trừ buổi 2 lần.
        BookingStatus status = booking.getStatus();
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Chỉ huỷ được booking đang PENDING/CONFIRMED (hiện tại: " + status + ")");
        }

        // Huỷ muộn chỉ áp cho phía học viên (chính học viên, hoặc lễ tân/admin huỷ hộ) và chỉ khi
        // booking đã CONFIRMED. PT huỷ -> CANCELLED (không lương, không trừ buổi). Huỷ PENDING miễn phí.
        boolean cancelledByTrainer = ROLE_TRAINER.equals(currentUserRole);
        boolean lateCancel = !cancelledByTrainer
                && status == BookingStatus.CONFIRMED
                && now.isAfter(bookingStart.minusHours(lateCancelHours));

        if (lateCancel) {
            booking.setStatus(BookingStatus.LATE_CANCELLED);
            Booking saved = bookingRepository.save(booking);
            publishSettled(saved, Outcome.LATE_CANCEL, true);
            return toResponse(saved);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional // KHÔNG readOnly: applyLazyStatus có thể ghi DB và phát sự kiện
    public BookingResponse getById(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy booking"));
        applyLazyStatus(booking);
        return toResponse(booking);
    }

    @Override
    @Transactional
    public List<BookingResponse> findMyBookings(UUID currentUserId, String currentUserRole) {
        List<Booking> bookings = ROLE_TRAINER.equals(currentUserRole)
                ? bookingRepository.findByTrainerIdOrderByBookingDateDescStartTimeDesc(currentUserId)
                : bookingRepository.findByMemberIdOrderByBookingDateDescStartTimeDesc(currentUserId);
        bookings.forEach(this::applyLazyStatus);
        return bookings.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public List<BookingResponse> findByBranchAndDate(UUID branchId, LocalDate date) {
        List<Booking> bookings = bookingRepository.findByBranchIdAndBookingDate(branchId, date);
        bookings.forEach(this::applyLazyStatus);
        return bookings.stream().map(this::toResponse).toList();
    }

    // ------------------------------------------------------------------
    // Face attendance (gọi bởi Agent 4)
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public Optional<BookingResponse> recordFaceScan(UUID memberId, LocalDateTime scanTime) {
        LocalDate today = scanTime.toLocalDate();

        // Lần đầu = check-in. Áp lazy-status trước để không check-in vào buổi vừa bị lật sang
        // PT_NO_SHOW/NO_SHOW; chỉ booking CONFIRMED mới check-in được (G5).
        List<Booking> pendingCheckIn = bookingRepository.findTodayPendingCheckIn(memberId, today);
        pendingCheckIn.forEach(this::applyLazyStatus);
        Optional<Booking> toCheckIn = pendingCheckIn.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .findFirst(); // đã sort theo start_time asc
        if (toCheckIn.isPresent()) {
            Booking booking = toCheckIn.get();
            booking.setCheckInTime(scanTime);
            booking.setCheckInMethod(METHOD_FACE);
            return Optional.of(toResponse(bookingRepository.save(booking)));
        }

        // Lượt tiếp theo = check-out (buổi đã check-in mà chưa check-out, còn hiệu lực)
        List<Booking> pendingCheckOut = bookingRepository.findTodayPendingCheckOut(memberId, today);
        pendingCheckOut.forEach(this::applyLazyStatus);
        Optional<Booking> toCheckOut = pendingCheckOut.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                .findFirst(); // check-in gần nhất trước đó
        if (toCheckOut.isPresent()) {
            Booking booking = toCheckOut.get();
            booking.setCheckOutTime(scanTime);
            booking.setCheckOutMethod(METHOD_FACE);
            return Optional.of(toResponse(bookingRepository.save(booking)));
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public Optional<TrainerScanResult> recordTrainerFaceScan(UUID trainerId, LocalDateTime scanTime) {
        List<Booking> candidates = bookingRepository.findTodayConfirmedByTrainer(trainerId, scanTime.toLocalDate());
        // Áp lazy-status trước: buổi vừa quá ân hạn sẽ lật sang PT_NO_SHOW/NO_SHOW và bị loại.
        candidates.forEach(this::applyLazyStatus);
        List<Booking> live = candidates.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .toList(); // vẫn sort theo start_time asc

        // (1) Có buổi PT đã quét vào mà chưa quét ra -> CHECK_OUT (ghi ngay, lý do nộp sau).
        Optional<Booking> toCheckOut = live.stream()
                .filter(b -> b.getPtCheckInTime() != null && b.getPtCheckOutTime() == null)
                .findFirst();
        if (toCheckOut.isPresent()) {
            Booking booking = toCheckOut.get();
            booking.setPtCheckOutTime(scanTime);
            booking.setPtCheckOutMethod(METHOD_FACE);
            Booking saved = bookingRepository.save(booking);
            boolean earlyLeave = scanTime.isBefore(LocalDateTime.of(saved.getBookingDate(), saved.getEndTime()));
            return Optional.of(new TrainerScanResult(toResponse(saved), TrainerScanResult.CHECK_OUT, earlyLeave));
        }

        // (2) Buổi chưa quét vào, giờ quét nằm trong [S - 30 phút, S + ân hạn] -> CHECK_IN (G6).
        Optional<Booking> toCheckIn = live.stream()
                .filter(b -> b.getPtCheckInTime() == null)
                .filter(b -> {
                    LocalDateTime start = LocalDateTime.of(b.getBookingDate(), b.getStartTime());
                    return !scanTime.isBefore(start.minusMinutes(TRAINER_CHECKIN_EARLY_MINUTES))
                            && !scanTime.isAfter(start.plusMinutes(noShowGraceMinutes));
                })
                .findFirst();
        if (toCheckIn.isPresent()) {
            Booking booking = toCheckIn.get();
            booking.setPtCheckInTime(scanTime);
            booking.setPtCheckInMethod(METHOD_FACE);
            Booking saved = bookingRepository.save(booking);
            return Optional.of(new TrainerScanResult(toResponse(saved), TrainerScanResult.CHECK_IN, false));
        }

        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // Validate rules (Issue 3.2)
    // ------------------------------------------------------------------

    private void validateBookingRules(Booking booking, UUID excludeBookingId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime bookingStart = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        UUID exclude = excludeBookingId != null ? excludeBookingId : BookingRepository.NO_EXCLUDE;

        if (!booking.getEndTime().isAfter(booking.getStartTime())) {
            throw new BusinessException(ErrorCode.SCHEDULE_TIME_INVALID, "Giờ kết thúc phải sau giờ bắt đầu");
        }

        // 1. Chặn giờ quá khứ + quá gần hiện tại (<30 phút)
        if (bookingStart.isBefore(now.plusMinutes(minLeadMinutes))) {
            throw new BusinessException(ErrorCode.BOOKING_TOO_SOON,
                    "Không thể đặt lịch cách hiện tại dưới " + minLeadMinutes + " phút, hoặc ở thời điểm đã qua");
        }

        // 2. Không đặt được ngoài ca làm việc thật của PT
        boolean withinWorkingHours = trainerScheduleService.isWithinWorkingHours(
                booking.getTrainer().getId(), booking.getBookingDate(), booking.getStartTime(), booking.getEndTime());
        if (!withinWorkingHours) {
            throw new BusinessException(ErrorCode.BOOKING_OUT_OF_WORKING_HOURS, "Ngoài ca làm việc thật của PT");
        }

        // 3. Tối đa 1 người/khung giờ/PT — chồng khoảng giờ là trùng (10:00-11:00 vs 10:30-11:30)
        List<Booking> slotTaken = bookingRepository.findActiveByTrainerSlot(
                booking.getTrainer().getId(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), exclude);
        if (!slotTaken.isEmpty()) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_TAKEN, "Khung giờ này của PT đã có người đặt");
        }

        // 4. Chặn member đặt trùng giờ với 2 PT khác nhau cùng lúc (cũng theo khoảng giờ chồng)
        List<Booking> memberClash = bookingRepository.findActiveByMemberSlot(
                booking.getMember().getId(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), exclude);
        if (!memberClash.isEmpty()) {
            throw new BusinessException(ErrorCode.BOOKING_DOUBLE_BOOKED,
                    "Hội viên đã có lịch khác trùng giờ này với 1 PT khác");
        }

        // 5. Giới hạn buổi/tuần theo gói (Thứ 2 -> Chủ nhật)
        Integer maxPerWeek = membershipLookupPort.resolveMaxSessionsPerWeek(booking.getMember().getId());
        if (maxPerWeek != null) {
            if (maxPerWeek == 0) {
                throw new BusinessException(ErrorCode.BOOKING_WEEKLY_LIMIT_EXCEEDED,
                        "Gói tập hiện tại không cho phép đặt lịch với PT");
            }
            LocalDate weekStart = booking.getBookingDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);
            long countInWeek = bookingRepository.countActiveInWeek(
                    booking.getMember().getId(), weekStart, weekEnd, exclude);
            if (countInWeek >= maxPerWeek) {
                throw new BusinessException(ErrorCode.BOOKING_WEEKLY_LIMIT_EXCEEDED,
                        "Đã đạt giới hạn " + maxPerWeek + " buổi/tuần theo gói tập");
            }
        }

        // TODO(G7, chờ xác nhận): chặn đặt khi gói hết buổi (remaining_sessions) — xem AGENT3_README.md.
    }

    private UUID resolveMemberId(UUID currentUserId, String currentUserRole, UUID requestedMemberId) {
        boolean canBookForOthers = STAFF_ROLES_CAN_BOOK_FOR_OTHERS.contains(currentUserRole);
        if (canBookForOthers) {
            if (requestedMemberId == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thiếu memberId khi đặt lịch hộ");
            }
            return requestedMemberId;
        }
        return currentUserId; // member tự đặt — luôn dùng chính currentUserId, bỏ qua memberId gửi lên nếu có
    }

    // ------------------------------------------------------------------
    // Lazy auto-status (Issue 3.4) — luật mới: ân hạn tính từ giờ BẮT ĐẦU
    // ------------------------------------------------------------------

    /**
     * Áp lại trạng thái theo thời gian MỖI KHI đọc booking — không cần @Scheduled/cron.
     * Idempotent: booking đã ở trạng thái cuối thì không xử lý lại.
     *
     * <ul>
     *   <li>PENDING mà còn ≤ ngưỡng (mặc định 60 phút) trước giờ bắt đầu -> CANCELLED.</li>
     *   <li>CONFIRMED, quá S + ân hạn (mặc định 15 phút):
     *     PT chưa quét vào -> PT_NO_SHOW (ưu tiên PT, kể cả khi học viên cũng vắng);
     *     PT đã quét vào mà học viên chưa -> NO_SHOW + phát MEMBER_NO_SHOW;
     *     cả hai đã quét vào -> giữ CONFIRMED, chờ PT bấm hoàn thành.</li>
     * </ul>
     * Mốc "quá S + ân hạn" là so sánh nghiêm ngặt: đúng S+15 phút thì chưa lật.
     */
    private void applyLazyStatus(Booking booking) {
        BookingStatus status = booking.getStatus();
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
            return; // đã ở trạng thái cuối, không xử lý lại
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());

        if (status == BookingStatus.PENDING) {
            if (!now.isBefore(start.minusMinutes(pendingCancelThresholdMinutes))) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
            }
            return;
        }

        // CONFIRMED
        if (!now.isAfter(start.plusMinutes(noShowGraceMinutes))) {
            return; // còn trong thời gian ân hạn
        }
        if (booking.getPtCheckInTime() == null) {
            booking.setStatus(BookingStatus.PT_NO_SHOW); // lỗi PT: không trừ buổi, không trả công
            bookingRepository.save(booking);
        } else if (booking.getCheckInTime() == null) {
            booking.setStatus(BookingStatus.NO_SHOW);    // lỗi học viên: mất buổi, PT vẫn được trả
            Booking saved = bookingRepository.save(booking);
            publishSettled(saved, Outcome.MEMBER_NO_SHOW, true);
        }
        // cả hai đã quét vào: giữ nguyên CONFIRMED
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private void publishSettled(Booking booking, Outcome outcome, boolean fullDuration) {
        eventPublisher.publishEvent(new BookingSettledEvent(
                booking.getId(), booking.getTrainer().getId(), booking.getMember().getId(),
                outcome, fullDuration, LocalDateTime.now(clock)));
    }

    /**
     * PT dạy đủ thời gian khi quét ra từ giờ kết thúc trở đi, hoặc quét ra sớm nhưng có nộp lý do.
     * Chưa quét ra thì trả false (chỗ gọi quyết định có chặn hay không).
     */
    private boolean isFullDuration(Booking b) {
        if (b.getPtCheckOutTime() == null) {
            return false;
        }
        LocalDateTime end = LocalDateTime.of(b.getBookingDate(), b.getEndTime());
        boolean hasReason = b.getPtEarlyLeaveReason() != null && !b.getPtEarlyLeaveReason().isBlank();
        return !b.getPtCheckOutTime().isBefore(end) || hasReason;
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .memberId(b.getMember().getId())
                .memberName(b.getMember().getFullName())
                .trainerId(b.getTrainer().getId())
                .trainerName(b.getTrainer().getFullName())
                .branchId(b.getBranchId())
                .bookingDate(b.getBookingDate())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .status(b.getStatus())
                .checkInTime(b.getCheckInTime())
                .checkInMethod(b.getCheckInMethod())
                .checkOutTime(b.getCheckOutTime())
                .checkOutMethod(b.getCheckOutMethod())
                .ptCheckInTime(b.getPtCheckInTime())
                .ptCheckInMethod(b.getPtCheckInMethod())
                .ptCheckOutTime(b.getPtCheckOutTime())
                .ptCheckOutMethod(b.getPtCheckOutMethod())
                .ptEarlyLeaveReason(b.getPtEarlyLeaveReason())
                .fullDuration(b.getPtCheckOutTime() == null ? null : isFullDuration(b))
                .notes(b.getNotes())
                .build();
    }
}
