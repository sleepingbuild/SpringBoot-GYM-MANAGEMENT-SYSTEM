package com.gym.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lý do PT rời buổi sớm. Service còn cắt khoảng trắng và kiểm tra lại độ dài tối thiểu sau khi cắt. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EarlyLeaveReasonRequest {

    @NotBlank(message = "Lý do là bắt buộc")
    @Size(min = 5, max = 255, message = "Lý do phải từ 5 đến 255 ký tự")
    private String reason;
}
