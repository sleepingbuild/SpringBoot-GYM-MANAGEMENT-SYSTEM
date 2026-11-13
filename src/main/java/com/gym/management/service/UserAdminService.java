package com.gym.management.service;

import com.gym.management.dto.request.AdminUserCreateRequest;
import com.gym.management.dto.request.AdminUserUpdateRequest;
import com.gym.management.dto.response.AdminUserResponse;
import com.gym.management.dto.response.PageResponse;

import java.util.Set;
import java.util.UUID;

/** Agent 1 - SHARED. Quản trị tài khoản: danh sách khách hàng/nhân viên, tạo nhân viên, gán vai trò, khoá/mở. */
public interface UserAdminService {

    /** group = CUSTOMER (hội viên) hoặc STAFF (nhân viên); status rỗng = tất cả, hoặc ACTIVE / INACTIVE. */
    PageResponse<AdminUserResponse> search(String group, String query, String status, int page, int size);

    AdminUserResponse createStaff(AdminUserCreateRequest request);

    AdminUserResponse update(UUID id, AdminUserUpdateRequest request);

    /** Gán vai trò cho tài khoản. Không cho tự đổi vai trò của chính mình. */
    AdminUserResponse setRoles(UUID actorId, UUID id, Set<String> roles);

    /** Khoá (INACTIVE) hoặc mở (ACTIVE) tài khoản. Không cho tự khoá chính mình. */
    AdminUserResponse setStatus(UUID actorId, UUID id, String status);
}
