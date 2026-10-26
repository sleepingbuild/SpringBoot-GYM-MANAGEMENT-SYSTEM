package com.gym.management.service.impl;

import com.gym.management.entity.GymCheckIn;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.GymCheckInRepository;
import com.gym.management.service.GymCheckInService;
import com.gym.management.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GymCheckInServiceImpl implements GymCheckInService {

    private final GymCheckInRepository gymCheckInRepository;
    private final MembershipService membershipService;

    @Override
    @Transactional
    public ScanResult recordFaceScan(UUID memberId, LocalDateTime now) {
        Optional<GymCheckIn> last = gymCheckInRepository.findFirstByMemberIdOrderByCheckInTimeDesc(memberId);

        // Dòng gần nhất còn mở và thuộc CÙNG NGÀY (theo Clock của caller): lần quét này là
        // check-out. Không kiểm tra gói để hội viên vẫn ra được khi gói vừa hết hạn lúc đang tập.
        // Dòng mở từ ngày trước (quên check-out) thì để nguyên, tạo dòng mới bên dưới.
        if (last.isPresent()
                && last.get().getCheckOutTime() == null
                && last.get().getCheckInTime().toLocalDate().equals(now.toLocalDate())) {
            GymCheckIn open = last.get();
            open.setCheckOutTime(now);
            gymCheckInRepository.save(open);
            return new ScanResult(ACTION_CHECK_OUT, open.getId());
        }

        requireActiveMembership(memberId);

        GymCheckIn checkIn = new GymCheckIn();
        checkIn.setMemberId(memberId);
        checkIn.setCheckInTime(now);
        checkIn.setMethod("FACE");
        GymCheckIn saved = gymCheckInRepository.save(checkIn);
        return new ScanResult(ACTION_CHECK_IN, saved.getId());
    }

    /**
     * Dùng đúng {@link MembershipService#getCurrentMembership} (nguồn chân lý duy nhất về gói ACTIVE).
     * Hàm này đã tự chuyển gói quá hạn sang EXPIRED nên Optional.empty() nghĩa là không còn gói ACTIVE:
     * chưa từng có gói nào thì MEMBERSHIP_NOT_FOUND, đã có gói (nay hết hạn) thì MEMBERSHIP_EXPIRED.
     */
    private void requireActiveMembership(UUID memberId) {
        if (membershipService.getCurrentMembership(memberId).isPresent()) {
            return;
        }
        // getHistory(UUID) đang @Deprecated; dùng tạm vì chỉ cần biết có gói nào hay chưa.
        // TODO: đổi sang getHistory(UUID, Pageable) khi biết accessor của PageResponse.
        @SuppressWarnings("deprecation")
        boolean hasAnyPackage = !membershipService.getHistory(memberId).isEmpty();
        if (hasAnyPackage) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_EXPIRED,
                    "Gói tập của hội viên đã hết hạn");
        }
        throw new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND,
                "Hội viên chưa có gói tập");
    }
}
