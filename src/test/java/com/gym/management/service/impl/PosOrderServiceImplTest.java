package com.gym.management.service.impl;

import com.gym.management.constant.RelatedType;
import com.gym.management.dto.request.PosOrderCreateRequest;
import com.gym.management.dto.response.PaymentTransactionResponse;
import com.gym.management.dto.response.PosOrderResponse;
import com.gym.management.entity.PosOrder;
import com.gym.management.entity.PosOrderItem;
import com.gym.management.entity.PosOrderStatus;
import com.gym.management.entity.Product;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.PosOrderRepository;
import com.gym.management.repository.ProductRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PosOrderServiceImplTest {

    @Mock private PosOrderRepository posOrderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentService paymentService;
    @InjectMocks private PosOrderServiceImpl service;

    private final UUID staffId = UUID.randomUUID();
    private User staff;
    private UUID txId;

    @BeforeEach
    void setUp() {
        staff = new User();
        ReflectionTestUtils.setField(staff, "id", staffId);
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));
        // save() trả lại chính entity, gán id như Hibernate sẽ làm
        when(posOrderRepository.save(any(PosOrder.class))).thenAnswer(inv -> {
            PosOrder o = inv.getArgument(0);
            if (o.getId() == null) {
                ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
            }
            return o;
        });
        txId = UUID.randomUUID();
        when(paymentService.createPendingTransaction(any(), eq(RelatedType.POS), any(), any()))
                .thenReturn(PaymentTransactionResponse.builder().id(txId).build());
        when(paymentService.confirmPayment(eq(txId), any()))
                .thenReturn(PaymentTransactionResponse.builder().id(txId).status("SUCCESS").build());
    }

    private Product product(String name, String price, int stock, boolean active) {
        Product p = new Product();
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStockQuantity(stock);
        p.setActive(active);
        when(productRepository.findByIdForUpdate(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private PosOrderCreateRequest request(Object... productAndQty) {
        PosOrderCreateRequest req = new PosOrderCreateRequest();
        req.setPaymentMethod("CASH");
        java.util.ArrayList<PosOrderCreateRequest.Item> items = new java.util.ArrayList<>();
        for (int i = 0; i < productAndQty.length; i += 2) {
            PosOrderCreateRequest.Item it = new PosOrderCreateRequest.Item();
            it.setProductId(((Product) productAndQty[i]).getId());
            it.setQuantity((Integer) productAndQty[i + 1]);
            items.add(it);
        }
        req.setItems(items);
        return req;
    }

    @Test
    void createOrder_computesTotalFromDbPrice_deductsStock_andConfirmsPayment() {
        Product water = product("Nước suối", "10000.00", 50, true);
        Product whey = product("Whey", "500000.00", 5, true);

        PosOrderResponse res = service.createOrder(staffId, request(water, 3, whey, 2));

        assertThat(res.getTotalAmount()).isEqualByComparingTo("1030000.00");
        assertThat(res.getStatus()).isEqualTo(PosOrderStatus.COMPLETED);
        assertThat(res.getPaymentTransactionId()).isEqualTo(txId);
        assertThat(res.getItems()).hasSize(2);
        assertThat(water.getStockQuantity()).isEqualTo(47);
        assertThat(whey.getStockQuantity()).isEqualTo(3);
        // khách vãng lai (không có memberId) → user của giao dịch là null, KHÔNG phải nhân viên (CH1)
        verify(paymentService).createPendingTransaction(isNull(), eq(RelatedType.POS), any(UUID.class),
                eq(new BigDecimal("1030000.00")));
        verify(paymentService).confirmPayment(txId, "CASH");
    }

    @Test
    void createOrder_snapshotsUnitPricePerItem() {
        Product water = product("Nước suối", "10000.00", 10, true);

        PosOrderResponse res = service.createOrder(staffId, request(water, 2));

        assertThat(res.getItems().get(0).getUnitPrice()).isEqualByComparingTo("10000.00");
        assertThat(res.getItems().get(0).getSubtotal()).isEqualByComparingTo("20000.00");
    }

    @Test
    void createOrder_outOfStock_throwsAndDoesNotTouchPayment() {
        Product whey = product("Whey", "500000.00", 1, true);

        assertThatThrownBy(() -> service.createOrder(staffId, request(whey, 2)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_OUT_OF_STOCK);

        verify(paymentService, never()).createPendingTransaction(any(), any(), any(), any());
        assertThat(whey.getStockQuantity()).isEqualTo(1);
    }

    @Test
    void createOrder_exactlyRemainingStock_isAllowed() {
        Product whey = product("Whey", "500000.00", 2, true);

        service.createOrder(staffId, request(whey, 2));

        assertThat(whey.getStockQuantity()).isZero();
    }

    @Test
    void createOrder_inactiveProduct_isRejected() {
        Product old = product("Hàng cũ", "1000.00", 10, false);

        assertThatThrownBy(() -> service.createOrder(staffId, request(old, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void createOrder_unknownProduct_isRejected() {
        PosOrderCreateRequest req = new PosOrderCreateRequest();
        req.setPaymentMethod("CASH");
        PosOrderCreateRequest.Item it = new PosOrderCreateRequest.Item();
        it.setProductId(UUID.randomUUID());
        it.setQuantity(1);
        req.setItems(List.of(it));
        when(productRepository.findByIdForUpdate(it.getProductId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(staffId, req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void createOrder_withMember_paymentUserIsTheBuyerNotTheStaff() {
        UUID memberId = UUID.randomUUID();
        User member = new User();
        ReflectionTestUtils.setField(member, "id", memberId);
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        Product water = product("Nước suối", "10000.00", 10, true);
        PosOrderCreateRequest req = request(water, 1);
        req.setMemberId(memberId);

        PosOrderResponse res = service.createOrder(staffId, req);

        assertThat(res.getMemberId()).isEqualTo(memberId);
        assertThat(res.getStaffId()).isEqualTo(staffId);
        verify(paymentService).createPendingTransaction(eq(member), eq(RelatedType.POS), any(UUID.class),
                eq(new BigDecimal("10000.00")));
    }

    @Test
    void createOrder_walkInCustomer_hasNullMember() {
        Product water = product("Nước suối", "10000.00", 10, true);

        PosOrderResponse res = service.createOrder(staffId, request(water, 1));

        assertThat(res.getMemberId()).isNull();
        assertThat(res.getStaffId()).isEqualTo(staffId);
    }

    @Test
    void cancelOrder_restoresStock_andIsIdempotent() {
        Product water = product("Nước suối", "10000.00", 7, true);
        PosOrder order = new PosOrder();
        UUID orderId = UUID.randomUUID();
        ReflectionTestUtils.setField(order, "id", orderId);
        order.setStaff(staff);
        order.setStatus(PosOrderStatus.COMPLETED);
        order.setPaymentMethod("CASH");
        PosOrderItem item = new PosOrderItem();
        item.setProduct(water);
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("10000.00"));
        order.addItem(item);
        when(posOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PosOrderResponse first = service.cancelOrder(orderId);
        PosOrderResponse second = service.cancelOrder(orderId); // huỷ lại

        assertThat(first.getStatus()).isEqualTo(PosOrderStatus.CANCELLED);
        assertThat(second.getStatus()).isEqualTo(PosOrderStatus.CANCELLED);
        assertThat(water.getStockQuantity()).isEqualTo(10); // hoàn đúng 1 lần
    }

    @Test
    void cancelOrder_pendingOrder_isRejected() {
        PosOrder order = new PosOrder();
        UUID orderId = UUID.randomUUID();
        ReflectionTestUtils.setField(order, "id", orderId);
        order.setStaff(staff);
        order.setStatus(PosOrderStatus.PENDING);
        when(posOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelOrder(orderId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
