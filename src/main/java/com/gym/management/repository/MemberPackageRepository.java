package com.gym.management.repository;

import com.gym.management.entity.MemberPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Trừ 1 buổi NGUYÊN TỬ trong DB (tránh lỗi mất cập nhật khi hai booking của cùng một
     * hội viên được chốt đồng thời: đọc → trừ trong bộ nhớ → save làm cả hai giao dịch cùng
     * ghi "n - 1").
     *
     * <p>Điều kiện {@code remainingSessions > 0} và {@code status = ACTIVE} nằm ngay trong câu
     * UPDATE nên quyết định "còn buổi hay không" thuộc về DB, không thể trừ âm.
     *
     * <p>{@code flushAutomatically = true}: ghi các thay đổi đang chờ (vd lazy-check đổi status)
     * trước khi chạy câu UPDATE. KHÔNG dùng {@code clearAutomatically}: nó sẽ tách các entity
     * đang quản lý (vd {@code Booking} của Agent 3) khỏi phiên làm việc trong cùng giao dịch.
     *
     * @return 1 nếu đã trừ được; 0 nếu hết buổi, gói không còn ACTIVE hoặc gói không giới hạn buổi
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE MemberPackage m SET m.remainingSessions = m.remainingSessions - 1 "
            + "WHERE m.id = :id AND m.status = :active AND m.remainingSessions > 0")
    int consumeOneSession(@Param("id") UUID id, @Param("active") String active);
}
