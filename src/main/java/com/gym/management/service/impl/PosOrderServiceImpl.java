package com.gym.management.service.impl;

import com.gym.management.constant.RelatedType;
import com.gym.management.dto.request.PosOrderCreateRequest;
import com.gym.management.dto.response.PageResponse;
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
import com.gym.management.service.PosOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 5 - POS & Commission Module.
 *
 * Lưu ý quan trọng: service này KHÔNG tự viết logic xác nhận thanh toán — mọi thao tác
 * với payment_transactions đi qua {@link PaymentService} dùng chung với Agent 2 (xem Javadoc
 * PaymentService). Service này chỉ chịu trách nhiệm: tính tiền, trừ/hoàn tồn kho, và đổi
 * status của chính pos_orders.
 */
@Service
@RequiredArgsConstructor
public class PosOrderServiceImpl implements PosOrderService {

    private final PosOrderRepository posOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public PosOrderResponse createOrder(UUID staffId, PosOrderCreateRequest request) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Không xác định được nhân viên lập đơn"));

        User member = null;
        if (request.getMemberId() != null) {
            member = userRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy hội viên"));
        }

        PosOrder order = new PosOrder();
        order.setStaff(staff);
        order.setMember(member);
        order.setBranchId(request.getBranchId());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(PosOrderStatus.PENDING);

        for (PosOrderCreateRequest.Item line : request.getItems()) {
            // Khoá bi quan trên đúng dòng sản phẩm — giữ khoá tới hết transaction này để
            // 2 đơn trừ cùng 1 sản phẩm gần như đồng thời không thể cùng đọc tồn kho cũ.
            Product product = productRepository.findByIdForUpdate(line.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            if (!product.isActive()) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm đã ngừng bán: " + product.getName());
            }
            if (product.getStockQuantity() < line.getQuantity()) {
                throw new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK,
                        "Sản phẩm \"" + product.getName() + "\" không đủ tồn kho (còn " + product.getStockQuantity() + ")");
            }

            product.setStockQuantity(product.getStockQuantity() - line.getQuantity());
            productRepository.save(product);

            // Giá chụp từ DB tại thời điểm bán — không tin bất kỳ giá nào client có thể gửi lên.
            BigDecimal unitPrice = product.getPrice();

            PosOrderItem item = new PosOrderItem();
            item.setProduct(product);
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(unitPrice);
            order.addItem(item);
        }

        BigDecimal total = order.getTotalAmount(); // suy ra từ các dòng, không lưu cột (CH5)
        order = posOrderRepository.save(order);

        // Bán tại quầy = thu tiền ngay, không có bước "chờ xác nhận" như Membership —
        // tạo transaction PENDING rồi confirm luôn trong cùng request/transaction.
        // user của giao dịch = NGƯỜI MUA (hội viên của đơn, null nếu khách vãng lai) — theo CH1
        // (docs/DB_DESIGN_REVIEW.md). Nhân viên thu tiền được lưu ở pos_orders.staff_id, không ở đây.
        PaymentTransactionResponse pendingTx = paymentService.createPendingTransaction(
                member, RelatedType.POS, order.getId(), total);
        PaymentTransactionResponse confirmedTx = paymentService.confirmPayment(
                pendingTx.getId(), request.getPaymentMethod());

        order.setStatus(PosOrderStatus.COMPLETED);
        order = posOrderRepository.save(order);

        return toResponse(order, confirmedTx.getId());
    }

    @Override
    @Transactional
    public PosOrderResponse cancelOrder(UUID id) {
        PosOrder order = posOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() == PosOrderStatus.CANCELLED) {
            return toResponse(order, null); // idempotent — huỷ lại không lỗi
        }
        if (order.getStatus() != PosOrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ huỷ được đơn đã hoàn thành");
        }

        for (PosOrderItem item : order.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(PosOrderStatus.CANCELLED);
        order = posOrderRepository.save(order);
        return toResponse(order, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PosOrderResponse getById(UUID id) {
        return toResponse(posOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đơn hàng")), null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PosOrderResponse> search(UUID branchId, LocalDate from, LocalDate to, Pageable pageable) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now();
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDateTime fromDateTime = effectiveFrom.atStartOfDay();
        LocalDateTime toDateTime = effectiveTo.plusDays(1).atStartOfDay(); // exclusive upper bound
        return PageResponse.from(
                posOrderRepository.search(branchId, fromDateTime, toDateTime, pageable),
                order -> toResponse(order, null));
    }

    private PosOrderResponse toResponse(PosOrder order, UUID paymentTransactionId) {
        List<PosOrderResponse.Item> items = order.getItems().stream()
                .map(item -> PosOrderResponse.Item.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return PosOrderResponse.builder()
                .id(order.getId())
                .memberId(order.getMember() != null ? order.getMember().getId() : null)
                .staffId(order.getStaff().getId())
                .branchId(order.getBranchId())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .paymentTransactionId(paymentTransactionId)
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
