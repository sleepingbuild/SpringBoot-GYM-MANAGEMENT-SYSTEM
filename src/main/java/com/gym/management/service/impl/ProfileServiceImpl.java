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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final int MIN_AGE = 18;

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    @Override
    public ProfileResponse getMyProfile(UUID userId) {
        User user = getUser(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        return toResponse(user, profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request) {
        if (request.getAge() != null && request.getAge() < MIN_AGE) {
            throw new BusinessException(ErrorCode.PROFILE_INVALID_AGE, "Tuổi phải từ " + MIN_AGE + " trở lên");
        }

        User user = getUser(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setUser(user);
                    return p;
                });

        profile.setAge(request.getAge());
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

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND, "Không tìm thấy user"));
    }

    private ProfileResponse toResponse(User user, UserProfile profile) {
        if (profile == null) {
            return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(),
                    null, null, null, null, null);
        }
        return new ProfileResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                profile.getAge(), profile.getWeightKg(), profile.getHeightCm(),
                profile.getGoal(), profile.getAvatarUrl());
    }
}
