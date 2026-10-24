package com.gym.management.service;

import com.gym.management.constant.MembershipStatus;
import com.gym.management.dto.response.PaymentTransactionResponse;
import com.gym.management.entity.MemberPackage;
import com.gym.management.entity.Package;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.BranchRepository;
import com.gym.management.repository.MemberPackageRepository;
import com.gym.management.repository.PackageRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.impl.MembershipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Test cơ bản cho state machine Membership — theo yêu cầu CONTRIBUTING.md mục 6
 * ("tối thiểu 1 test cho mỗi nhánh logic quan trọng") và ISSUES.md 2.3/2.4/2.5/2.6.
 *
 * LƯU Ý: PaymentService bị mock nên các test này KHÔNG cover luồng tạo/confirm payment
 * thật (đã tách trách nhiệm) — chỉ cover đúng phần state machine của MembershipService.
 */
@ExtendWith(MockitoExtension.class)
class MembershipServiceImplTest {

    @Mock
    private MemberPackageRepository memberPackageRepository;
    @Mock
    private PackageRepository packageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private PaymentService paymentService;

    private MembershipServiceImpl membershipService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        membershipService = new MembershipServiceImpl(
                memberPackageRepository, packageRepository, userRepository, branchRepository, paymentService);

        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setFullName("Nguyễn Văn A");

