package com.gym.management.integration.hardware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.entity.FaceProfile;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.FaceProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaceMatchServiceImplTest {

    @Mock
    private FaceProfileRepository faceProfileRepository;

    private FaceMatchServiceImpl faceMatchService;

    private static final List<Double> BASE = buildDescriptor(0.0);

    @BeforeEach
    void setUp() {
        FaceMatchProperties properties = new FaceMatchProperties();
        properties.setMatchThreshold(0.45); // đúng giá trị thật trong application-local.yml
        faceMatchService = new FaceMatchServiceImpl(faceProfileRepository, properties, new ObjectMapper());
    }

    private static List<Double> buildDescriptor(double value) {
        Double[] arr = new Double[128];
        for (int i = 0; i < 128; i++) arr[i] = value;
        return List.of(arr);
    }

    @Test
    void euclideanDistance_sameVector_isZero() {
        assertThat(faceMatchService.euclideanDistance(BASE, BASE)).isEqualTo(0.0);
    }

    @Test
    void euclideanDistance_differentVector_isPositive() {
        List<Double> other = buildDescriptor(1.0);
        assertThat(faceMatchService.euclideanDistance(BASE, other)).isGreaterThan(0.0);
    }

    @Test
    void validateDescriptor_wrongLength_throwsFaceDescriptorInvalid() {
        List<Double> shortDescriptor = List.of(1.0, 2.0, 3.0);

        assertThatThrownBy(() -> faceMatchService.matchAgainstAll(shortDescriptor))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACE_DESCRIPTOR_INVALID);
    }

    @Test
    void matchAgainstAll_distanceBelowThreshold_returnsMatchedUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        FaceProfile profile = new FaceProfile();
        profile.setUser(user);
        // Lệch rất nhỏ (0.001 mỗi chiều) -> khoảng cách Euclidean << 0.45
        profile.setDescriptor(new ObjectMapper().writeValueAsString(buildDescriptor(0.001)));

        when(faceProfileRepository.findAll()).thenReturn(List.of(profile));

        Optional<UUID> result = faceMatchService.matchAgainstAll(BASE);

        assertThat(result).contains(userId);
    }

    @Test
    void matchAgainstAll_distanceAboveThreshold_returnsEmpty() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());

        FaceProfile profile = new FaceProfile();
        profile.setUser(user);
        // Lệch xa (10.0 mỗi chiều) -> khoảng cách Euclidean >> 0.45
        profile.setDescriptor(new ObjectMapper().writeValueAsString(buildDescriptor(10.0)));

        when(faceProfileRepository.findAll()).thenReturn(List.of(profile));

        assertThat(faceMatchService.matchAgainstAll(BASE)).isEmpty();
    }

    @Test
    void matchAgainstAll_noProfiles_returnsEmpty() {
        when(faceProfileRepository.findAll()).thenReturn(List.of());

        assertThat(faceMatchService.matchAgainstAll(BASE)).isEmpty();
    }

    @Test
    void matchAgainstUser_profileNotFound_throwsFaceProfileNotFound() {
        UUID userId = UUID.randomUUID();
        when(faceProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> faceMatchService.matchAgainstUser(BASE, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACE_PROFILE_NOT_FOUND);
    }

    @Test
    void matchAgainstUser_withinThreshold_returnsTrue() throws Exception {
        UUID userId = UUID.randomUUID();
        FaceProfile profile = new FaceProfile();
        profile.setDescriptor(new ObjectMapper().writeValueAsString(buildDescriptor(0.001)));
        when(faceProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertThat(faceMatchService.matchAgainstUser(BASE, userId)).isTrue();
    }

    @Test
    void matchAgainstUser_beyondThreshold_returnsFalse() throws Exception {
        UUID userId = UUID.randomUUID();
        FaceProfile profile = new FaceProfile();
        profile.setDescriptor(new ObjectMapper().writeValueAsString(buildDescriptor(10.0)));
        when(faceProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertThat(faceMatchService.matchAgainstUser(BASE, userId)).isFalse();
    }
}
