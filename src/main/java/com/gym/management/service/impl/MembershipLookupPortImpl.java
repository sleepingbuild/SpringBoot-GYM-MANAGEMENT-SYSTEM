package com.gym.management.service.impl;

import com.gym.management.entity.MemberPackage;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.service.MembershipLookupPort;
import com.gym.management.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ⚠️ SỬA LẦN 3 (2026-09-30, đã xác minh bằng source thật):
 * - Lần 1: `getCurrentMembership()` trả `Optional<MemberPackage>` chứ không phải object trần —
 *   đã sửa bằng `.orElseThrow(...)`.
 * - Lần 2: field liên kết sang `Package` trên `MemberPackage` KHÔNG tên `package` (đúng như dự
 *   đoán — `package` là từ khoá dành riêng của Java) mà tên là `gymPackage`
 *   (`@JoinColumn(name = "package_id")`, xem `MemberPackage.java` dòng 27-29) → getter Lombok
 *   thật là `getGymPackage()`, đã sửa lại đúng tên bên dưới.
 *
 * ⚠️ CÒN 1 CHỖ CHƯA XÁC MINH: `getMaxSessionsPerWeek()` gọi trên entity `Package` (trả về từ
 * `getGymPackage()`) — đây là tên field theo đúng `DATABASE_SCHEMA.md` (cột
 * `packages.max_sessions_per_week`) nhưng chưa thấy source thật `Package.java` của Agent 2 nên
 * chưa chắc 100% khớp tên Java field/getter. Nếu build lần tới lại lỗi đúng dòng này, làm y hệt
 * quy trình vừa rồi: chạy `Select-String -Path "...\entity\Package.java" -Pattern "max|Session"`
 * và báo lại tên thật.
 */
@Component
@RequiredArgsConstructor
public class MembershipLookupPortImpl implements MembershipLookupPort {

    private final MembershipService membershipService;

    @Override
    public Integer resolveMaxSessionsPerWeek(UUID memberId) {
        MemberPackage currentMembership = membershipService.getCurrentMembership(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND,
                        "Hội viên chưa có gói tập đang hoạt động, không thể đặt lịch PT"));
        return currentMembership.getGymPackage().getMaxSessionsPerWeek();
    }
}
