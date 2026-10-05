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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;


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
        // Gói mới ở trạng thái SCHEDULED, bắt đầu đúng ngày gói cũ hết hạn
        assertThat(result.getMemberPackage().getStatus()).isEqualTo(MembershipStatus.SCHEDULED);
        assertThat(result.getMemberPackage().getStartDate()).isEqualTo(current.getEndDate());
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
