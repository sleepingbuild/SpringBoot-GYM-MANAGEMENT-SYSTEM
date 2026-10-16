package com.gym.management.entity;

import com.gym.management.constant.LeadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Agent 6 - Khách tiềm năng. Tham chiếu user qua UUID (không @ManyToOne User) để module
 * Leads không phụ thuộc entity của module khác.
 */
@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
public class Lead extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadStatus status = LeadStatus.NEW;

    @Column(name = "assigned_sales_id")
    private UUID assignedSalesId;

    @Column(name = "converted_user_id")
    private UUID convertedUserId;

    @Column(length = 500)
    private String notes;
}
