package com.gym.management.service.impl;

import com.gym.management.constant.MembershipStatus;
import com.gym.management.constant.RelatedType;
import com.gym.management.dto.response.MemberPackageResponse;
import com.gym.management.dto.response.PackagePurchaseResponse;
import com.gym.management.dto.response.PaymentTransactionResponse;
import com.gym.management.entity.Branch;
import com.gym.management.entity.MemberPackage;
import com.gym.management.entity.Package;
import com.gym.management.entity.PaymentTransaction;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.BranchRepository;
import com.gym.management.repository.MemberPackageRepository;
import com.gym.management.repository.PackageRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.MembershipService;
import com.gym.management.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 2 - Membership, Package & Payment Module.
 * Xem Javadoc đầy đủ ở {@link MembershipService} — file này chỉ hiện thực hoá.
 *
 * Quyết định thiết kế đáng chú ý (đọc trước khi sửa):
 * 1. {@link #getCurrentMembership} luôn chạy lazy-check cho TOÀN BỘ bản ghi của user trước
 *    khi xác định ACTIVE — không tin cột status "thô" nếu chưa qua bước này.
 * 2. Hạ cấp KHÔNG đi qua trạng thái PENDING (khác với đăng ký mới/nâng cấp) — tạo thẳng
 *    SCHEDULED và coi như thu tiền ngay lúc lập lịch (payment_transaction = SUCCESS ngay).
 *    ĐÂY LÀ GIẢ ĐỊNH vì REQUIREMENTS.md không nói rõ bước thanh toán của luồng hạ cấp —
 *    cần Product Owner (Phi) xác nhận lại nếu muốn hạ cấp cũng phải qua bước confirm riêng.
 * 3. PaymentTransaction được tạo trực tiếp qua {@link PaymentService} (không tự đụng
 *    PaymentTransactionRepository ở đây) để giữ đúng "1 nguồn chân lý" cho việc ghi log
 *    giao dịch — kể cả khi Agent 2 sở hữu cả 2 entity.
 */
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MemberPackageRepository memberPackageRepository;
    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PaymentService paymentService;

    // ============================================================
    // getCurrentMembership — NGUỒN CHÂN LÝ DUY NHẤT
    // ============================================================

    @Override
    @Transactional
    public Optional<MemberPackage> getCurrentMembership(UUID userId) {
        LocalDate today = LocalDate.now(VN_ZONE);
        List<MemberPackage> all = memberPackageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (MemberPackage mp : all) {
            applyLazyTransition(mp, today);
        }
        return memberPackageRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, MembershipStatus.ACTIVE);
    }

    /** Lazy-check cho 1 bản ghi — REQUIREMENTS.md mục 2 & mục 7 (điểm 3: lazy-check thay vì cron). */
    private void applyLazyTransition(MemberPackage mp, LocalDate today) {
        if (MembershipStatus.ACTIVE.equals(mp.getStatus())
                && mp.getEndDate() != null
                && today.isAfter(mp.getEndDate())) {
            mp.setStatus(MembershipStatus.EXPIRED);
            memberPackageRepository.save(mp);
        } else if (MembershipStatus.SCHEDULED.equals(mp.getStatus())
                && !today.isBefore(mp.getStartDate())) {
            mp.setStatus(MembershipStatus.ACTIVE);
            memberPackageRepository.save(mp);
        }
    }

    @Override
    public MemberPackageResponse getCurrentMembershipResponse(UUID userId) {
        MemberPackage mp = getCurrentMembership(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Bạn hiện không có gói tập nào đang hoạt động"));
        return toResponse(mp);
    }

    @Override
    public List<MemberPackageResponse> getHistory(UUID userId) {
        return memberPackageRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public com.gym.management.dto.response.PageResponse<MemberPackageResponse> getHistory(UUID userId, org.springframework.data.domain.Pageable pageable) {
        return com.gym.management.dto.response.PageResponse.from(
                memberPackageRepository.findByUserId(userId, pageable), this::toResponse);
    }

    // ============================================================
    // Đăng ký mới
    // ============================================================

    @Override
    @Transactional
    public PackagePurchaseResponse registerNewPackage(UUID targetUserId, UUID packageId, UUID branchId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));
        Package pkg = requireActivePackage(packageId);
        Branch branch = resolveBranch(branchId);

        // Gán đủ field bắt buộc TRƯỚC khi validate (bài học REQUIREMENTS.md mục 7 / bug "fail âm thầm" ở .NET)
        MemberPackage newMp = new MemberPackage();
        newMp.setUser(user);
        newMp.setGymPackage(pkg);
        newMp.setBranch(branch);

        // Chặn nếu đang có PENDING; KHÔNG chặn nếu đang có ACTIVE (đây không phải luồng nâng/hạ cấp,
        // nên nếu có ACTIVE thì phải dùng upgrade/downgrade — nhưng theo đúng REQUIREMENTS ta vẫn
        // chỉ chặn theo PENDING, không tự ý chặn thêm theo ACTIVE ở đây).
        requireNoPendingPackage(targetUserId);

        newMp.setStatus(MembershipStatus.PENDING);
        newMp.setStartDate(LocalDate.now(VN_ZONE)); // dự kiến — sẽ bị ghi đè bằng ngày thanh toán thật lúc confirm
        newMp = memberPackageRepository.save(newMp);

        PaymentTransactionResponse tx = paymentService.createPendingTransaction(
                user, RelatedType.MEMBERSHIP, newMp.getId(), pkg.getPrice());

        return PackagePurchaseResponse.builder()
                .memberPackage(toResponse(newMp))
                .paymentTransaction(tx)
                .build();
    }

    // ============================================================
    // Nâng cấp
    // ============================================================

    @Override
    @Transactional
    public PackagePurchaseResponse upgradePackage(UUID currentMemberPackageId, UUID newPackageId, UUID branchId,
                                                   UUID actingUserId, boolean staffOverride) {
        MemberPackage current = requireActiveMemberPackage(currentMemberPackageId);
        requireOwnershipOrStaff(current, actingUserId, staffOverride);
        Package newPkg = requireActivePackage(newPackageId);

        if (newPkg.getPrice().compareTo(current.getGymPackage().getPrice()) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Gói mới phải có giá lớn hơn hoặc bằng gói hiện tại để nâng cấp — dùng API hạ cấp nếu giá thấp hơn");
        }

        User user = current.getUser();
        requireNoPendingPackage(user.getId());

        // Huỷ gói cũ NGAY LẬP TỨC (không đợi thanh toán xong) — REQUIREMENTS.md mục 2
        current.setStatus(MembershipStatus.CANCELLED);
        memberPackageRepository.save(current);

        Branch branch = branchId != null ? resolveBranch(branchId) : current.getBranch();

        MemberPackage newMp = new MemberPackage();
        newMp.setUser(user);
        newMp.setGymPackage(newPkg);
        newMp.setBranch(branch);
        newMp.setStatus(MembershipStatus.PENDING);
        newMp.setStartDate(LocalDate.now(VN_ZONE)); // dự kiến, ghi đè khi confirm
        newMp = memberPackageRepository.save(newMp);

        PaymentTransactionResponse tx = paymentService.createPendingTransaction(
                user, RelatedType.MEMBERSHIP, newMp.getId(), newPkg.getPrice());

        return PackagePurchaseResponse.builder()
                .memberPackage(toResponse(newMp))
                .paymentTransaction(tx)
                .build();
    }

    // ============================================================
    // Hạ cấp
    // ============================================================

    @Override
    @Transactional
    public PackagePurchaseResponse downgradePackage(UUID currentMemberPackageId, UUID newPackageId, UUID branchId,
                                                     UUID actingUserId, boolean staffOverride) {
        MemberPackage current = requireActiveMemberPackage(currentMemberPackageId);
        requireOwnershipOrStaff(current, actingUserId, staffOverride);
        Package newPkg = requireActivePackage(newPackageId);

        if (newPkg.getPrice().compareTo(current.getGymPackage().getPrice()) >= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Gói mới phải có giá thấp hơn gói hiện tại để hạ cấp — dùng API nâng cấp nếu giá cao hơn hoặc bằng");
        }
        if (current.getEndDate() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Gói hiện tại không có ngày hết hạn xác định, không thể lên lịch hạ cấp");
        }

        User user = current.getUser();
        Branch branch = branchId != null ? resolveBranch(branchId) : current.getBranch();

        // Gói cũ GIỮ NGUYÊN — chỉ tạo bản ghi SCHEDULED bắt đầu NGÀY SAU end_date của gói cũ.
        // end_date là ngày CUỐI CÙNG gói cũ còn hiệu lực (lazy-check chỉ chuyển EXPIRED khi
        // today > end_date), nên nếu start_date = end_date thì đúng ngày đó cả 2 gói cùng ACTIVE.
        MemberPackage newMp = new MemberPackage();
        newMp.setUser(user);
        newMp.setGymPackage(newPkg);
        newMp.setBranch(branch);
        newMp.setStatus(MembershipStatus.SCHEDULED);
        newMp.setStartDate(current.getEndDate().plusDays(1));
        newMp.setEndDate(computeEndDate(newPkg, newMp.getStartDate()));
        newMp.setRemainingSessions(newPkg.getSessionCount());
        newMp = memberPackageRepository.save(newMp);

        // GIẢ ĐỊNH (xem Javadoc đầu file): hạ cấp thu tiền ngay lúc lập lịch, không qua bước confirm riêng.
        PaymentTransactionResponse tx = paymentService.createPendingTransaction(
                user, RelatedType.MEMBERSHIP, newMp.getId(), newPkg.getPrice());
        tx = paymentService.confirmPayment(tx.getId(), null);

        return PackagePurchaseResponse.builder()
                .memberPackage(toResponse(newMp))
                .paymentTransaction(tx)
                .build();
    }

    // ============================================================
    // Kích hoạt khi thanh toán xong (gọi bởi PaymentServiceImpl)
    // ============================================================

    @Override
    @Transactional
    public MemberPackageResponse activateFromPendingPayment(UUID memberPackageId) {
        MemberPackage mp = memberPackageRepository.findById(memberPackageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        if (!MembershipStatus.PENDING.equals(mp.getStatus())) {
            // Idempotent: confirm gọi lại lần 2, hoặc member_package không phải luồng PENDING (vd SCHEDULED) → bỏ qua.
            return toResponse(mp);
        }

        LocalDate today = LocalDate.now(VN_ZONE);
        mp.setStatus(MembershipStatus.ACTIVE);
        mp.setStartDate(today); // GHI ĐÈ ngày dự kiến bằng ngày thanh toán thành công thật
        mp.setEndDate(computeEndDate(mp.getGymPackage(), today));
        mp.setRemainingSessions(mp.getGymPackage().getSessionCount());
        return toResponse(memberPackageRepository.save(mp));
    }

    // ============================================================
    // Trừ buổi tập — gọi bởi BookingSettledMembershipListener khi booking được chốt
    // (COMPLETED / MEMBER_NO_SHOW / LATE_CANCEL). Xem Javadoc ở MembershipService.
    // ============================================================

    @Override
    @Transactional
    public ConsumeResult tryConsumeSession(UUID userId) {
        Optional<MemberPackage> current = getCurrentMembership(userId);
        if (current.isEmpty()) {
            return ConsumeResult.NO_ACTIVE_MEMBERSHIP;
        }
        MemberPackage mp = current.get();
        if (mp.getRemainingSessions() == null) {
            return ConsumeResult.UNLIMITED; // gói không giới hạn buổi (vd TIME_BASED không kèm session_count)
        }
        if (mp.getRemainingSessions() <= 0) {
            return ConsumeResult.NO_SESSION_LEFT; // không trừ âm
        }
        mp.setRemainingSessions(mp.getRemainingSessions() - 1);
        memberPackageRepository.save(mp);
        return ConsumeResult.CONSUMED;
    }

    @Override
    @Transactional
    public void consumeSession(UUID userId) {
        ConsumeResult result = tryConsumeSession(userId);
        if (result == ConsumeResult.NO_ACTIVE_MEMBERSHIP) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND);
        }
        if (result == ConsumeResult.NO_SESSION_LEFT) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_NO_SESSION);
        }
    }

    // ============================================================
    // Helpers nội bộ
    // ============================================================

    private Package requireActivePackage(UUID packageId) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PACKAGE_NOT_FOUND));
        if (!pkg.isActive()) {
            throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND, "Gói tập hiện không còn mở bán");
        }
        return pkg;
    }

    private MemberPackage requireActiveMemberPackage(UUID memberPackageId) {
        MemberPackage mp = memberPackageRepository.findById(memberPackageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
        if (!MembershipStatus.ACTIVE.equals(mp.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ có thể nâng cấp/hạ cấp từ gói đang ở trạng thái ACTIVE");
        }
        return mp;
    }

    private void requireOwnershipOrStaff(MemberPackage mp, UUID actingUserId, boolean staffOverride) {
        if (staffOverride) {
            return; // lễ tân/admin được thao tác cho bất kỳ hội viên nào
        }
        if (!mp.getUser().getId().equals(actingUserId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "Bạn không có quyền thao tác trên gói tập của người khác");
        }
    }

    private void requireNoPendingPackage(UUID userId) {
        boolean hasPending = memberPackageRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, MembershipStatus.PENDING)
                .isPresent();
        if (hasPending) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PENDING_EXISTS);
        }
    }

    private Branch resolveBranch(UUID branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy chi nhánh"));
    }

    private LocalDate computeEndDate(Package pkg, LocalDate start) {
        return pkg.getDurationDays() != null ? start.plusDays(pkg.getDurationDays()) : null;
    }

    private MemberPackageResponse toResponse(MemberPackage mp) {
        return MemberPackageResponse.builder()
                .id(mp.getId())
                .userId(mp.getUser().getId())
                .userFullName(mp.getUser().getFullName())
                .packageId(mp.getGymPackage().getId())
                .packageName(mp.getGymPackage().getName())
                .branchId(mp.getBranch() != null ? mp.getBranch().getId() : null)
                .status(mp.getStatus())
                .startDate(mp.getStartDate())
                .endDate(mp.getEndDate())
                .remainingSessions(mp.getRemainingSessions())
                .build();
    }
}
