package com.gym.management.repository;

import com.gym.management.entity.MemberPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberPackageRepository extends JpaRepository<MemberPackage, UUID> {

    /**
     * Lấy các gói ACTIVE của 1 user, mới nhất trước — CheckInService duyệt lần lượt
     * cho tới khi tìm được 1 gói hợp lệ (còn hạn/còn buổi/đúng khung giờ).
     */
    List<MemberPackage> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);
}
