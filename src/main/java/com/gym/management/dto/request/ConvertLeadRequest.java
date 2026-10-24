package com.gym.management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Lead chỉ có tên và số điện thoại, nên convert phải nhận thêm email (mật khẩu tuỳ chọn). */
@Getter
@Setter
public class ConvertLeadRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    /** Tuỳ chọn. Bỏ trống thì hệ thống sinh mật khẩu ngẫu nhiên và trả về một lần trong response. */
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    private String password;
}
