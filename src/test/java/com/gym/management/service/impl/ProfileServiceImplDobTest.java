package com.gym.management.service.impl;

import com.gym.management.dto.request.UpdateProfileRequest;
import com.gym.management.dto.response.ProfileResponse;
import com.gym.management.entity.User;
import com.gym.management.entity.UserProfile;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.UserProfileRepository;
import com.gym.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplDobTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserRepository userRepository;

    private ProfileServiceImpl service;
    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        // "Hôm nay" cố định: 2026-10-25 (giờ Việt Nam)
        Clock clock = Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), VN);
        service = new ProfileServiceImpl(userProfileRepository, userRepository, clock);

        user = new User();
        // Tài khoản đăng ký ngày 2026-10-01
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 10, 1, 10, 0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private UpdateProfileRequest requestWith(LocalDate dob) {
        UpdateProfileRequest r = new UpdateProfileRequest();
        r.setDateOfBirth(dob);
        return r;
    }

    @Test
    void exactly18OnRegistrationDay_isAcceptedAndAgeIsComputed() {
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileResponse res = service.updateMyProfile(userId, requestWith(LocalDate.of(2008, 10, 1)));

        assertEquals(LocalDate.of(2008, 10, 1), res.getDateOfBirth());
        assertEquals(18, res.getAge());
    }

    @Test
    void oneDayShortOf18OnRegistrationDay_isRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMyProfile(userId, requestWith(LocalDate.of(2008, 10, 2))));

        assertEquals(ErrorCode.PROFILE_INVALID_AGE, ex.getErrorCode());
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void futureDateOfBirth_isRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMyProfile(userId, requestWith(LocalDate.of(2030, 1, 1))));

        assertEquals(ErrorCode.PROFILE_INVALID_AGE, ex.getErrorCode());
    }

    @Test
    void over120YearsOld_isRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMyProfile(userId, requestWith(LocalDate.of(1890, 1, 1))));

        assertEquals(ErrorCode.PROFILE_INVALID_AGE, ex.getErrorCode());
    }

    @Test
    void nullDateOfBirth_isAcceptedAndAgeIsNull() {
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileResponse res = service.updateMyProfile(userId, requestWith(null));

        assertNull(res.getDateOfBirth());
        assertNull(res.getAge());
    }
}
