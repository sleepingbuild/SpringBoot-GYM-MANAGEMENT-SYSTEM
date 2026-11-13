package com.gym.management.service.impl;

import com.gym.management.dto.request.AdminUserCreateRequest;
import com.gym.management.dto.request.AdminUserUpdateRequest;
import com.gym.management.dto.response.AdminUserResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.entity.Role;
import com.gym.management.entity.RoleName;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.RoleRepository;
import com.gym.management.repository.UserAdminRepository;
import com.gym.management.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    static final int MAX_PAGE_SIZE = 50;
    static final String STATUS_ACTIVE = "ACTIVE";
    static final String STATUS_INACTIVE = "INACTIVE";
    static final List<String> STAFF_ROLES = List.of(
            RoleName.SUPER_ADMIN.name(), RoleName.RECEPTIONIST.name(),
            RoleName.TRAINER.name(), RoleName.SALES.name());
    static final List<String> CUSTOMER_ROLES = List.of(RoleName.MEMBER.name());

    private final UserAdminRepository userAdminRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String group, String query, String status, int page, int size) {
        List<String> roleNames = switch (group == null ? "" : group.trim().toUpperCase(Locale.ROOT)) {
            case "CUSTOMER" -> CUSTOMER_ROLES;
            case "STAFF" -> STAFF_ROLES;
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "group chỉ nhận CUSTOMER hoặc STAFF");
        };
        String normalizedStatus = status == null || status.isBlank() ? "" : requireStatus(status);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("fullName").ascending().and(Sort.by("id").ascending()));

        return PageResponse.from(
                userAdminRepository.search(roleNames, normalizedStatus, DirectoryServiceImpl.normalizeQuery(query), pageable),
                UserAdminServiceImpl::toResponse);
    }

    @Override
    @Transactional
    public AdminUserResponse createStaff(AdminUserCreateRequest request) {
        Set<String> roleNames = normalizeRoles(request.getRoles());
        if (roleNames.contains(RoleName.MEMBER.name())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tài khoản nhân viên không được có vai trò MEMBER");
        }
        String email = request.getEmail().trim();
        String phone = normalizePhone(request.getPhone());

        if (userAdminRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, "Email đã được sử dụng");
        }
        if (phone != null && userAdminRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.AUTH_PHONE_ALREADY_EXISTS, "Số điện thoại đã được sử dụng");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoles(resolveRoles(roleNames));
        return toResponse(userAdminRepository.save(user));
    }

    @Override
    @Transactional
    public AdminUserResponse update(UUID id, AdminUserUpdateRequest request) {
        User user = getUser(id);
        String phone = normalizePhone(request.getPhone());
        if (phone != null && userAdminRepository.existsByPhoneAndIdNot(phone, id)) {
            throw new BusinessException(ErrorCode.AUTH_PHONE_ALREADY_EXISTS, "Số điện thoại đã được sử dụng");
        }
        user.setFullName(request.getFullName().trim());
        user.setPhone(phone);
        return toResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse setRoles(UUID actorId, UUID id, Set<String> roles) {
        User user = getUser(id);
        rejectSelfChange(actorId, id, "Không thể tự đổi vai trò của chính mình");

        Set<String> roleNames = normalizeRoles(roles);
        if (roleNames.contains(RoleName.MEMBER.name()) && roleNames.size() > 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Hội viên (MEMBER) không được kết hợp với vai trò nhân viên");
        }
        user.setRoles(resolveRoles(roleNames));
        return toResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse setStatus(UUID actorId, UUID id, String status) {
        User user = getUser(id);
        rejectSelfChange(actorId, id, "Không thể tự khoá tài khoản của chính mình");
        user.setStatus(requireStatus(status));
        return toResponse(user);
    }

    private User getUser(UUID id) {
        return userAdminRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    private static void rejectSelfChange(UUID actorId, UUID targetId, String message) {
        if (targetId.equals(actorId)) {
            throw new BusinessException(ErrorCode.USER_SELF_CHANGE_FORBIDDEN, message);
        }
    }

    private static String requireStatus(String raw) {
        String status = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_ACTIVE.equals(status) && !STATUS_INACTIVE.equals(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trạng thái chỉ nhận ACTIVE hoặc INACTIVE");
        }
        return status;
    }

    /** Chuẩn hoá tên vai trò (cắt khoảng trắng, chữ hoa) và từ chối tên không có trong RoleName. */
    private static Set<String> normalizeRoles(Set<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Phải chọn ít nhất một vai trò");
        }
        Set<String> result = new LinkedHashSet<>();
        for (String name : raw) {
            String normalized = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
            boolean known = false;
            for (RoleName candidate : RoleName.values()) {
                if (candidate.name().equals(normalized)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vai trò không hợp lệ: " + name);
            }
            result.add(normalized);
        }
        return result;
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            roles.add(roleRepository.findByName(name).orElseThrow(() -> new IllegalStateException(
                    "Role " + name + " chưa được seed trong DB - kiểm tra V1__init_schema.sql")));
        }
        return roles;
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static AdminUserResponse toResponse(User user) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getStatus(), roleNames);
    }
}
