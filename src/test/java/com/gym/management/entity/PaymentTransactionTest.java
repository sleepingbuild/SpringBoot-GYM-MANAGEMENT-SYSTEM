package com.gym.management.entity;

import com.gym.management.constant.RelatedType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V7 (CH1): cặp relatedType/relatedId được lưu bằng 2 cột khoá ngoại; bảo đảm luôn "đúng một trong hai".
 */
class PaymentTransactionTest {

    @Test
    void newTransaction_hasNoRelatedYet() {
        PaymentTransaction tx = new PaymentTransaction();

        assertThat(tx.getRelatedType()).isNull();
        assertThat(tx.getRelatedId()).isNull();
    }

    @Test
    void setRelated_membership_setsOnlyMemberPackageId() {
        PaymentTransaction tx = new PaymentTransaction();
        UUID memberPackageId = UUID.randomUUID();

        tx.setRelated(RelatedType.MEMBERSHIP, memberPackageId);

        assertThat(tx.getMemberPackageId()).isEqualTo(memberPackageId);
        assertThat(tx.getPosOrderId()).isNull();
        assertThat(tx.getRelatedType()).isEqualTo(RelatedType.MEMBERSHIP);
        assertThat(tx.getRelatedId()).isEqualTo(memberPackageId);
    }

    @Test
    void setRelated_pos_setsOnlyPosOrderId() {
        PaymentTransaction tx = new PaymentTransaction();
        UUID posOrderId = UUID.randomUUID();

        tx.setRelated(RelatedType.POS, posOrderId);

        assertThat(tx.getPosOrderId()).isEqualTo(posOrderId);
        assertThat(tx.getMemberPackageId()).isNull();
        assertThat(tx.getRelatedType()).isEqualTo(RelatedType.POS);
        assertThat(tx.getRelatedId()).isEqualTo(posOrderId);
    }

    @Test
    void setRelated_switchingType_clearsThePreviousColumn() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setRelated(RelatedType.MEMBERSHIP, UUID.randomUUID());
        UUID posOrderId = UUID.randomUUID();

        tx.setRelated(RelatedType.POS, posOrderId);

        assertThat(tx.getMemberPackageId()).isNull();
        assertThat(tx.getPosOrderId()).isEqualTo(posOrderId);
    }

    @Test
    void setRelated_unknownType_throws() {
        PaymentTransaction tx = new PaymentTransaction();

        assertThatThrownBy(() -> tx.setRelated("OTHER", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
