package com.gym.management.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Agent 1 - SHARED FILE.
 * Đại diện cho mọi tài khoản trong hệ thống (Admin, Lễ tân, PT, Sales, Hội viên).
 * Vai trò cụ thể được xác định qua bảng user_roles (many-to-many).
 *
 * Các agent khác (2-6) chỉ nên tham chiếu User qua service interface
 * (vd: UserService.getById), KHÔNG tự ý thêm cột nghiệp vụ riêng vào entity này.
 * Nếu cần thông tin đặc thù (vd: hồ sơ PT: commission_rate, specialization)
 * → tạo entity riêng (TrainerProfile) liên kết 1-1 với User (xem Agent 5).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, BANNED

    @Column(name = "avatar_url")
    private String avatarUrl;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
