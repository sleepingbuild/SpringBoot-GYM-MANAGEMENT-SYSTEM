package com.gym.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agent 1 - SHARED FILE (định nghĩa cùng lúc với bảng branches trong V1).
 */
@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
public class Branch extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, CLOSED
}
