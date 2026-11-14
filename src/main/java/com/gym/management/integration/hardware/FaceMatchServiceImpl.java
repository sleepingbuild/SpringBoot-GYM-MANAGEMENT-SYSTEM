package com.gym.management.integration.hardware;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.entity.FaceProfile;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.FaceProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceMatchServiceImpl implements FaceMatchService {

    private static final int DESCRIPTOR_LENGTH = 128;

    private final FaceProfileRepository faceProfileRepository;
    private final FaceMatchProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<UUID> matchAgainstAll(List<Double> inputDescriptor) {
        validateDescriptor(inputDescriptor);

        UUID bestUserId = null;
        double bestDistance = Double.MAX_VALUE;

        List<FaceProfile> profiles = faceProfileRepository.findAll();
        for (FaceProfile profile : profiles) {
            List<Double> stored = parseDescriptor(profile.getDescriptor());
            double distance = euclideanDistance(inputDescriptor, stored);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestUserId = profile.getUser().getId();
            }
        }

        boolean matched = bestUserId != null && bestDistance < properties.getMatchThreshold();
        // Chỉ ghi khoảng cách và mã người dùng (không ghi descriptor). Dùng để hiệu chỉnh ngưỡng.
        log.debug("face-match mode=1:N profiles={} bestDistance={} threshold={} matched={} bestUserId={}",
                profiles.size(), formatDistance(bestUserId == null ? null : bestDistance),
                properties.getMatchThreshold(), matched, bestUserId);

        if (matched) {
            return Optional.of(bestUserId);
        }
        return Optional.empty();
    }

    @Override
    public boolean matchAgainstUser(List<Double> inputDescriptor, UUID targetUserId) {
        validateDescriptor(inputDescriptor);

        FaceProfile profile = faceProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FACE_PROFILE_NOT_FOUND,
                        "Bạn chưa đăng ký hồ sơ khuôn mặt"));

        List<Double> stored = parseDescriptor(profile.getDescriptor());
        double distance = euclideanDistance(inputDescriptor, stored);
        boolean matched = distance < properties.getMatchThreshold();
        log.debug("face-match mode=1:1 distance={} threshold={} matched={} userId={}",
                formatDistance(distance), properties.getMatchThreshold(), matched, targetUserId);
        return matched;
    }

    @Override
    public double euclideanDistance(List<Double> a, List<Double> b) {
        double sum = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double diff = a.get(i) - b.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private static String formatDistance(Double distance) {
        return distance == null ? "n/a" : String.format(Locale.ROOT, "%.4f", distance);
    }

    private void validateDescriptor(List<Double> descriptor) {
        if (descriptor == null || descriptor.size() != DESCRIPTOR_LENGTH) {
            throw new BusinessException(ErrorCode.FACE_DESCRIPTOR_INVALID,
                    "Descriptor phải có đúng " + DESCRIPTOR_LENGTH + " chiều");
        }
    }

    private List<Double> parseDescriptor(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FACE_DESCRIPTOR_INVALID,
                    "Dữ liệu descriptor trong hệ thống bị lỗi định dạng");
        }
    }
}
