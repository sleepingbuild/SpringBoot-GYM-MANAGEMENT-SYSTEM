package com.gym.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PackageRequest {

    @NotBlank(message = "Tên gói không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá gói không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá gói phải lớn hơn 0")
    private BigDecimal price;

    @Positive(message = "Số ngày sử dụng phải lớn hơn 0 nếu có")
    private Integer durationDays; // nullable

    @Positive(message = "Số buổi phải lớn hơn 0 nếu có")
    private Integer sessionCount; // nullable

    @NotBlank
    @Pattern(regexp = "TIME_BASED|SESSION_BASED|PT_1ON1", message = "package_type không hợp lệ")
    private String packageType;

    @NotBlank
    @Pattern(regexp = "OFF_PEAK|FULL_TIME", message = "peak_type không hợp lệ")
    private String peakType;

    /** NULL = không giới hạn, 0 = không cho đặt PT, N = tối đa N buổi/tuần. */
    private Integer maxSessionsPerWeek;

    private Boolean active;
}
