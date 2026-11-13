package com.gym.management.controller;

import com.gym.management.dto.request.TrainerPayRateRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.service.CommissionService;
import com.gym.management.service.PosOrderService;
import com.gym.management.service.ProductService;
import com.gym.management.service.TrainerPayRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Chốt phân quyền và cách controller của Agent 5 truyền tham số xuống service. Logic nghiệp vụ đã có test ở tầng
 * service; ở đây chỉ chặn hai loại lỗi mà test service không thấy: quên @PreAuthorize ở endpoint mới, và
 * controller truyền sai/nhầm tham số (vd lọc tháng sai, quên gói tham số).
 */
@ExtendWith(MockitoExtension.class)
class PosCommissionControllersTest {

    private static final String STAFF = "hasAnyRole('RECEPTIONIST','SUPER_ADMIN')";
    private static final String ADMIN = "hasRole('SUPER_ADMIN')";

    @Mock private ProductService productService;
    @Mock private PosOrderService posOrderService;
    @Mock private CommissionService commissionService;
    @Mock private TrainerPayRateService trainerPayRateService;

    private static boolean isEndpoint(Method m) {
        return m.isAnnotationPresent(GetMapping.class) || m.isAnnotationPresent(PostMapping.class)
                || m.isAnnotationPresent(PutMapping.class) || m.isAnnotationPresent(DeleteMapping.class);
    }

    private static Set<String> endpointsWithoutPreAuthorize(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(PosCommissionControllersTest::isEndpoint)
                .filter(m -> !m.isAnnotationPresent(PreAuthorize.class))
                .map(Method::getName)
                .collect(Collectors.toSet());
    }

    private static Method method(Class<?> controller, String name) {
        return Arrays.stream(controller.getDeclaredMethods()).filter(m -> m.getName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void everyEndpoint_hasPreAuthorize_exceptTheTwoPublicProductReads() {
        // Chỉ xem danh sách đang bán và chi tiết sản phẩm mở cho mọi người đã đăng nhập (hội viên xem giá).
        assertThat(endpointsWithoutPreAuthorize(ProductController.class)).containsExactlyInAnyOrder("listActive", "getById");
        assertThat(endpointsWithoutPreAuthorize(PosController.class)).isEmpty();
        assertThat(endpointsWithoutPreAuthorize(CommissionController.class)).isEmpty();
        assertThat(endpointsWithoutPreAuthorize(TrainerPayRateController.class)).isEmpty();
    }

    @Test
    void writesAndOrders_areStaffOnly() {
        for (String name : List.of("listAll", "create", "update", "deactivate")) {
            assertThat(method(ProductController.class, name).getAnnotation(PreAuthorize.class).value()).as(name).isEqualTo(STAFF);
        }
        for (String name : List.of("create", "getById", "search", "cancel")) {
            assertThat(method(PosController.class, name).getAnnotation(PreAuthorize.class).value()).as(name).isEqualTo(STAFF);
        }
    }

    @Test
    void memberPos_isMemberOnly_andRequestCannotCarryBuyerOrPaymentMethod() {
        assertThat(endpointsWithoutPreAuthorize(MemberPosController.class)).isEmpty();
        for (String name : List.of("create", "list", "getById")) {
            assertThat(method(MemberPosController.class, name).getAnnotation(PreAuthorize.class).value())
                    .as(name).isEqualTo("hasRole('MEMBER')");
        }
        // Người mua lấy từ JWT, phương thức thanh toán cố định: request không được có hai trường này.
        assertThat(Arrays.stream(com.gym.management.dto.request.MemberPosOrderRequest.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)).doesNotContain("memberId", "paymentMethod");
    }

    @Test
    void payRatesAndPayouts_areSuperAdminOnly_andOwnSalaryIsTrainerOnly() {
        assertThat(method(TrainerPayRateController.class, "getByTrainerId").getAnnotation(PreAuthorize.class).value()).isEqualTo(ADMIN);
        assertThat(method(TrainerPayRateController.class, "upsert").getAnnotation(PreAuthorize.class).value()).isEqualTo(ADMIN);
        assertThat(method(CommissionController.class, "search").getAnnotation(PreAuthorize.class).value()).isEqualTo(ADMIN);
        assertThat(method(CommissionController.class, "markPaid").getAnnotation(PreAuthorize.class).value()).isEqualTo(ADMIN);
        assertThat(method(CommissionController.class, "myCommissions").getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('TRAINER')");
    }

    @Test
    void pos_getByIdAndCancel_passTheRequestedId() {
        PosController controller = new PosController(posOrderService);
        UUID id = UUID.randomUUID();

        controller.getById(id);
        controller.cancel(id);

        verify(posOrderService).getById(id);
        verify(posOrderService).cancelOrder(id);
        verifyNoMoreInteractions(posOrderService);
    }

    @Test
    void pos_search_passesBranchDatesAndPaging() {
        PosController controller = new PosController(posOrderService);
        UUID branch = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        controller.search(branch, from, to, 3, 10, "createdAt,desc");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(posOrderService).search(eq(branch), eq(from), eq(to), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(3);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void commissions_search_parsesMonthAndPassesStaff() {
        CommissionController controller = new CommissionController(commissionService);
        UUID staff = UUID.randomUUID();

        controller.search(staff, "2026-10", 0, 20, "createdAt,desc");

        verify(commissionService).search(eq(staff), eq(YearMonth.of(2026, 10)), any(Pageable.class));
    }

    @Test
    void commissions_search_blankMonthMeansAllMonths() {
        CommissionController controller = new CommissionController(commissionService);

        controller.search(null, "  ", 0, 20, "createdAt,desc");
        controller.search(null, null, 0, 20, "createdAt,desc");

        verify(commissionService, org.mockito.Mockito.times(2)).search(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void commissions_search_badMonthIsAValidationError_notAServerError() {
        CommissionController controller = new CommissionController(commissionService);

        assertThatThrownBy(() -> controller.search(null, "10/2026", 0, 20, "createdAt,desc"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoMoreInteractions(commissionService);
    }

    @Test
    void commissions_markPaid_passesTheRequestedId() {
        CommissionController controller = new CommissionController(commissionService);
        UUID id = UUID.randomUUID();

        controller.markPaid(id);

        verify(commissionService).markPaid(id);
        verifyNoMoreInteractions(commissionService);
    }

    @Test
    void payRates_upsert_passesTrainerIdAndRequest() {
        TrainerPayRateController controller = new TrainerPayRateController(trainerPayRateService);
        UUID trainer = UUID.randomUUID();
        TrainerPayRateRequest request = new TrainerPayRateRequest();
        request.setAmountPerSession(new BigDecimal("150000"));

        controller.upsert(trainer, request);

        verify(trainerPayRateService).upsert(trainer, request);
        verifyNoMoreInteractions(trainerPayRateService);
    }

    @Test
    void products_deactivate_isSoftDeleteCall() {
        ProductController controller = new ProductController(productService);
        UUID id = UUID.randomUUID();

        controller.deactivate(id);

        verify(productService).deactivate(id);
        verifyNoMoreInteractions(productService);
    }
}
