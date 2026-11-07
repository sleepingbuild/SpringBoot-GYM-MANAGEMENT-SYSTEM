package com.gym.management.service;

import com.gym.management.entity.Commission;
import com.gym.management.entity.TrainerPayRate;
import com.gym.management.entity.User;
import com.gym.management.repository.CommissionRepository;
import com.gym.management.repository.PosOrderRepository;
import com.gym.management.repository.ProductRepository;
import com.gym.management.repository.TrainerPayRateRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.event.BookingSettledEvent;
import com.gym.management.service.event.BookingSettledEvent.Outcome;
import com.gym.management.service.impl.CommissionServiceImpl;
import com.gym.management.service.impl.PosOrderServiceImpl;
import com.gym.management.service.impl.ProductServiceImpl;
import com.gym.management.service.impl.TrainerPayRateServiceImpl;
import com.gym.management.service.listener.BookingSettledCommissionListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test Mockito thuần không dựng Spring nên không thấy lỗi nối bean (vòng phụ thuộc, thiếu bean, listener
 * không được đăng ký). Test này dựng context thật chỉ với các bean của Agent 5 (repository và PaymentService
 * là mock, không cần DB), cấm vòng phụ thuộc như Spring Boot, rồi phát một BookingSettledEvent để chắc
 * listener thực sự đến được tầng ghi lương.
 *
 * Lưu ý phạm vi: context này không bật quản lý transaction nên không kiểm chứng được AFTER_COMMIT hay
 * REQUIRES_NEW; hai điều đó chỉ kiểm chứng được khi thử với luồng chốt buổi thật của Agent 3.
 */
class PosCommissionWiringTest {

    private AnnotationConfigApplicationContext ctx;
    private CommissionRepository commissionRepository;
    private TrainerPayRateRepository trainerPayRateRepository;
    private UserRepository userRepository;

    private AnnotationConfigApplicationContext newContext() {
        commissionRepository = mock(CommissionRepository.class);
        trainerPayRateRepository = mock(TrainerPayRateRepository.class);
        userRepository = mock(UserRepository.class);
        AnnotationConfigApplicationContext c = new AnnotationConfigApplicationContext();
        c.setAllowCircularReferences(false); // mặc định của Spring Boot
        c.registerBean(CommissionRepository.class, () -> commissionRepository);
        c.registerBean(TrainerPayRateRepository.class, () -> trainerPayRateRepository);
        c.registerBean(UserRepository.class, () -> userRepository);
        c.registerBean(ProductRepository.class, () -> mock(ProductRepository.class));
        c.registerBean(PosOrderRepository.class, () -> mock(PosOrderRepository.class));
        c.registerBean(PaymentService.class, () -> mock(PaymentService.class));
        c.register(ProductServiceImpl.class, PosOrderServiceImpl.class, TrainerPayRateServiceImpl.class,
                CommissionServiceImpl.class, BookingSettledCommissionListener.class);
        return c;
    }

    @AfterEach
    void tearDown() {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    @DisplayName("Context của Agent 5 khởi động được khi cấm vòng phụ thuộc")
    void contextStartsAndExposesTheServices() {
        ctx = newContext();

        assertThatCode(ctx::refresh).doesNotThrowAnyException();

        assertThat(ctx.getBean(ProductService.class)).isNotNull();
        assertThat(ctx.getBean(PosOrderService.class)).isNotNull();
        assertThat(ctx.getBean(TrainerPayRateService.class)).isNotNull();
        assertThat(ctx.getBean(CommissionService.class)).isNotNull();
    }

    @Test
    @DisplayName("BookingSettledEvent đến được listener và ghi một dòng lương theo mức của PT")
    void bookingSettledEvent_reachesCommissionWrite() {
        ctx = newContext();
        ctx.refresh();
        UUID trainerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        User trainer = new User();
        TrainerPayRate rate = new TrainerPayRate();
        rate.setTrainer(trainer);
        rate.setAmountPerSession(new BigDecimal("150000"));
        when(commissionRepository.existsByBookingId(bookingId)).thenReturn(false);
        when(trainerPayRateRepository.findByTrainerId(trainerId)).thenReturn(Optional.of(rate));
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));

        ctx.publishEvent(new BookingSettledEvent(bookingId, trainerId, UUID.randomUUID(), Outcome.COMPLETED, true, LocalDateTime.now()));

        verify(commissionRepository).save(any(Commission.class));
    }

    @Test
    @DisplayName("Event fullDuration=false không tạo dòng lương")
    void bookingSettledEvent_notFullDuration_isIgnored() {
        ctx = newContext();
        ctx.refresh();

        ctx.publishEvent(new BookingSettledEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Outcome.COMPLETED, false, LocalDateTime.now()));

        verify(commissionRepository, never()).save(any());
    }
}
