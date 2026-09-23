package com.gym.management.service;

import com.gym.management.dto.request.UpdateProfileRequest;
import com.gym.management.dto.response.ProfileResponse;

import java.util.UUID;

public interface ProfileService {

    ProfileResponse getMyProfile(UUID userId);

    ProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request);

    ProfileResponse updateAvatar(UUID userId, String avatarUrl);
}
