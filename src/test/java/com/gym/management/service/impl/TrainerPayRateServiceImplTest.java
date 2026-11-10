package com.gym.management.service.impl;

import com.gym.management.dto.request.TrainerPayRateRequest;
import com.gym.management.dto.response.TrainerPayRateResponse;
import com.gym.management.entity.Role;
import com.gym.management.entity.TrainerPayRate;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.TrainerPayRateRepository;
import com.gym.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainerPayRateServiceImplTest {

    @Mock private TrainerPayRateRepository trainerPayRateRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TrainerPayRateServiceImpl service;

    private final UUID trainerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(trainerPayRateRepository.save(any(TrainerPayRate.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private User userWithRole(String roleName) {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", trainerId);
        Role role = new Role();
        ReflectionTestUtils.setField(role, "name", roleName);
        u.getRoles().add(role);
        return u;
    }

    private TrainerPayRateRequest request(String amount) {
        TrainerPayRateRequest r = new TrainerPayRateRequest();
        r.setAmountPerSession(new BigDecimal(amount));
        return r;
    }

    @Test
    void get_withoutRate_throwsTrainerPayRateNotFound() {
        when(trainerPayRateRepository.findByTrainerId(trainerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByTrainerId(trainerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRAINER_PAY_RATE_NOT_FOUND);
    }

    @Test
    void get_existingRate_returnsAmount() {
        TrainerPayRate rate = new TrainerPayRate();
        rate.setTrainer(userWithRole("TRAINER"));
        rate.setAmountPerSession(new BigDecimal("150000.00"));
        when(trainerPayRateRepository.findByTrainerId(trainerId)).thenReturn(Optional.of(rate));

        TrainerPayRateResponse res = service.getByTrainerId(trainerId);

        assertThat(res.getTrainerId()).isEqualTo(trainerId);
        assertThat(res.getAmountPerSession()).isEqualByComparingTo("150000");
    }

    @Test
    void upsert_forTrainerWithoutRate_createsIt() {
        when(trainerPayRateRepository.findByTrainerId(trainerId)).thenReturn(Optional.empty());
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(userWithRole("TRAINER")));

        TrainerPayRateResponse res = service.upsert(trainerId, request("200000"));

        assertThat(res.getAmountPerSession()).isEqualByComparingTo("200000");
        verify(trainerPayRateRepository).save(any(TrainerPayRate.class));
    }

    @Test
    void upsert_existingRate_updatesWithoutLookingUpTheUser() {
        TrainerPayRate rate = new TrainerPayRate();
        rate.setTrainer(userWithRole("TRAINER"));
        rate.setAmountPerSession(new BigDecimal("150000"));
        when(trainerPayRateRepository.findByTrainerId(trainerId)).thenReturn(Optional.of(rate));

        TrainerPayRateResponse res = service.upsert(trainerId, request("180000"));

        assertThat(res.getAmountPerSession()).isEqualByComparingTo("180000");
        verify(userRepository, never()).findById(any());
    }

    @Test
    void upsert_userWhoIsNotATrainer_isRejected() {
        when(trainerPayRateRepository.findByTrainerId(trainerId)).thenReturn(Optional.empty());
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(userWithRole("MEMBER")));

        assertThatThrownBy(() -> service.upsert(trainerId, request("150000")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(trainerPayRateRepository, never()).save(any());
    }

    @Test
    void upsert_unknownUser_isNotFound() {
        when(trainerPayRateRepository.findByTrainerId(trainerId)).thenReturn(Optional.empty());
        when(userRepository.findById(trainerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(trainerId, request("150000")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
