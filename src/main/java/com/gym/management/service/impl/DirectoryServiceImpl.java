package com.gym.management.service.impl;

import com.gym.management.dto.response.BranchResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.dto.response.TrainerResponse;
import com.gym.management.dto.response.UserSummaryResponse;
import com.gym.management.entity.RoleName;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.BranchRepository;
import com.gym.management.repository.UserDirectoryRepository;
import com.gym.management.service.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {

    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_QUERY_LENGTH = 100;

    private final UserDirectoryRepository userDirectoryRepository;
    private final BranchRepository branchRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> searchMembers(String role, String query, int page, int size) {
        if (role == null || !RoleName.MEMBER.name().equalsIgnoreCase(role.trim())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ hỗ trợ tìm hội viên (role=MEMBER)");
        }
        // Thứ tự sắp xếp cố định (không nhận sort từ client); thêm id để phân trang ổn định khi trùng tên.
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("fullName").ascending().and(Sort.by("id").ascending()));

        return PageResponse.from(
                userDirectoryRepository.searchActiveByRole(RoleName.MEMBER.name(), normalizeQuery(query), pageable),
                u -> new UserSummaryResponse(u.getId(), u.getFullName(), u.getPhone(), u.getEmail()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerResponse> listTrainers() {
        return userDirectoryRepository.findActiveByRole(RoleName.TRAINER.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches() {
        return branchRepository.findAll(Sort.by("name").ascending()).stream()
                .filter(b -> "ACTIVE".equals(b.getStatus()))
                .map(b -> new BranchResponse(b.getId(), b.getName()))
                .toList();
    }

    /** Cắt khoảng trắng, giới hạn độ dài, thoát ký tự đặc biệt của LIKE. Null thành chuỗi rỗng (không lọc). */
    static String normalizeQuery(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            trimmed = trimmed.substring(0, MAX_QUERY_LENGTH);
        }
        return escapeLike(trimmed);
    }

    /** Thoát ! trước, rồi % _ [ (SQL Server coi [ là ký tự đặc biệt của LIKE). Khớp với ESCAPE '!' trong truy vấn. */
    static String escapeLike(String raw) {
        return raw.replace("!", "!!").replace("%", "!%").replace("_", "!_").replace("[", "![");
    }
}
