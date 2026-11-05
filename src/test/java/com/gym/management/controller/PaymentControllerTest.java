package com.gym.management.controller;

import com.gym.management.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * H3 (frontend đợt 1): GET /payments có thêm bộ lọc status và userId. Chốt hai điều: controller chuyển đúng
 * các tham số xuống service, và endpoint tra cứu toàn bộ giao dịch vẫn chỉ dành cho Lễ tân/Super Admin.
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Test
    void getAll_shouldPassStatusAndUserFiltersToService() {
        PaymentController controller = new PaymentController(paymentService);
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        controller.getAll(from, to, "PENDING", userId, 1, 10, "createdAt,desc");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentService).getAllPayments(eq(from), eq(to), eq("PENDING"), eq(userId), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void getAll_shouldRemainStaffOnly() throws NoSuchMethodException {
        Method getAll = PaymentController.class.getMethod("getAll",
                LocalDate.class, LocalDate.class, String.class, UUID.class, int.class, int.class, String.class);

        assertThat(getAll.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')");
    }
}
