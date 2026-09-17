package com.gym.management.service.impl;

import com.gym.management.dto.response.FaceProfileResponse;
import com.gym.management.entity.FaceProfile;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.integration.hardware.MockFaceMatcher;
import com.gym.management.repository.FaceProfileRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.FaceProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaceProfileServiceImpl implements FaceProfileService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB, khớp application.yml

    private final FaceProfileRepository faceProfileRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FaceProfileResponse registerOrReplace(UUID targetUserId, MultipartFile image, UUID registeredById) {
        validateImage(image);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_USER_NOT_FOUND, "Không tìm thấy hội viên/PT"));
        User registeredBy = userRepository.findById(registeredById)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_USER_NOT_FOUND, "Không tìm thấy tài khoản đang đăng ký ảnh"));

        byte[] imageBytes = readBytes(image);
        String hash = MockFaceMatcher.sha256Hex(imageBytes);

        FaceProfile profile = faceProfileRepository.findByUserId(targetUserId)
                .orElseGet(() -> {
                    FaceProfile p = new FaceProfile();
                    p.setUser(targetUser);
                    return p;
                });

        profile.setImage(imageBytes);
        profile.setImageHash(hash);
        profile.setRegisteredBy(registeredBy);
        faceProfileRepository.save(profile);

        return new FaceProfileResponse(targetUser.getId(), targetUser.getFullName(), true, profile.getUpdatedAt() != null ? profile.getUpdatedAt() : profile.getCreatedAt());
    }

    @Override
    public FaceProfileResponse getStatus(UUID targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_USER_NOT_FOUND));

        return faceProfileRepository.findByUserId(targetUserId)
                .map(p -> new FaceProfileResponse(user.getId(), user.getFullName(), true,
                        p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt()))
                .orElseGet(() -> new FaceProfileResponse(user.getId(), user.getFullName(), false, null));
    }

    @Override
    @Transactional
    public void delete(UUID targetUserId) {
        FaceProfile profile = faceProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FACE_PROFILE_NOT_FOUND, "Hội viên/PT này chưa đăng ký ảnh khuôn mặt"));
        faceProfileRepository.delete(profile);
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.FACE_IMAGE_INVALID, "Vui lòng chọn ảnh");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.FACE_IMAGE_INVALID, "File tải lên phải là ảnh (jpg/png)");
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FACE_IMAGE_INVALID, "Ảnh vượt quá dung lượng cho phép (5MB)");
        }
    }

    private byte[] readBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FACE_IMAGE_INVALID, "Không đọc được file ảnh");
        }
    }
}
