package com.gym.management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/** Super Admin tạo tài khoản nhân viên (không tạo hội viên: hội viên tự đăng ký hoặc chuyển từ lead). */
@Getter
@Setter
public class AdminUserCreateRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự")
    private String password;

    @NotEmpty(message = "Phải chọn ít nhất một vai trò")
    private Set<String> roles;
}
