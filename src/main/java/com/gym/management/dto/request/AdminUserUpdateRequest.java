package com.gym.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserUpdateRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;
}
