package com.gym.management.dto.response;

import com.gym.management.entity.CommissionStatus;
import com.gym.management.service.event.BookingSettledEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionResponse {
    private UUID id;
    private UUID staffId;
    private UUID bookingId;
    private BookingSettledEvent.Outcome outcome;
    private BigDecimal amount;
    private CommissionStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
