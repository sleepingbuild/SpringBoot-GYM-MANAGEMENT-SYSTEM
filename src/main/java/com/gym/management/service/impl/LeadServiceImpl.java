package com.gym.management.service.impl;

import com.gym.management.constant.LeadStatus;
import com.gym.management.dto.request.ChangeLeadStatusRequest;
import com.gym.management.dto.request.ConvertLeadRequest;
import com.gym.management.dto.request.LeadRequest;
import com.gym.management.dto.response.LeadResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.entity.Lead;
import com.gym.management.entity.RoleName;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.LeadRepository;
import com.gym.management.security.CurrentUserId;
import com.gym.management.security.CurrentUserRole;
import com.gym.management.service.AuthService;
import com.gym.management.service.LeadService;
import com.gym.management.util.PageableUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agent 6 - Leads/CRM.
 * Quyền: SUPER_ADMIN thấy và sửa mọi lead; SALES chỉ thấy lead của mình hoặc chưa gán.
 * Sales thao tác lên lead chưa gán thì tự nhận lead đó.
 */
@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "fullName", "status");
    private static final Pattern VN_PHONE = Pattern.compile("^0\\d{9,10}$");
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Bỏ các ký tự dễ nhầm (0/O, 1/l/I) vì Sales sẽ đọc hoặc chép mật khẩu cho hội viên. */
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    static final int GENERATED_PASSWORD_LENGTH = 12;

    private final LeadRepository leadRepository;
    private final AuthService authService;

    @Override
    @Transactional
    public LeadResponse create(LeadRequest request) {
        String phone = normalizePhone(request.getPhone());
        boolean duplicate = leadRepository.existsByPhone(phone);

        Lead lead = new Lead();
        applyEditableFields(lead, request, phone);
        lead.setStatus(LeadStatus.NEW);
        if (!isAdmin()) {
            lead.setAssignedSalesId(CurrentUserId.get());
        }

        LeadResponse response = LeadResponse.from(leadRepository.save(lead));
        response.setDuplicatePhone(duplicate);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> search(String status, String keyword, int page, int size, String sort) {
        LeadStatus statusFilter = (status == null || status.isBlank()) ? null : LeadStatus.parse(status);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().replaceAll("[%_]", "");
        UUID restrictTo = isAdmin() ? null : CurrentUserId.get();
        Pageable pageable = PageableUtils.of(page, size, validateSort(sort));

        Specification<Lead> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("status"), statusFilter));
            }
            if (kw != null && !kw.isEmpty()) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), "%" + kw.toLowerCase() + "%"),
                        cb.like(root.get("phone"), "%" + kw + "%")));
            }
            if (restrictTo != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("assignedSalesId"), restrictTo),
                        cb.isNull(root.get("assignedSalesId"))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Lead> result = leadRepository.findAll(spec, pageable);
        return PageResponse.from(result, LeadResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponse getById(UUID id) {
        return LeadResponse.from(loadAccessible(id, false));
    }

    @Override
    @Transactional
    public LeadResponse update(UUID id, LeadRequest request) {
        Lead lead = loadAccessible(id, true);
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new BusinessException(ErrorCode.LEAD_ALREADY_CONVERTED, "Lead đã chuyển đổi, không thể chỉnh sửa");
        }
        applyEditableFields(lead, request, normalizePhone(request.getPhone()));
        claimIfUnassigned(lead);
        return LeadResponse.from(leadRepository.save(lead));
    }

    @Override
    @Transactional
    public LeadResponse changeStatus(UUID id, ChangeLeadStatusRequest request) {
        LeadStatus target = LeadStatus.parse(request.getStatus());
        if (target == LeadStatus.CONVERTED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Không đặt CONVERTED trực tiếp, hãy dùng POST /api/v1/leads/{id}/convert");
        }

        Lead lead = loadAccessible(id, true);
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new BusinessException(ErrorCode.LEAD_ALREADY_CONVERTED, "Lead đã chuyển đổi, không thể đổi trạng thái");
        }
        if (!lead.getStatus().canTransitionTo(target)) {
            throw new BusinessException(ErrorCode.LEAD_INVALID_STATUS_TRANSITION,
                    "Không thể chuyển lead từ " + lead.getStatus() + " sang " + target);
        }
        lead.setStatus(target);
        claimIfUnassigned(lead);
        return LeadResponse.from(leadRepository.save(lead));
    }

    @Override
    @Transactional
    public LeadResponse convert(UUID id, ConvertLeadRequest request) {
        Lead lead = loadAccessible(id, true);
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new BusinessException(ErrorCode.LEAD_ALREADY_CONVERTED, "Lead đã được chuyển đổi trước đó");
        }
        if (!lead.getStatus().canTransitionTo(LeadStatus.CONVERTED)) {
            throw new BusinessException(ErrorCode.LEAD_INVALID_STATUS_TRANSITION,
                    "Lead đang ở trạng thái " + lead.getStatus() + ", không thể chuyển đổi thành hội viên");
        }

        // Tạo tài khoản qua service của Agent 1 (kiểm tra email/số điện thoại trùng nằm ở đó).
        // Cùng transaction: nếu lưu lead lỗi thì tài khoản cũng bị rollback.
        boolean generated = request.getPassword() == null || request.getPassword().isBlank();
        String rawPassword = generated ? generatePassword() : request.getPassword();
        UUID userId = authService.createMemberAccount(
                lead.getFullName(),
                request.getEmail().trim().toLowerCase(),
                lead.getPhone(),
                rawPassword).getId();

        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedUserId(userId);
        claimIfUnassigned(lead);
        LeadResponse response = LeadResponse.from(leadRepository.save(lead));
        if (generated) {
            response.setTemporaryPassword(rawPassword); // chỉ trả một lần, không lưu ở đâu khác
        }
        return response;
    }

    // ---------- helpers ----------

    private Lead loadAccessible(UUID id, boolean forUpdate) {
        Lead lead = (forUpdate ? leadRepository.findByIdForUpdate(id) : leadRepository.findById(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAD_NOT_FOUND, "Không tìm thấy lead"));
        if (!isAdmin()) {
            UUID me = CurrentUserId.get();
            if (lead.getAssignedSalesId() != null && !lead.getAssignedSalesId().equals(me)) {
                // Trả 404 thay vì 403 để không lộ sự tồn tại của lead người khác
                throw new BusinessException(ErrorCode.LEAD_NOT_FOUND, "Không tìm thấy lead");
            }
        }
        return lead;
    }

    private void claimIfUnassigned(Lead lead) {
        if (!isAdmin() && lead.getAssignedSalesId() == null) {
            lead.setAssignedSalesId(CurrentUserId.get());
        }
    }

    private boolean isAdmin() {
        return CurrentUserRole.has(RoleName.SUPER_ADMIN.name());
    }

    private void applyEditableFields(Lead lead, LeadRequest request, String normalizedPhone) {
        lead.setFullName(request.getFullName().trim());
        lead.setPhone(normalizedPhone);
        lead.setSource(blankToNull(request.getSource()));
        lead.setNotes(blankToNull(request.getNotes()));
    }

    private String validateSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        String property = sort.split(",")[0].trim();
        if (!SORTABLE.contains(property)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Không hỗ trợ sắp xếp theo '" + property + "' (hỗ trợ: " + String.join(", ", SORTABLE) + ")");
        }
        return sort;
    }

    /** Chuẩn hoá +84xxxxxxxxx / 84xxxxxxxxx về 0xxxxxxxxx để so khớp số trùng. */
    static String normalizePhone(String raw) {
        String p = raw == null ? "" : raw.replaceAll("[\\s.\\-()]", "");
        if (p.startsWith("+84")) {
            p = "0" + p.substring(3);
        } else if (p.startsWith("84") && p.length() >= 11) {
            p = "0" + p.substring(2);
        }
        if (!VN_PHONE.matcher(p).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Số điện thoại không hợp lệ (cần dạng 0xxxxxxxxx hoặc +84xxxxxxxxx)");
        }
        return p;
    }

    static String generatePassword() {
        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
