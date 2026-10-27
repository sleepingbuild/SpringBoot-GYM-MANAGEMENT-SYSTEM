package com.gym.management.service.impl;

import com.gym.management.dto.response.CommissionResponse;
import com.gym.management.dto.response.CommissionSummaryResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.entity.Commission;
import com.gym.management.entity.CommissionStatus;
import com.gym.management.entity.TrainerPayRate;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.CommissionRepository;
import com.gym.management.repository.TrainerPayRateRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.CommissionService;
import com.gym.management.service.event.BookingSettledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final CommissionRepository commissionRepository;
    private final TrainerPayRateRepository trainerPayRateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void recordSessionPay(UUID trainerId, UUID bookingId, BookingSettledEvent.Outcome outcome) {
        if (commissionRepository.existsByBookingId(bookingId)) {
            // Idempotent theo thiết kế — có thể nhận cùng 1 event (hoặc lazy-check chạy lại)
            // nhiều lần cho cùng 1 booking. Không dùng unique-constraint exception để chống
            // trùng vì nằm trong listener đồng bộ của transaction completeBooking (xem
            // docs/CHANGE_PT_ATTENDANCE_AND_PAY.md mục Agent 5) — exception ở đây sẽ làm
            // hỏng luôn transaction hoàn thành buổi tập của PT.
            log.debug("Booking {} đã có bản ghi lương, bỏ qua", bookingId);
            return;
        }

        Optional<TrainerPayRate> rateOpt = trainerPayRateRepository.findByTrainerId(trainerId);
        if (rateOpt.isEmpty()) {
            log.warn("PT {} chưa được cấu hình lương theo buổi — bỏ qua tính lương cho booking {} (outcome={})",
                    trainerId, bookingId, outcome);
            return;
        }

        User trainer = userRepository.findById(trainerId).orElse(null);
        if (trainer == null) {
            log.warn("Không tìm thấy user trainerId={} khi ghi nhận lương cho booking {}", trainerId, bookingId);
            return;
        }

        Commission commission = new Commission();
        commission.setStaff(trainer);
        commission.setBookingId(bookingId);
        commission.setOutcome(outcome);
        commission.setAmount(rateOpt.get().getAmountPerSession());
        commission.setStatus(CommissionStatus.PENDING);
        commissionRepository.save(commission);
    }

    @Override
    @Transactional
    public CommissionResponse markPaid(UUID commissionId) {
        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bản ghi lương"));
        if (commission.getStatus() != CommissionStatus.PAID) {
            commission.setStatus(CommissionStatus.PAID);
            commission.setPaidAt(LocalDateTime.now());
            commission = commissionRepository.save(commission);
        }
        return toResponse(commission);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommissionResponse> myCommissions(UUID trainerId, Pageable pageable) {
        return PageResponse.from(commissionRepository.findByStaffId(trainerId, pageable), this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommissionResponse> search(UUID staffId, YearMonth month, Pageable pageable) {
        LocalDateTime[] range = monthRange(month);
        return PageResponse.from(
                commissionRepository.search(staffId, range[0], range[1], pageable), this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionSummaryResponse summarize(YearMonth month) {
        LocalDateTime[] range = monthRange(month);
        List<Commission> commissions = range[0] != null && range[1] != null
                ? commissionRepository.findByCreatedAtBetween(range[0], range[1])
                : commissionRepository.findAll();

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        int pendingCount = 0;
        int paidCount = 0;

        for (Commission c : commissions) {
            totalAmount = totalAmount.add(c.getAmount());
            if (c.getStatus() == CommissionStatus.PAID) {
                paidAmount = paidAmount.add(c.getAmount());
                paidCount++;
            } else {
                pendingAmount = pendingAmount.add(c.getAmount());
                pendingCount++;
            }
        }

        return CommissionSummaryResponse.builder()
                .month(month != null ? month.format(DateTimeFormatter.ofPattern("yyyy-MM")) : null)
                .totalSessions(commissions.size())
                .totalAmount(totalAmount)
                .pendingCount(pendingCount)
                .pendingAmount(pendingAmount)
                .paidCount(paidCount)
                .paidAmount(paidAmount)
                .build();
    }

    /** Trả [from, to) của tháng; {null, null} nếu month == null (không lọc theo tháng). */
    private LocalDateTime[] monthRange(YearMonth month) {
        if (month == null) {
            return new LocalDateTime[]{null, null};
        }
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        return new LocalDateTime[]{from, to};
    }

    private CommissionResponse toResponse(Commission commission) {
        return CommissionResponse.builder()
                .id(commission.getId())
                .staffId(commission.getStaff().getId())
                .bookingId(commission.getBookingId())
                .outcome(commission.getOutcome())
                .amount(commission.getAmount())
                .status(commission.getStatus())
                .paidAt(commission.getPaidAt())
                .createdAt(commission.getCreatedAt())
                .build();
    }
}
