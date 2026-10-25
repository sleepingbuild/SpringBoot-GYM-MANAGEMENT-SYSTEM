package com.gym.management.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.dto.request.FaceDescriptorRequest;
import com.gym.management.dto.response.FaceProfileResponse;
import com.gym.management.entity.FaceProfile;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.FaceProfileRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.FaceProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaceProfileServiceImpl implements FaceProfileService {

    private final FaceProfileRepository faceProfileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public FaceProfileResponse registerSelf(UUID currentUserId, FaceDescriptorRequest request) {
        return upsert(currentUserId, currentUserId, request.getDescriptor());
    }

    @Override
    @Transactional
    public FaceProfileResponse registerFor(UUID targetUserId, UUID registeredBy, FaceDescriptorRequest request) {
        return upsert(targetUserId, registeredBy, request.getDescriptor());
    }

    @Override
    public FaceProfileResponse getByUserId(UUID userId) {
        return toResponse(faceProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FACE_PROFILE_NOT_FOUND,
                        "Người dùng chưa đăng ký hồ sơ khuôn mặt")));
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        if (faceProfileRepository.findByUserId(userId).isEmpty()) {
            throw new BusinessException(ErrorCode.FACE_PROFILE_NOT_FOUND,
                    "Người dùng chưa đăng ký hồ sơ khuôn mặt");
        }
        faceProfileRepository.deleteByUserId(userId);
    }

    // 1 người 1 hồ sơ — đăng ký lại = ghi đè (REQUIREMENTS.md mục 4)
    private FaceProfileResponse upsert(UUID targetUserId, UUID registeredBy, List<Double> descriptor) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy người dùng"));

        FaceProfile profile = faceProfileRepository.findByUserId(targetUserId).orElseGet(() -> {
            FaceProfile p = new FaceProfile();
            p.setUser(target);
            return p;
        });
        profile.setDescriptor(serialize(descriptor));
        profile.setRegisteredBy(userRepository.getReferenceById(registeredBy));

        return toResponse(faceProfileRepository.save(profile));
    }

    private String serialize(List<Double> descriptor) {
        try {
            return objectMapper.writeValueAsString(descriptor);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.FACE_DESCRIPTOR_INVALID, "Không thể lưu descriptor");
        }
    }

    private FaceProfileResponse toResponse(FaceProfile p) {
        return FaceProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .userFullName(p.getUser().getFullName())
                .referenceImage(p.getReferenceImage())
                .registeredBy(p.getRegisteredBy() != null ? p.getRegisteredBy().getId() : null)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
