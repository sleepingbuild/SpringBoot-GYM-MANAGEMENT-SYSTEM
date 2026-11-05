package com.gym.management.dto;

import com.gym.management.dto.request.PosOrderCreateRequest;
import com.gym.management.dto.request.ProductRequest;
import com.gym.management.dto.request.TrainerPayRateRequest;
import com.gym.management.entity.ProductCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chặn dữ liệu xấu đi thẳng vào DB (trước đây tồn kho âm, tên quá 150 ký tự, giá tràn DECIMAL(12,2)
 * và paymentMethod = null đều lọt qua DTO và chỉ nổ ở tầng DB hoặc ghi dữ liệu sai).
 */
class PosRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static <T> Set<String> fields(T bean) {
        return validator.validate(bean).stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private ProductRequest product() {
        ProductRequest r = new ProductRequest();
        r.setName("Whey 2lb");
        r.setCategory(ProductCategory.SUPPLEMENT);
        r.setPrice(new BigDecimal("500000"));
        r.setStockQuantity(10);
        r.setUnit("hộp");
        return r;
    }

    private PosOrderCreateRequest order(int quantity) {
        PosOrderCreateRequest r = new PosOrderCreateRequest();
        PosOrderCreateRequest.Item item = new PosOrderCreateRequest.Item();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(quantity);
        r.setItems(List.of(item));
        return r;
    }

    @Test
    void validProduct_hasNoViolations() {
        assertThat(validator.validate(product())).isEmpty();
    }

    @Test
    void product_negativeStockIsRejected() {
        ProductRequest r = product();
        r.setStockQuantity(-1);
        assertThat(fields(r)).containsExactly("stockQuantity");
    }

    @Test
    void product_nullStockIsAllowed_meansKeepOrDefaultZero() {
        ProductRequest r = product();
        r.setStockQuantity(null);
        assertThat(validator.validate(r)).isEmpty();
    }

    @Test
    void product_nameTooLongAndUnitTooLongAreRejected() {
        ProductRequest r = product();
        r.setName("x".repeat(151));
        r.setUnit("y".repeat(21));
        assertThat(fields(r)).containsExactlyInAnyOrder("name", "unit");
    }

    @Test
    void product_priceMustBePositiveAndFitDecimal12_2() {
        ProductRequest zero = product();
        zero.setPrice(BigDecimal.ZERO);
        assertThat(fields(zero)).containsExactly("price");

        ProductRequest tooBig = product();
        tooBig.setPrice(new BigDecimal("12345678901")); // 11 chữ số nguyên
        assertThat(fields(tooBig)).containsExactly("price");

        ProductRequest tooPrecise = product();
        tooPrecise.setPrice(new BigDecimal("10.123"));
        assertThat(fields(tooPrecise)).containsExactly("price");
    }

    @Test
    void validOrder_hasNoViolations() {
        assertThat(validator.validate(order(2))).isEmpty();
    }

    @Test
    void order_paymentMethodNullOrUnknownIsRejected() {
        PosOrderCreateRequest nullMethod = order(1);
        nullMethod.setPaymentMethod(null);
        assertThat(fields(nullMethod)).containsExactly("paymentMethod");

        PosOrderCreateRequest bad = order(1);
        bad.setPaymentMethod("BITCOIN");
        assertThat(fields(bad)).containsExactly("paymentMethod");
    }

    @Test
    void order_quantityMustBeBetween1And1000() {
        assertThat(validator.validate(order(0))).isNotEmpty();
        assertThat(validator.validate(order(-3))).isNotEmpty();
        assertThat(validator.validate(order(1001))).isNotEmpty();
        assertThat(validator.validate(order(1000))).isEmpty();
    }

    @Test
    void order_emptyOrTooManyLinesIsRejected() {
        PosOrderCreateRequest empty = order(1);
        empty.setItems(List.of());
        assertThat(fields(empty)).containsExactly("items");

        PosOrderCreateRequest many = order(1);
        PosOrderCreateRequest.Item item = new PosOrderCreateRequest.Item();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(1);
        many.setItems(java.util.Collections.nCopies(101, item));
        assertThat(fields(many)).containsExactly("items");
    }

    @Test
    void payRate_mustBePositiveAndFitDecimal() {
        TrainerPayRateRequest ok = new TrainerPayRateRequest();
        ok.setAmountPerSession(new BigDecimal("150000"));
        assertThat(validator.validate(ok)).isEmpty();

        TrainerPayRateRequest zero = new TrainerPayRateRequest();
        zero.setAmountPerSession(BigDecimal.ZERO);
        assertThat(validator.validate(zero)).isNotEmpty();

        TrainerPayRateRequest huge = new TrainerPayRateRequest();
        huge.setAmountPerSession(new BigDecimal("99999999999"));
        assertThat(validator.validate(huge)).isNotEmpty();

        assertThat(validator.validate(new TrainerPayRateRequest())).isNotEmpty(); // null
    }
}
