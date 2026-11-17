package com.gym.management.controller;

import com.gym.management.dto.request.SelfPurchaseRequest;
import com.gym.management.exception.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hội viên tự mua gói: chốt các điều kiện an toàn của endpoint mà không cần dựng Spring context.
 * Logic mua và thanh toán đã có test ở MemberSelfPurchaseServiceImplTest.
 */
class MemberSelfPurchaseControllerTest {

    @Test
    void purchase_shouldBeRestrictedToMembersOnly() throws NoSuchMethodException {
        Method purchase = MemberSelfPurchaseController.class.getMethod("purchase", SelfPurchaseRequest.class);

        assertThat(purchase.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('MEMBER')");
    }

    @Test
    void request_shouldNotAcceptAUserId_soTheBuyerIsAlwaysTheLoggedInMember() {
        assertThatThrownBy(() -> SelfPurchaseRequest.class.getDeclaredField("userId"))
                .isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    void request_withoutPackageId_shouldFailValidation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Set<ConstraintViolation<SelfPurchaseRequest>> violations = validator.validate(new SelfPurchaseRequest());

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("packageId");
    }

    @Test
    void request_withPackageIdOnly_shouldBeValid() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        SelfPurchaseRequest request = new SelfPurchaseRequest();
        request.setPackageId(UUID.randomUUID());

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.getBranchId()).isNull();
    }

    @Test
    void purchase_shouldReturnTypedApiResponse() throws NoSuchMethodException {
        Method purchase = MemberSelfPurchaseController.class.getMethod("purchase", SelfPurchaseRequest.class);

        assertThat(purchase.getReturnType()).isEqualTo(ApiResponse.class);
    }
}
