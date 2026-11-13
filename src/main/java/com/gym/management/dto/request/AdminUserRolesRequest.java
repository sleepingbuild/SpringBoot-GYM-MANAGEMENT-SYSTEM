package com.gym.management.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AdminUserRolesRequest {

    @NotEmpty(message = "Phải chọn ít nhất một vai trò")
    private Set<String> roles;
}
