package com.gym.management.dto.response;

import com.gym.management.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private UUID trainerId;
    private String trainerName;
    private UUID branchId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private LocalDateTime checkInTime;
    private String checkInMethod;
    private LocalDateTime checkOutTime;
    private String checkOutMethod;
    private LocalDateTime ptCheckInTime;
    private String ptCheckInMethod;
    private LocalDateTime ptCheckOutTime;
    private String ptCheckOutMethod;
    private String ptEarlyLeaveReason;
    /**
     * Tính động, không lưu DB. null = PT chưa quét ra nên chưa xác định được.
     * true = PT ra từ giờ kết thúc trở đi, hoặc ra sớm nhưng có lý do. false = ra sớm không lý do.
     */
    private Boolean fullDuration;
    private String notes;
}
