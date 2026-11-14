package com.gym.management.service.impl;

import com.gym.management.constant.RelatedType;
import com.gym.management.constant.PaymentMethodType;
import com.gym.management.dto.request.MemberPosOrderRequest;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
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

        return placeOrder(staff, member, request.getBranchId(), request.getPaymentMethod(), request.getItems());
    }

    @Override
    @Transactional
    public PosOrderResponse createOwnOrder(UUID memberId, MemberPosOrderRequest request) {
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Không xác định được hội viên"));
        // Thanh toán mô phỏng cùng cách MemberSelfPurchaseService của Agent 2: LOCAL_CONFIRM, xác nhận ngay.
        return placeOrder(null, member, request.getBranchId(), PaymentMethodType.LOCAL_CONFIRM, request.getItems());
    }

    /**
     * Lõi tạo đơn dùng chung cho đơn tại quầy (staff != null) và đơn hội viên tự mua (staff == null):
     * giá lấy từ DB, khoá bi quan theo id tăng dần, trừ kho, rồi thu tiền ngay trong cùng transaction.
     */
    private PosOrderResponse placeOrder(User staff, User member, UUID branchId, String paymentMethod,
                                        List<PosOrderCreateRequest.Item> lines) {
        PosOrder order = new PosOrder();
        order.setStaff(staff);
        order.setMember(member);
        order.setBranchId(branchId);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(PosOrderStatus.PENDING);

        // Gộp các dòng trùng sản phẩm (giữ thứ tự khách chọn để hiển thị).
        Map<UUID, Integer> wanted = new LinkedHashMap<>();
        for (PosOrderCreateRequest.Item line : lines) {
            wanted.merge(line.getProductId(), line.getQuantity(), Integer::sum);
        }

        // Khoá bi quan từng dòng sản phẩm, luôn theo thứ tự id TĂNG DẦN: nếu 2 đơn cùng bán sản phẩm A và B
        // mà khoá theo thứ tự khách chọn (A,B và B,A) thì sẽ khoá chéo nhau và SQL Server giết 1 đơn (deadlock).
        // Khoá được giữ tới hết transaction nên 2 đơn trừ cùng 1 sản phẩm không thể cùng đọc tồn kho cũ.
        Map<UUID, Product> locked = new HashMap<>();
        for (UUID productId : new TreeSet<>(wanted.keySet())) {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            int quantity = wanted.get(productId);

            if (!product.isActive()) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm đã ngừng bán: " + product.getName());
            }
            if (product.getStockQuantity() < quantity) {
                throw new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK,
                        "Sản phẩm \"" + product.getName() + "\" không đủ tồn kho (còn " + product.getStockQuantity() + ")");
            }

            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);
            locked.put(productId, product);
        }

        for (Map.Entry<UUID, Integer> line : wanted.entrySet()) {
            Product product = locked.get(line.getKey());
            PosOrderItem item = new PosOrderItem();
            item.setProduct(product);
            item.setQuantity(line.getValue());
            // Giá chụp từ DB tại thời điểm bán — không tin bất kỳ giá nào client có thể gửi lên.
            item.setUnitPrice(product.getPrice());
            order.addItem(item);
        }

        BigDecimal total = order.getTotalAmount(); // suy ra từ các dòng, không lưu cột (CH5)
        // saveAndFlush: dòng pos_orders phải thật sự được ghi TRƯỚC dòng payment_transactions, vì từ V7 giao dịch có
        // khoá ngoại pos_order_id -> pos_orders(id). Không dựa vào thứ tự hàng đợi INSERT mặc định của Hibernate.
        order = posOrderRepository.saveAndFlush(order);

        // Bán tại quầy = thu tiền ngay, không có bước "chờ xác nhận" như Membership —
        // tạo transaction PENDING rồi confirm luôn trong cùng request/transaction.
        // user của giao dịch = NGƯỜI MUA (hội viên của đơn, null nếu khách vãng lai) — theo CH1
        // (docs/DB_DESIGN_REVIEW.md). Nhân viên thu tiền được lưu ở pos_orders.staff_id, không ở đây.
        PaymentTransactionResponse pendingTx = paymentService.createPendingTransaction(
                member, RelatedType.POS, order.getId(), total);
        PaymentTransactionResponse confirmedTx = paymentService.confirmPayment(
                pendingTx.getId(), paymentMethod);

        order.setStatus(PosOrderStatus.COMPLETED);
        order = posOrderRepository.save(order);

        return toResponse(order, confirmedTx.getId());
    }

    @Override
    @Transactional
    public PosOrderResponse cancelOrder(UUID id) {
        PosOrder order = posOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POS_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() == PosOrderStatus.CANCELLED) {
            return toResponse(order, null); // idempotent — huỷ lại không lỗi
        }
        if (order.getStatus() != PosOrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ huỷ được đơn đã hoàn thành");
        }

        // Hoàn tiền TRƯỚC khi hoàn kho: nếu không hoàn được (PAYMENT_NOT_REFUNDABLE) thì cả thao tác huỷ
        // rollback — đơn vẫn COMPLETED, tồn kho không đổi. Agent 2 tự tìm giao dịch theo đơn (cột pos_order_id
        // của V7); Agent 5 không giữ transactionId. Huỷ lại đơn đã CANCELLED thoát sớm ở trên nên không hoàn lần hai.
        paymentService.refundByRelated(RelatedType.POS, order.getId());

        // Cùng thứ tự khoá (id tăng dần) với createOrder để huỷ đơn không khoá chéo với đơn đang tạo.
        List<PosOrderItem> byProduct = new ArrayList<>(order.getItems());
        byProduct.sort(Comparator.comparing(i -> i.getProduct().getId()));
        for (PosOrderItem item : byProduct) {
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
                .orElseThrow(() -> new BusinessException(ErrorCode.POS_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng")), null);
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

    @Override
    @Transactional(readOnly = true)
    public PosOrderResponse getOwnById(UUID memberId, UUID orderId) {
        PosOrder order = posOrderRepository.findById(orderId)
                .filter(o -> o.getMember() != null && memberId.equals(o.getMember().getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.POS_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));
        return toResponse(order, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PosOrderResponse> searchOwn(UUID memberId, Pageable pageable) {
        return PageResponse.from(posOrderRepository.findByMemberId(memberId, pageable), order -> toResponse(order, null));
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
                .staffId(order.getStaff() != null ? order.getStaff().getId() : null)
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
