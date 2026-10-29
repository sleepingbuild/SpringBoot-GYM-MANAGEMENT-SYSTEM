package com.gym.management.service.impl;

import com.gym.management.dto.response.FaceProfileStatusResponse;
import com.gym.management.entity.FaceProfile;
import com.gym.management.repository.FaceProfileRepository;
import com.gym.management.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaceProfileMyStatusTest {

    @Mock private FaceProfileRepository faceProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private FaceProfileServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void registered_returnsTrueAndUpdatedAt() {
        FaceProfile profile = new FaceProfile();
        profile.setCreatedAt(LocalDateTime.of(2026, 10, 1, 8, 0));
        profile.setUpdatedAt(LocalDateTime.of(2026, 10, 20, 9, 30));
        when(faceProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        FaceProfileStatusResponse result = service.getMyStatus(userId);

        assertThat(result.isRegistered()).isTrue();
        assertThat(result.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 20, 9, 30));
    }

    @Test
    void registeredButNeverUpdated_fallsBackToCreatedAt() {
        FaceProfile profile = new FaceProfile();
        profile.setCreatedAt(LocalDateTime.of(2026, 10, 1, 8, 0));
        when(faceProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        FaceProfileStatusResponse result = service.getMyStatus(userId);

        assertThat(result.isRegistered()).isTrue();
        assertThat(result.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 8, 0));
    }

    @Test
    void notRegistered_returnsFalseWithoutError() {
        when(faceProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        FaceProfileStatusResponse result = service.getMyStatus(userId);

        assertThat(result.isRegistered()).isFalse();
        assertThat(result.getUpdatedAt()).isNull();
        verifyNoMoreInteractions(userRepository);
    }
}
