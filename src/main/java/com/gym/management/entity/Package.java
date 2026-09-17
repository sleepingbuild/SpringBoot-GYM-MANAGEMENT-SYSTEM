package com.gym.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Agent 2 - Membership & Package Module.
 * Lưu ý: đây là bản TỐI THIỂU để CheckInService (Agent 3) hoạt động được.
 * Agent 2 khi làm đầy đủ nghiệp vụ (freeze, upgrade...) mở rộng thêm ở đây,
 * không tạo entity trùng lặp mới.
 */
@Entity
@Table(name = "packages")
@Getter
@Setter
@NoArgsConstructor
public class Package extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "session_count")
    private Integer sessionCount;

    @Column(name = "package_type", nullable = false, length = 20)
    private String packageType; // TIME_BASED, SESSION_BASED, PT_1ON1

    @Column(name = "peak_type", nullable = false, length = 20)
    private String peakType = "FULL_TIME"; // OFF_PEAK, FULL_TIME

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
