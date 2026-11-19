package com.gym.management.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body tuỳ chọn của POST /bookings/{id}/complete. Lý do chỉ bắt buộc khi buổi chưa đủ số phút tối thiểu;
 * service kiểm tra điều đó (và độ dài tối thiểu sau khi cắt khoảng trắng), nên ở đây chỉ chặn quá dài.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteBookingRequest {

    @Size(max = 255, message = "Lý do tối đa 255 ký tự")
    private String reason;
}
