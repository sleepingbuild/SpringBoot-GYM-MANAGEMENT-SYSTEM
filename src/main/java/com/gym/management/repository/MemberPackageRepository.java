package com.gym.management.repository;

import com.gym.management.entity.MemberPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberPackageRepository extends JpaRepository<MemberPackage, UUID> {

    /**
     * Lấy các gói ACTIVE của 1 user, mới nhất trước — dùng để xác định
     * "gói hiện tại" (getCurrentMembership, xem REQUIREMENTS.md mục 2) và để
     * BookingService kiểm tra giới hạn buổi/tuần khi đặt lịch.
     */
    List<MemberPackage> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);
}
