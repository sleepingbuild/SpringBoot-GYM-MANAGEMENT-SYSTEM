package com.gym.management.service;

import com.gym.management.dto.response.BranchResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.dto.response.TrainerResponse;
import com.gym.management.dto.response.UserSummaryResponse;

import java.util.List;

/** Agent 1 - SHARED. Danh mục chỉ đọc cho giao diện: hội viên, huấn luyện viên, chi nhánh. */
public interface DirectoryService {

    /** Tìm hội viên đang hoạt động theo tên, số điện thoại hoặc email. Hiện chỉ hỗ trợ role MEMBER. */
    PageResponse<UserSummaryResponse> searchMembers(String role, String query, int page, int size);

    /** Huấn luyện viên đang hoạt động, sắp theo tên. */
    List<TrainerResponse> listTrainers();

    /** Chi nhánh đang hoạt động, sắp theo tên. */
    List<BranchResponse> listBranches();
}