        lenient().when(memberPackageRepository.save(any(MemberPackage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getCurrentMembership_shouldLazyExpire_activePackagePastEndDate() {
        MemberPackage expiredButStillMarkedActive = new MemberPackage();
        expiredButStillMarkedActive.setId(UUID.randomUUID());
        expiredButStillMarkedActive.setUser(user);
        expiredButStillMarkedActive.setStatus(MembershipStatus.ACTIVE);
        expiredButStillMarkedActive.setStartDate(LocalDate.now().minusDays(40));
        expiredButStillMarkedActive.setEndDate(LocalDate.now().minusDays(5)); // đã quá hạn

        when(memberPackageRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(expiredButStillMarkedActive));
        when(memberPackageRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty()); // sau lazy-check, không còn bản ghi ACTIVE nào

        Optional<MemberPackage> result = membershipService.getCurrentMembership(userId);

        assertThat(result).isEmpty();
        assertThat(expiredButStillMarkedActive.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
    }

    @Test
    void getCurrentMembership_shouldLazyActivate_scheduledPackageReachedStartDate() {
        MemberPackage scheduled = new MemberPackage();
        scheduled.setId(UUID.randomUUID());
        scheduled.setUser(user);
        scheduled.setStatus(MembershipStatus.SCHEDULED);
        scheduled.setStartDate(LocalDate.now().minusDays(1)); // đã tới ngày (hoặc qua)

        when(memberPackageRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(scheduled));
        when(memberPackageRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(scheduled));

        Optional<MemberPackage> result = membershipService.getCurrentMembership(userId);

        assertThat(result).isPresent();
        assertThat(scheduled.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void registerNewPackage_shouldThrow_whenPendingAlreadyExists() {
        UUID packageId = UUID.randomUUID();
        Package pkg = activePackage(packageId, "500K", new BigDecimal("500000"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(pkg));
        when(memberPackageRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, MembershipStatus.PENDING))
                .thenReturn(Optional.of(new MemberPackage())); // đang có 1 bản ghi PENDING

        assertThatThrownBy(() -> membershipService.registerNewPackage(userId, packageId, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBERSHIP_PENDING_EXISTS);
    }

    @Test
    void downgradePackage_shouldKeepCurrentActive_andScheduleNewOneAtCurrentEndDate() {
        UUID currentMpId = UUID.randomUUID();
        UUID newPackageId = UUID.randomUUID();

        Package currentPkg = activePackage(UUID.randomUUID(), "Gói cao cấp", new BigDecimal("1000000"));
        Package cheaperPkg = activePackage(newPackageId, "Gói tiết kiệm", new BigDecimal("400000"));
        cheaperPkg.setDurationDays(30);

        MemberPackage current = new MemberPackage();
        current.setId(currentMpId);
        current.setUser(user);
        current.setGymPackage(currentPkg);
        current.setStatus(MembershipStatus.ACTIVE);
        current.setStartDate(LocalDate.now().minusDays(10));
        current.setEndDate(LocalDate.now().plusDays(20));

        when(memberPackageRepository.findById(currentMpId)).thenReturn(Optional.of(current));
        when(packageRepository.findById(newPackageId)).thenReturn(Optional.of(cheaperPkg));
        when(paymentService.createPendingTransaction(any(), any(), any(), any()))
                .thenReturn(PaymentTransactionResponse.builder().id(UUID.randomUUID()).build());
        when(paymentService.confirmPayment(any(), any()))
                .thenReturn(PaymentTransactionResponse.builder().id(UUID.randomUUID()).build());

        var result = membershipService.downgradePackage(currentMpId, newPackageId, null, userId, false);

        // Gói cũ GIỮ NGUYÊN trạng thái ACTIVE — hạ cấp không đụng vào gói đang chạy
        assertThat(current.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        // Gói mới ở trạng thái SCHEDULED, bắt đầu NGÀY SAU khi gói cũ hết hạn (không chồng 2 gói ACTIVE)
        assertThat(result.getMemberPackage().getStatus()).isEqualTo(MembershipStatus.SCHEDULED);
        assertThat(result.getMemberPackage().getStartDate()).isEqualTo(current.getEndDate().plusDays(1));
        assertThat(result.getMemberPackage().getEndDate())
                .isEqualTo(result.getMemberPackage().getStartDate().plusDays(30));
    }

    @Test
    void upgradePackage_shouldCancelOldImmediately_andCreateNewPending() {
        UUID currentMpId = UUID.randomUUID();
        UUID newPackageId = UUID.randomUUID();

        Package currentPkg = activePackage(UUID.randomUUID(), "Gói cơ bản", new BigDecimal("400000"));
        Package higherPkg = activePackage(newPackageId, "Gói cao cấp", new BigDecimal("900000"));

        MemberPackage current = new MemberPackage();
        current.setId(currentMpId);
        current.setUser(user);
        current.setGymPackage(currentPkg);
        current.setStatus(MembershipStatus.ACTIVE);
        current.setStartDate(LocalDate.now().minusDays(5));
        current.setEndDate(LocalDate.now().plusDays(25));

        when(memberPackageRepository.findById(currentMpId)).thenReturn(Optional.of(current));
        when(packageRepository.findById(newPackageId)).thenReturn(Optional.of(higherPkg));
        when(memberPackageRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, MembershipStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentService.createPendingTransaction(any(), any(), any(), any()))
                .thenReturn(PaymentTransactionResponse.builder().id(UUID.randomUUID()).build());

        var result = membershipService.upgradePackage(currentMpId, newPackageId, null, userId, false);

        // Gói cũ phải bị CANCELLED NGAY LẬP TỨC, không đợi thanh toán
        assertThat(current.getStatus()).isEqualTo(MembershipStatus.CANCELLED);
        // Gói mới ở trạng thái PENDING, chờ xác nhận thanh toán
        assertThat(result.getMemberPackage().getStatus()).isEqualTo(MembershipStatus.PENDING);
    }

    @Test
    void upgradePackage_shouldReject_whenNewPackageIsCheaper() {
        UUID currentMpId = UUID.randomUUID();
        UUID newPackageId = UUID.randomUUID();

        Package currentPkg = activePackage(UUID.randomUUID(), "Gói cao cấp", new BigDecimal("900000"));
        Package cheaperPkg = activePackage(newPackageId, "Gói cơ bản", new BigDecimal("400000"));

        MemberPackage current = new MemberPackage();
        current.setId(currentMpId);
        current.setUser(user);
        current.setGymPackage(currentPkg);
        current.setStatus(MembershipStatus.ACTIVE);
        current.setEndDate(LocalDate.now().plusDays(10));

        when(memberPackageRepository.findById(currentMpId)).thenReturn(Optional.of(current));
        when(packageRepository.findById(newPackageId)).thenReturn(Optional.of(cheaperPkg));

        // dùng nhầm upgrade cho 1 gói rẻ hơn -> phải bị chặn, hướng dẫn dùng downgrade
        assertThatThrownBy(() -> membershipService.upgradePackage(currentMpId, newPackageId, null, userId, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        // Gói cũ KHÔNG được đụng vào khi validate thất bại
        assertThat(current.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void upgradePackage_shouldReject_whenActingUserIsNotOwnerAndNotStaff() {
        UUID currentMpId = UUID.randomUUID();
        UUID newPackageId = UUID.randomUUID();
        UUID someoneElseId = UUID.randomUUID();

        Package currentPkg = activePackage(UUID.randomUUID(), "Gói cơ bản", new BigDecimal("400000"));

        MemberPackage current = new MemberPackage();
        current.setId(currentMpId);
        current.setUser(user); // chủ sở hữu thật là `user` (userId), không phải someoneElseId
        current.setGymPackage(currentPkg);
        current.setStatus(MembershipStatus.ACTIVE);
        current.setEndDate(LocalDate.now().plusDays(10));

        when(memberPackageRepository.findById(currentMpId)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> membershipService.upgradePackage(currentMpId, newPackageId, null, someoneElseId, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN_ROLE);
    }

    // ===== Ranh giới ngày hạ cấp: không bao giờ có 2 gói ACTIVE cùng lúc =====

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private MemberPackage membership(String status, LocalDate start, LocalDate end, Integer remaining) {
        MemberPackage mp = new MemberPackage();
        mp.setId(UUID.randomUUID());
        mp.setUser(user);
        mp.setStatus(status);
        mp.setStartDate(start);
        mp.setEndDate(end);
        mp.setRemainingSessions(remaining);
        return mp;
    }

    @Test
    void getCurrentMembership_onLastDayOfOldPackage_shouldKeepOldActive_andScheduledStillWaiting() {
        LocalDate today = LocalDate.now(VN_ZONE);
        MemberPackage old = membership(MembershipStatus.ACTIVE, today.minusDays(29), today, 5); // hôm nay là ngày CUỐI
        MemberPackage next = membership(MembershipStatus.SCHEDULED, today.plusDays(1), today.plusDays(31), 10);

        when(memberPackageRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(next, old));
        when(memberPackageRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(old));

        membershipService.getCurrentMembership(userId);

        assertThat(old.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(next.getStatus()).isEqualTo(MembershipStatus.SCHEDULED); // chưa được lên ACTIVE sớm 1 ngày
    }

    @Test
    void getCurrentMembership_dayAfterOldPackageEnds_shouldExpireOld_andActivateScheduled() {
        LocalDate today = LocalDate.now(VN_ZONE);
        MemberPackage old = membership(MembershipStatus.ACTIVE, today.minusDays(30), today.minusDays(1), 5);
        MemberPackage next = membership(MembershipStatus.SCHEDULED, today, today.plusDays(30), 10);

        when(memberPackageRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(next, old));
        when(memberPackageRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(next));

        membershipService.getCurrentMembership(userId);

        assertThat(old.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
        assertThat(next.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    // ===== tryConsumeSession / consumeSession =====

    private void stubCurrentMembership(MemberPackage active) {
        when(memberPackageRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(active == null ? List.of() : List.of(active));
        when(memberPackageRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.ofNullable(active));
    }

    @Test
    void tryConsumeSession_shouldDecrementRemainingSessions() {
        LocalDate today = LocalDate.now(VN_ZONE);
        MemberPackage active = membership(MembershipStatus.ACTIVE, today.minusDays(5), today.plusDays(25), 3);
        stubCurrentMembership(active);

        MembershipService.ConsumeResult result = membershipService.tryConsumeSession(userId);

        assertThat(result).isEqualTo(MembershipService.ConsumeResult.CONSUMED);
        assertThat(active.getRemainingSessions()).isEqualTo(2);
    }

    @Test
    void tryConsumeSession_shouldReturnUnlimited_andNotTouchPackage_whenRemainingIsNull() {
        LocalDate today = LocalDate.now(VN_ZONE);
        MemberPackage active = membership(MembershipStatus.ACTIVE, today.minusDays(5), today.plusDays(25), null);
        stubCurrentMembership(active);

        assertThat(membershipService.tryConsumeSession(userId)).isEqualTo(MembershipService.ConsumeResult.UNLIMITED);
        assertThat(active.getRemainingSessions()).isNull();
    }

    @Test
    void tryConsumeSession_shouldNotGoNegative_whenNoSessionLeft() {
        LocalDate today = LocalDate.now(VN_ZONE);
        MemberPackage active = membership(MembershipStatus.ACTIVE, today.minusDays(5), today.plusDays(25), 0);
        stubCurrentMembership(active);

        assertThat(membershipService.tryConsumeSession(userId)).isEqualTo(MembershipService.ConsumeResult.NO_SESSION_LEFT);
        assertThat(active.getRemainingSessions()).isEqualTo(0); // không trừ âm
    }

    @Test
    void tryConsumeSession_shouldNotThrow_whenNoActiveMembership() {
        stubCurrentMembership(null);

        assertThat(membershipService.tryConsumeSession(userId)).isEqualTo(MembershipService.ConsumeResult.NO_ACTIVE_MEMBERSHIP);
    }

    @Test
    void consumeSession_shouldThrowNoSession_whenNoSessionLeft() {
        LocalDate today = LocalDate.now(VN_ZONE);
        stubCurrentMembership(membership(MembershipStatus.ACTIVE, today.minusDays(5), today.plusDays(25), 0));

        assertThatThrownBy(() -> membershipService.consumeSession(userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBERSHIP_NO_SESSION);
    }

    @Test
    void consumeSession_shouldThrowNotFound_whenNoActiveMembership() {
        stubCurrentMembership(null);

        assertThatThrownBy(() -> membershipService.consumeSession(userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBERSHIP_NOT_FOUND);
    }

    private Package activePackage(UUID id, String name, BigDecimal price) {
        Package pkg = new Package();
        pkg.setId(id);
        pkg.setName(name);
        pkg.setPrice(price);
        pkg.setPackageType("TIME_BASED");
        pkg.setPeakType("FULL_TIME");
        pkg.setActive(true);
        return pkg;
    }
}
