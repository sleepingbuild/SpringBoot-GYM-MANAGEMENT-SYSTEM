package com.gym.management.service;

import com.gym.management.dto.request.TrainerPayRateRequest;
import com.gym.management.dto.response.TrainerPayRateResponse;

import java.util.UUID;

/**
 * Agent 5 - POS & Commission Module.
 */
public interface TrainerPayRateService {

    TrainerPayRateResponse getByTrainerId(UUID trainerId);

    /** Upsert — tạo mới nếu PT chưa có mức lương, cập nhật nếu đã có. */
    TrainerPayRateResponse upsert(UUID trainerId, TrainerPayRateRequest request);
}
