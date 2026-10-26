package com.gym.management.service.impl;

import com.gym.management.dto.request.UpdateProfileRequest;
import com.gym.management.dto.response.ProfileResponse;
import com.gym.management.entity.User;
import com.gym.management.entity.UserProfile;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.UserProfileRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final int MIN_AGE = 18;
    private static final int MAX_AGE = 120;

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    public ProfileResponse getMyProfile(UUID userId) {
        User user = getUser(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        return toResponse(user, profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        validateDateOfBirth(user, request.getDateOfBirth());

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setUser(user);
                    return p;
                });

        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setWeightKg(request.getWeightKg());
        profile.setHeightCm(request.getHeightCm());
        profile.setGoal(request.getGoal());
        userProfileRepository.save(profile);

        return toResponse(user, profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateAvatar(UUID userId, String avatarUrl) {
        User user = getUser(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setUser(user);
                    return p;
                });
        profile.setAvatarUrl(avatarUrl);
        userProfileRepository.save(profile);
        return toResponse(user, profile);
    }

    /** Từ 18 tuổi trở lên tính đến ngày đăng ký của tài khoản; không quá {@value #MAX_AGE} tuổi so với hiện tại. */
    private void validateDateOfBirth(User user, LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate registeredOn = user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : today;

        if (dateOfBirth.isAfter(registeredOn.minusYears(MIN_AGE))) {
            throw new BusinessException(ErrorCode.PROFILE_INVALID_AGE,
                    "Phải từ " + MIN_AGE + " tuổi trở lên tính đến ngày đăng ký");
        }
        if (dateOfBirth.isBefore(today.minusYears(MAX_AGE))) {
            throw new BusinessException(ErrorCode.PROFILE_INVALID_AGE, "Ngày sinh không hợp lệ");
        }
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND, "Không tìm thấy user"));
    }

    private ProfileResponse toResponse(User user, UserProfile profile) {
        if (profile == null) {
            return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(),
                    null, null, null, null, null, null);
        }
        LocalDate dob = profile.getDateOfBirth();
        Integer age = dob == null ? null : Period.between(dob, LocalDate.now(clock)).getYears();
        return new ProfileResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                dob, age, profile.getWeightKg(), profile.getHeightCm(),
                profile.getGoal(), profile.getAvatarUrl());
    }
}
