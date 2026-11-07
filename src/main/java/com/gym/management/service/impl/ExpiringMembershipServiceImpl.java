package com.gym.management.service.impl;

import com.gym.management.dto.response.ExpiringMemberResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.ExpiringMemberRepository;
import com.gym.management.service.ExpiringMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpiringMembershipServiceImpl implements ExpiringMembershipService {

    private final ExpiringMemberRepository repository;

    /** Bean Clock dùng chung của dự án (Asia/Ho_Chi_Minh), để "hôm nay" khớp mọi module. */
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<ExpiringMemberResponse> findExpiringSoon(int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Tham số days phải từ 1 đến " + MAX_DAYS);
        }
        LocalDate today = LocalDate.now(clock);
        return repository.findExpiringBetween(today, today.plusDays(days)).stream()
                .map(r -> new ExpiringMemberResponse(r.userId(), r.fullName(), r.phone(), r.email(),
                        r.packageName(), r.endDate(), ChronoUnit.DAYS.between(today, r.endDate()),
                        r.remainingSessions()))
                .toList();
    }
}
