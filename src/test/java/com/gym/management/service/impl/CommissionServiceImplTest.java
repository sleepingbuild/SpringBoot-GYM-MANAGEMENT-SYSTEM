package com.gym.management.service.impl;

import com.gym.management.dto.response.CommissionResponse;
import com.gym.management.dto.response.CommissionSummaryResponse;
import com.gym.management.entity.Commission;
import com.gym.management.entity.CommissionStatus;
import com.gym.management.entity.TrainerPayRate;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.repository.CommissionRepository;
import com.gym.management.repository.TrainerPayRateRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.event.BookingSettledEvent.Outcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommissionServiceImplTest {

    @Mock private CommissionRepository commissionRepository;
    @Mock private TrainerPayRateRepository trainerPayRateRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private CommissionServiceImpl service;

    private final UUID trainerId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private User trainer;

    @BeforeEach
    void setUp() {
        trainer = new User();
        ReflectionTestUtils.setField(trainer, "id", trainerId);
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(commissionRepository.save(any(Commission.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private TrainerPayRate rate(String amount) {
        TrainerPayRate r = new TrainerPayRate();
        r.setTrainer(trainer);
        r.setAmountPerSession(new BigDecimal(amount));
        return r;
    }

    @Test
    void recordSessionPay_createsPendingCommissionWithCurrentRate_forAllThreeOutcomes() {
        when(commissionRepository.existsByBookingId(any())).thenReturn(false);
        when(trainerPayRateRepository.findByTrainerId(trainerId)).thenReturn(Optional.of(rate("150000.00")));

        for (Outcome outcome : Outcome.values()) {
            service.recordSessionPay(trainerId, UUID.randomUUID(), outcome);
        }

        ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Commission::getOutcome)
                .containsExactly(Outcome.COMPLETED, Outcome.MEMBER_NO_SHOW, Outcome.LATE_CANCEL);
        assertThat(captor.getAllValues()).allSatisfy(c -> {
            assertThat(c.getAmount()).isEqualByComparingTo("150000.00");
            assertThat(c.getStatus()).isEqualTo(CommissionStatus.PENDING);
            assertThat(c.getStaff()).isSameAs(trainer);
        });
    }

    @Test
    void recordSessionPay_calledTwiceForSameBooking_createsOnlyOne() {
        when(trainerPayRateRepository.findByTrainerId(trainerId)).thenReturn(Optional.of(rate("150000.00")));
        when(commissionRepository.existsByBookingId(bookingId)).thenReturn(false, true);

        service.recordSessionPay(trainerId, bookingId, Outcome.COMPLETED);
        service.recordSessionPay(trainerId, bookingId, Outcome.COMPLETED);

        verify(commissionRepository, org.mockito.Mockito.times(1)).save(any(Commission.class));
    }

    @Test
    void recordSessionPay_trainerWithoutRate_skipsWithoutThrowing() {
        when(commissionRepository.existsByBookingId(bookingId)).thenReturn(false);
        when(trainerPayRateRepository.findByTrainerId(trainerId)).thenReturn(Optional.empty());

        service.recordSessionPay(trainerId, bookingId, Outcome.COMPLETED); // không ném lỗi

        verify(commissionRepository, never()).save(any());
    }

    @Test
    void markPaid_setsPaidAndTimestamp_andIsIdempotent() {
        Commission c = new Commission();
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(c, "id", id);
        c.setStaff(trainer);
        c.setBookingId(bookingId);
        c.setOutcome(Outcome.COMPLETED);
        c.setAmount(new BigDecimal("150000.00"));
        when(commissionRepository.findById(id)).thenReturn(Optional.of(c));

        CommissionResponse first = service.markPaid(id);
        var paidAt = first.getPaidAt();
        CommissionResponse second = service.markPaid(id);

        assertThat(first.getStatus()).isEqualTo(CommissionStatus.PAID);
        assertThat(paidAt).isNotNull();
        assertThat(second.getPaidAt()).isEqualTo(paidAt); // không ghi đè thời điểm trả lần đầu
        verify(commissionRepository, org.mockito.Mockito.times(1)).save(any(Commission.class));
    }

    @Test
    void markPaid_unknownId_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(commissionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markPaid(id)).isInstanceOf(BusinessException.class);
    }

    @Test
    void summarize_splitsPendingAndPaid() {
        Commission pending = commission("100000.00", CommissionStatus.PENDING);
        Commission paid1 = commission("150000.00", CommissionStatus.PAID);
        Commission paid2 = commission("150000.00", CommissionStatus.PAID);
        when(commissionRepository.findByCreatedAtBetween(any(), any()))
                .thenReturn(List.of(pending, paid1, paid2));

        CommissionSummaryResponse s = service.summarize(YearMonth.of(2026, 10));

        assertThat(s.getMonth()).isEqualTo("2026-10");
        assertThat(s.getTotalSessions()).isEqualTo(3);
        assertThat(s.getTotalAmount()).isEqualByComparingTo("400000.00");
        assertThat(s.getPendingCount()).isEqualTo(1);
        assertThat(s.getPendingAmount()).isEqualByComparingTo("100000.00");
        assertThat(s.getPaidCount()).isEqualTo(2);
        assertThat(s.getPaidAmount()).isEqualByComparingTo("300000.00");
    }

    private Commission commission(String amount, CommissionStatus status) {
        Commission c = new Commission();
        c.setStaff(trainer);
        c.setBookingId(UUID.randomUUID());
        c.setOutcome(Outcome.COMPLETED);
        c.setAmount(new BigDecimal(amount));
        c.setStatus(status);
        return c;
    }
}
