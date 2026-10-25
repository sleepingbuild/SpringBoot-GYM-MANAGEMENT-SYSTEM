package com.gym.management.service;

import com.gym.management.constant.PaymentStatus;
import com.gym.management.constant.RelatedType;
import com.gym.management.entity.PaymentTransaction;
import com.gym.management.exception.BusinessException;
import com.gym.management.repository.BranchRepository;
import com.gym.management.repository.MemberPackageRepository;
import com.gym.management.repository.PackageRepository;
import com.gym.management.repository.PaymentTransactionRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.impl.MembershipServiceImpl;
import com.gym.management.service.impl.PaymentServiceImpl;
import com.gym.management.service.listener.MembershipPaymentListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Chặn tái phát lỗi "master không khởi động được" do vòng phụ thuộc bean MembershipServiceImpl &lt;-&gt;
 * PaymentServiceImpl (Jenkins báo). Các test Mockito thuần KHÔNG bắt được lỗi này vì không dựng Spring context.
 *
 * Test dựng context thật chỉ với đúng các bean của module (repository là mock, không cần DB) và cấm vòng
 * phụ thuộc giống Spring Boot (allowCircularReferences = false). Nếu ai đó lại cho PaymentServiceImpl inject
 * MembershipService (hoặc ngược lại tạo vòng khác), refresh() sẽ ném lỗi và test này đỏ.
 */
class MembershipPaymentWiringTest {

    private AnnotationConfigApplicationContext ctx;

    private AnnotationConfigApplicationContext newContext() {
        AnnotationConfigApplicationContext c = new AnnotationConfigApplicationContext();
        c.setAllowCircularReferences(false); // mặc định của Spring Boot
        c.registerBean(MemberPackageRepository.class, () -> mock(MemberPackageRepository.class));
        c.registerBean(PackageRepository.class, () -> mock(PackageRepository.class));
        c.registerBean(UserRepository.class, () -> mock(UserRepository.class));
        c.registerBean(BranchRepository.class, () -> mock(BranchRepository.class));
        c.registerBean(PaymentTransactionRepository.class, () -> mock(PaymentTransactionRepository.class));
        c.register(MembershipServiceImpl.class, PaymentServiceImpl.class, MembershipPaymentListener.class);
        return c;
    }

    @AfterEach
    void tearDown() {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    @DisplayName("Context khởi động được khi cấm vòng phụ thuộc (không còn Membership <-> Payment)")
    void contextStartsWithoutCircularDependency() {
        ctx = newContext();

        assertThatCode(ctx::refresh).doesNotThrowAnyException();

        assertThat(ctx.getBean(MembershipService.class)).isNotNull();
        assertThat(ctx.getBean(PaymentService.class)).isNotNull();
    }

    @Test
    @DisplayName("confirmPayment MEMBERSHIP: sự kiện tới được Membership (tìm member_package để kích hoạt)")
    void confirmMembershipPayment_reachesMembershipViaEvent() {
        ctx = newContext();
        ctx.refresh();

        PaymentTransactionRepository txRepo = ctx.getBean(PaymentTransactionRepository.class);
        MemberPackageRepository mpRepo = ctx.getBean(MemberPackageRepository.class);

        UUID txId = UUID.randomUUID();
        UUID memberPackageId = UUID.randomUUID();

        PaymentTransaction tx = new PaymentTransaction();
        tx.setRelatedType(RelatedType.MEMBERSHIP);
        tx.setRelatedId(memberPackageId);
        tx.setStatus(PaymentStatus.PENDING);

        when(txRepo.findById(txId)).thenReturn(Optional.of(tx));
        when(txRepo.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        // Không có member_package → activateFromPendingPayment ném MEMBERSHIP_NOT_FOUND. Lỗi này chứng minh
        // sự kiện đã đi đúng đường Payment → listener → Membership, và (đồng bộ) lỗi đi ngược ra confirmPayment
        // để rollback chung transaction, y như khi Payment gọi thẳng trước đây.
        when(mpRepo.findById(memberPackageId)).thenReturn(Optional.empty());

        PaymentService paymentService = ctx.getBean(PaymentService.class);

        assertThatThrownBy(() -> paymentService.confirmPayment(txId, null))
                .isInstanceOf(BusinessException.class);
        verify(mpRepo).findById(memberPackageId);
    }
}
