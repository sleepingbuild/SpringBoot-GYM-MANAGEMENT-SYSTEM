package com.gym.management.service.impl;

import com.gym.management.dto.request.TrainerPayRateRequest;
import com.gym.management.dto.response.TrainerPayRateResponse;
import com.gym.management.entity.TrainerPayRate;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.TrainerPayRateRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.TrainerPayRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainerPayRateServiceImpl implements TrainerPayRateService {

    private final TrainerPayRateRepository trainerPayRateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public TrainerPayRateResponse getByTrainerId(UUID trainerId) {
        return toResponse(trainerPayRateRepository.findByTrainerId(trainerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "PT này chưa được cấu hình lương theo buổi")));
    }

    @Override
    @Transactional
    public TrainerPayRateResponse upsert(UUID trainerId, TrainerPayRateRequest request) {
        TrainerPayRate rate = trainerPayRateRepository.findByTrainerId(trainerId)
                .orElseGet(() -> {
                    User trainer = userRepository.findById(trainerId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy PT"));
                    TrainerPayRate fresh = new TrainerPayRate();
                    fresh.setTrainer(trainer);
                    return fresh;
                });
        rate.setAmountPerSession(request.getAmountPerSession());
        return toResponse(trainerPayRateRepository.save(rate));
    }

    private TrainerPayRateResponse toResponse(TrainerPayRate rate) {
        return TrainerPayRateResponse.builder()
                .trainerId(rate.getTrainer().getId())
                .amountPerSession(rate.getAmountPerSession())
                .build();
    }
}
