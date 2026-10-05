package com.gym.management.integration.hardware;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaceMatchService {

    /**
     * So khớp 1:N — dùng cho luồng Kiosk. Trả userId khớp gần nhất trong ngưỡng, hoặc empty.
     */
    Optional<UUID> matchAgainstAll(List<Double> inputDescriptor);

    /**
     * So khớp 1:1 — dùng cho luồng tự điểm danh, chỉ so với đúng 1 profile của currentUserId.
     */
    boolean matchAgainstUser(List<Double> inputDescriptor, UUID targetUserId);

    double euclideanDistance(List<Double> a, List<Double> b);
}
