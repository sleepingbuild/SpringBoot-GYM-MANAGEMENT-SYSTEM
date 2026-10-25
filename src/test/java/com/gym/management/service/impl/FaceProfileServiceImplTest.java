package com.gym.management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.dto.request.FaceDescriptorRequest;
import com.gym.management.dto.response.FaceProfileResponse;
import com.gym.management.entity.FaceProfile;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.FaceProfileRepository;
import com.gym.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaceProfileServiceImplTest {

    @Mock private FaceProfileRepository faceProfileRepository;
    @Mock private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FaceProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FaceProfileServiceImpl(faceProfileRepository, userRepository, objectMapper);
    }

    private static List<Double> descriptor(double value) {
        Double[] arr = new Double[128];
        for (int i = 0; i < 128; i++) arr[i] = value;
        return List.of(arr);
    }

    private static FaceDescriptorRequest request(double value) {
        FaceDescriptorRequest request = new FaceDescriptorRequest();
        request.setDescriptor(descriptor(value));
        return request;
    }

    private static User user(String name) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName(name);
        return user;
    }

    @Test
    void registerSelf_newProfile_savesDescriptorAndRegisteredBySelf() throws Exception {
        User me = user("Hoi Vien A");
        when(userRepository.findById(me.getId())).thenReturn(Optional.of(me));
        when(faceProfileRepository.findByUserId(me.getId())).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(me.getId())).thenReturn(me);
        when(faceProfileRepository.save(any(FaceProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        FaceProfileResponse response = service.registerSelf(me.getId(), request(0.5));

        ArgumentCaptor<FaceProfile> captor = ArgumentCaptor.forClass(FaceProfile.class);
        verify(faceProfileRepository).save(captor.capture());
        FaceProfile saved = captor.getValue();
        assertThat(saved.getDescriptor()).isEqualTo(objectMapper.writeValueAsString(descriptor(0.5)));
        assertThat(saved.getReferenceImage()).isNull(); // v1: không lưu ảnh (quyết định A.4)
        assertThat(response.getUserId()).isEqualTo(me.getId());
        assertThat(response.getRegisteredBy()).isEqualTo(me.getId());
    }

    @Test
    void registerFor_byAnotherUser_recordsRegistrar() {
        User target = user("Hoi Vien B");
        User receptionist = user("Le Tan");
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(faceProfileRepository.findByUserId(target.getId())).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(receptionist.getId())).thenReturn(receptionist);
        when(faceProfileRepository.save(any(FaceProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        FaceProfileResponse response = service.registerFor(target.getId(), receptionist.getId(), request(0.2));

        assertThat(response.getUserId()).isEqualTo(target.getId());
        assertThat(response.getRegisteredBy()).isEqualTo(receptionist.getId());
    }

    @Test
    void register_existingProfile_overwritesDescriptorInsteadOfCreatingSecond() throws Exception {
        User me = user("Hoi Vien C");
        FaceProfile existing = new FaceProfile();
        existing.setUser(me);
        existing.setDescriptor("[\"old\"]");

        when(userRepository.findById(me.getId())).thenReturn(Optional.of(me));
        when(faceProfileRepository.findByUserId(me.getId())).thenReturn(Optional.of(existing));
        when(userRepository.getReferenceById(me.getId())).thenReturn(me);
        when(faceProfileRepository.save(any(FaceProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registerSelf(me.getId(), request(0.9));

        // 1 người 1 hồ sơ: đăng ký lại ghi đè chính bản ghi cũ (REQUIREMENTS.md mục 4)
        verify(faceProfileRepository).save(existing);
        assertThat(existing.getDescriptor()).isEqualTo(objectMapper.writeValueAsString(descriptor(0.9)));
    }

    @Test
    void register_userNotFound_throwsResourceNotFoundAndSavesNothing() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerFor(missing, UUID.randomUUID(), request(0.1)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);

        verify(faceProfileRepository, never()).save(any());
    }

    @Test
    void getByUserId_found_returnsResponse() {
        User me = user("Hoi Vien D");
        FaceProfile profile = new FaceProfile();
        profile.setUser(me);
        profile.setDescriptor("[]");
        when(faceProfileRepository.findByUserId(me.getId())).thenReturn(Optional.of(profile));

        FaceProfileResponse response = service.getByUserId(me.getId());

        assertThat(response.getUserId()).isEqualTo(me.getId());
        assertThat(response.getUserFullName()).isEqualTo("Hoi Vien D");
        assertThat(response.getRegisteredBy()).isNull();
    }

    @Test
    void getByUserId_notFound_throwsFaceProfileNotFound() {
        UUID userId = UUID.randomUUID();
        when(faceProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByUserId(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACE_PROFILE_NOT_FOUND);
    }

    @Test
    void deleteByUserId_found_deletes() {
        UUID userId = UUID.randomUUID();
        when(faceProfileRepository.findByUserId(userId)).thenReturn(Optional.of(new FaceProfile()));

        service.deleteByUserId(userId);

        verify(faceProfileRepository).deleteByUserId(userId);
    }

    @Test
    void deleteByUserId_notFound_throwsAndDeletesNothing() {
        UUID userId = UUID.randomUUID();
        when(faceProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteByUserId(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACE_PROFILE_NOT_FOUND);

        verify(faceProfileRepository, never()).deleteByUserId(any());
    }
}
