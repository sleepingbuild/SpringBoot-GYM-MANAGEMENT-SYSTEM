package com.gym.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Dùng cho tạo mới (POST) và cập nhật (PUT) lead. */
@Getter
@Setter
public class LeadRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Size(max = 50, message = "Nguồn tối đa 50 ký tự")
    private String source;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String notes;
}
