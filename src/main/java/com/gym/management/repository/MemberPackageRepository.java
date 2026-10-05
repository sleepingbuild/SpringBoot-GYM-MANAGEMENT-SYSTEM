package com.gym.management.repository;

import com.gym.management.entity.MemberPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberPackageRepository extends JpaRepository<MemberPackage, UUID> {

    /**
     * Dùng bởi {@code MembershipService.getCurrentMembership} — 1 nguồn chân lý duy nhất
     * cho "gói hiện tại" (status = ACTIVE). Về lý thuyết chỉ có tối đa 1 bản ghi ACTIVE/user
     * tại 1 thời điểm; lấy bản mới nhất để an toàn nếu có dữ liệu lệch.
     */
    Optional<MemberPackage> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    List<MemberPackage> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<MemberPackage> findByUserId(UUID userId, Pageable pageable);

    // Dự phòng cho tác vụ quét hàng loạt (báo cáo, job thủ công) — luồng chính vẫn dùng lazy-check khi đọc.
    List<MemberPackage> findByStatusAndEndDateBefore(String status, LocalDate date);

    List<MemberPackage> findByStatusAndStartDateLessThanEqual(String status, LocalDate date);
}
