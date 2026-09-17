package com.gym.management.integration.hardware;

import com.gym.management.entity.FaceProfile;
import com.gym.management.repository.FaceProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent 3 - Check-in Engine & Access Control.
 *
 * ⚠️ ĐÂY LÀ MOCK — KHÔNG PHẢI NHẬN DIỆN KHUÔN MẶT THẬT.
 *
 * So khớp bằng SHA-256 hash của toàn bộ ảnh (byte-for-byte). Nghĩa là chỉ nhận
 * ra đúng khi ảnh chụp lúc check-in giống Y HỆT ảnh đã đăng ký (không xử lý
 * góc mặt/ánh sáng/biểu cảm khác nhau như face recognition thật).
 *
 * Mục đích của mock này là dựng xong pipeline (entity, API, luồng check-in 6
 * bước) để test end-to-end ngay bây giờ. Khi có SDK/Cloud API thật:
 *   1. Viết class mới (vd AzureFaceMatcher) implements FaceMatcher, gọi API
 *      Identify (1:N search) thật, so sánh embedding thay vì hash.
 *   2. Đánh dấu class mới @Primary, xoá @Primary ở MockFaceMatcher (hoặc xoá
 *      hẳn class này).
 *   3. Không cần sửa CheckInService/CheckInController — chúng chỉ biết interface.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockFaceMatcher implements FaceMatcher {

    private final FaceProfileRepository faceProfileRepository;

    @Override
    public Optional<UUID> identify(byte[] imageBytes) {
        String hash = sha256Hex(imageBytes);
        Optional<FaceProfile> match = faceProfileRepository.findByImageHash(hash);

        if (match.isEmpty()) {
            log.info("[MockFaceMatcher] Không tìm thấy hồ sơ khớp với ảnh (hash={})", hash);
            return Optional.empty();
        }

        UUID userId = match.get().getUser().getId();
        log.info("[MockFaceMatcher] Khớp với user {} (hash={})", userId, hash);
        return Optional.of(userId);
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng trên JVM này", e);
        }
    }
}
