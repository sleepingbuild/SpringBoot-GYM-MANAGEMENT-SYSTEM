package com.gym.management.service.impl;

import com.gym.management.dto.request.BookingRequest;
import com.gym.management.dto.request.BookingUpdateRequest;
import com.gym.management.dto.response.BookingResponse;
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
import com.gym.management.service.event.BookingCompletedEvent;
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
 * ⚠️ Đọc kỹ AGENT3_README.md trước khi merge — đặc biệt mục 2 (thứ tự ưu tiên lazy auto-status,
 * không được REQUIREMENTS.md/ISSUES.md nói rõ, đây là quyết định của Agent 3 cần PO xác nhận)
 * và mục 3 (cách Agent 4/5 gọi vào module này).
 *
 * Vai trò "RECEPTIONIST"/"SUPER_ADMIN" dùng để nhận biết luồng "đặt hộ" — tên role giả định
 * khớp bảng roles trong REQUIREMENTS.md mục 1 (SUPER_ADMIN/RECEPTIONIST/SALES/TRAINER/MEMBER).
 */
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final List<String> STAFF_ROLES_CAN_BOOK_FOR_OTHERS = List.of("RECEPTIONIST", "SUPER_ADMIN");

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

    @Value("${gms.booking.pt-no-show-grace-minutes:30}")
    private int ptNoShowGraceMinutes;

    // ------------------------------------------------------------------
    // Tạo mới
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

    @Override
    @Transactional
    public BookingResponse completeBooking(UUID trainerId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndTrainerId(bookingId, trainerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy booking"));
        applyLazyStatus(booking);

        // Chặn CẢ Ở SERVER — nút chỉ hiện ở FE khi check_in_time có giá trị, nhưng đây là chốt
        // chặn thật, đề phòng gọi thẳng API (REQUIREMENTS.md mục 3).
        if (booking.getCheckInTime() == null) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_CHECKED_IN,
                    "Hội viên chưa điểm danh khuôn mặt, không thể đánh dấu hoàn thành");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingCompletedEvent(
                saved.getId(), saved.getTrainer().getId(), saved.getMember().getId(), LocalDateTime.now(clock)));

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

        LocalDateTime bookingStart = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        if (LocalDateTime.now(clock).isAfter(bookingStart)) {
            throw new BusinessException(ErrorCode.BOOKING_PAST_CANNOT_CANCEL, "Không thể huỷ buổi đã diễn ra");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getById(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy booking"));
        applyLazyStatus(booking);
        return toResponse(booking);
    }

    @Override
    @Transactional
    public List<BookingResponse> findMyBookings(UUID currentUserId, String currentUserRole) {
        List<Booking> bookings = "TRAINER".equals(currentUserRole)
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

        // Lần đầu trong ngày = check-in
        List<Booking> pendingCheckIn = bookingRepository.findTodayPendingCheckIn(memberId, today);
        if (!pendingCheckIn.isEmpty()) {
            Booking booking = pendingCheckIn.get(0); // gần giờ nhất, đã sort theo start_time asc
            booking.setCheckInTime(scanTime);
            booking.setCheckInMethod("FACE");
            return Optional.of(toResponse(bookingRepository.save(booking)));
        }

        // Lượt tiếp theo = check-out
        List<Booking> pendingCheckOut = bookingRepository.findTodayPendingCheckOut(memberId, today);
        if (!pendingCheckOut.isEmpty()) {
            Booking booking = pendingCheckOut.get(0); // check-in gần nhất trước đó
            booking.setCheckOutTime(scanTime);
            booking.setCheckOutMethod("FACE");
            return Optional.of(toResponse(bookingRepository.save(booking)));
        }

        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // Validate rules (Issue 3.2)
    // ------------------------------------------------------------------

    private void validateBookingRules(Booking booking, UUID excludeBookingId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime bookingStart = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());

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

        // 3. Tối đa 1 người/khung giờ/PT
        List<Booking> slotTaken = bookingRepository.findActiveByTrainerSlot(
                booking.getTrainer().getId(), booking.getBookingDate(), booking.getStartTime(), excludeBookingId);
        if (!slotTaken.isEmpty()) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_TAKEN, "Khung giờ này của PT đã có người đặt");
        }

        // 4. Chặn member đặt trùng giờ với 2 PT khác nhau cùng lúc
        List<Booking> memberClash = bookingRepository.findActiveByMemberSlot(
                booking.getMember().getId(), booking.getBookingDate(), booking.getStartTime(), excludeBookingId);
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
                    booking.getMember().getId(), weekStart, weekEnd, excludeBookingId);
            if (countInWeek >= maxPerWeek) {
                throw new BusinessException(ErrorCode.BOOKING_WEEKLY_LIMIT_EXCEEDED,
                        "Đã đạt giới hạn " + maxPerWeek + " buổi/tuần theo gói tập");
            }
        }
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
    // Lazy auto-status (Issue 3.4)
    // ------------------------------------------------------------------

    /**
     * Áp lại trạng thái theo thời gian MỖI KHI đọc booking — không cần @Scheduled/cron
     * (REQUIREMENTS.md mục 3 + mục 7). Idempotent: gọi lại nhiều lần trên booking đã ở
     * trạng thái cuối (COMPLETED/CANCELLED/NO_SHOW/PT_NO_SHOW) không có tác dụng gì thêm.
     *
     * ⚠️ Thứ tự kiểm tra dưới đây theo ĐÚNG thứ tự liệt kê trong ISSUES.md Issue 3.4 / SCH
     * REQUIREMENTS.md mục 3 — luật nào khớp trước thì dừng lại, không có văn bản nào nói rõ
     * độ ưu tiên khi 2 điều kiện cùng đúng, đây là quyết định của Agent 3 (xem AGENT3_README.md
     * mục 2) — CẦN PRODUCT OWNER XÁC NHẬN LẠI.
     */
    private void applyLazyStatus(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            return; // đã ở trạng thái cuối, không xử lý lại
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime bookingStart = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        LocalDateTime bookingEnd = LocalDateTime.of(booking.getBookingDate(), booking.getEndTime());

        boolean changed = false;

        if (booking.getStatus() == BookingStatus.PENDING
                && !now.isBefore(bookingStart.minusMinutes(pendingCancelThresholdMinutes))) {
            booking.setStatus(BookingStatus.CANCELLED);
            changed = true;
        } else if (booking.getStatus() == BookingStatus.CONFIRMED
                && booking.getCheckInTime() == null
                && now.isAfter(bookingEnd)) {
            // Quá giờ kết thúc mà member chưa face check-in -> NO_SHOW (ưu tiên hơn PT_NO_SHOW
            // vì đây là lỗi của member, không phải của PT).
            booking.setStatus(BookingStatus.NO_SHOW);
            changed = true;
        } else if (booking.getStatus() == BookingStatus.CONFIRMED
                && now.isAfter(bookingStart.plusMinutes(ptNoShowGraceMinutes))) {
            // Member đã check-in (hoặc chưa tới giờ kết thúc) nhưng PT chưa đánh dấu hoàn thành
            // quá 30 phút kể từ giờ hẹn -> PT_NO_SHOW.
            booking.setStatus(BookingStatus.PT_NO_SHOW);
            changed = true;
        }

        if (changed) {
            bookingRepository.save(booking);
        }
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
                .notes(b.getNotes())
                .build();
    }
}
