package com.gym.management.service;

import com.gym.management.dto.response.MemberPackageResponse;
import com.gym.management.dto.response.PackagePurchaseResponse;
import com.gym.management.entity.MemberPackage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent 2 - Membership, Package & Payment Module.
 *
 * ⚠️ {@link #getCurrentMembership(UUID)} là NGUỒN CHÂN LÝ DUY NHẤT cho khái niệm
 * "gói tập hiện tại của 1 user" trong TOÀN BỘ hệ thống (định nghĩa = bản ghi
 * member_packages có status = ACTIVE). Agent 3 (giới hạn buổi/tuần), Agent 5 (gián tiếp
 * qua Agent 3), Agent 6 (sắp hết hạn) đều PHẢI gọi lại đúng method này — KHÔNG tự viết
 * query "SELECT ... WHERE status = 'ACTIVE'" ở nơi khác. Xem REQUIREMENTS.md mục 2 & 7,
 * và bug thật đã xảy ra ở bản .NET (2 trang hiển thị lệch nhau vì tự định nghĩa 2 lần).
 */
public interface MembershipService {

    /**
     * Trả về gói ACTIVE hiện tại (nếu có). Tự chạy lazy-check TRƯỚC khi trả kết quả:
     * ACTIVE quá end_date → chuyển EXPIRED; SCHEDULED tới đúng start_date → chuyển ACTIVE.
     * Không cần @Scheduled/cron — chạy ngay tại thời điểm đọc.
     */
    Optional<MemberPackage> getCurrentMembership(UUID userId);

    /** Bản DTO của {@link #getCurrentMembership}, ném MEMBERSHIP_NOT_FOUND nếu không có gói ACTIVE nào. */
    MemberPackageResponse getCurrentMembershipResponse(UUID userId);

    /** @deprecated dùng {@link #getHistory(UUID, org.springframework.data.domain.Pageable)}. */
    @Deprecated
    List<MemberPackageResponse> getHistory(UUID userId);

    /** Lịch sử toàn bộ gói đã đăng ký (mọi trạng thái), có phân trang, mới nhất trước. */
    com.gym.management.dto.response.PageResponse<MemberPackageResponse> getHistory(UUID userId, org.springframework.data.domain.Pageable pageable);

    /**
     * Đăng ký gói MỚI HOÀN TOÀN — dùng cho hội viên mới hoặc hội viên gia hạn sau khi
     * gói cũ đã EXPIRED (không còn ACTIVE). Chặn nếu đang có gói PENDING; KHÔNG chặn
     * nếu đang có gói ACTIVE (xem lưu ý bug cũ ở REQUIREMENTS.md mục 2 — đây KHÔNG phải
     * luồng nâng/hạ cấp, dùng {@link #upgradePackage} / {@link #downgradePackage} cho việc đó).
     */
    PackagePurchaseResponse registerNewPackage(UUID targetUserId, UUID packageId, UUID branchId);

    /**
     * Nâng cấp: newPackage.price ≥ giá gói đang ACTIVE. Huỷ (CANCELLED) gói cũ NGAY LẬP TỨC,
     * tạo gói mới PENDING (ACTIVE khi thanh toán xong).
     *
     * @param actingUserId người đang gọi API (từ CurrentUserId/@CurrentUser)
     * @param staffOverride true nếu người gọi là RECEPTIONIST/SUPER_ADMIN (được thao tác hộ bất kỳ ai);
     *                      false thì service tự đối chiếu currentMemberPackage.user.id == actingUserId,
     *                      ném AUTH_FORBIDDEN_ROLE nếu không khớp (member không được đổi hộ người khác)
     */
    PackagePurchaseResponse upgradePackage(UUID currentMemberPackageId, UUID newPackageId, UUID branchId,
                                            UUID actingUserId, boolean staffOverride);

    /**
     * Hạ cấp: newPackage.price < giá gói đang ACTIVE. Gói cũ GIỮ NGUYÊN chạy tới hết hạn,
     * tạo bản ghi SCHEDULED bắt đầu đúng ngày gói cũ end_date.
     *
     * @see #upgradePackage(UUID, UUID, UUID, UUID, boolean) tham số actingUserId/staffOverride tương tự
     */
    PackagePurchaseResponse downgradePackage(UUID currentMemberPackageId, UUID newPackageId, UUID branchId,
                                              UUID actingUserId, boolean staffOverride);

    /**
     * Gọi bởi {@link PaymentService#confirmPayment} khi 1 giao dịch MEMBERSHIP chuyển SUCCESS —
     * chuyển member_package từ PENDING → ACTIVE, start_date GHI ĐÈ = ngày xác nhận thật
     * (không giữ ngày dự kiến lúc đăng ký). Idempotent: gọi lại khi đã ACTIVE thì không làm gì thêm.
     */
    MemberPackageResponse activateFromPendingPayment(UUID memberPackageId);

    /**
     * Trừ 1 buổi tập của gói ACTIVE hiện tại — dùng bởi Agent 3/4 khi 1 booking chuyển COMPLETED
     * hoặc member check-in hợp lệ. Bỏ qua (không trừ) nếu gói không giới hạn buổi (remaining_sessions
     * = null, ví dụ TIME_BASED không kèm session_count). Ném MEMBERSHIP_NO_SESSION nếu đã hết buổi.
     */
    void consumeSession(UUID userId);
}
