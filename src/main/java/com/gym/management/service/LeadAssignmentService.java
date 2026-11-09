package com.gym.management.service;

import com.gym.management.dto.request.AssignLeadRequest;
import com.gym.management.dto.response.LeadResponse;
import com.gym.management.dto.response.SalesUserResponse;

import java.util.List;
import java.util.UUID;

/** Agent 6 - Super Admin giao lead cho Sales phụ trách. Quyền Super Admin do controller kiểm tra. */
public interface LeadAssignmentService {

    /**
     * Giao lead cho một Sales đang hoạt động, hoặc bỏ giao khi salesId rỗng.
     * Lead CONVERTED (LEAD_ALREADY_CONVERTED) và LOST không giao lại được.
     */
    LeadResponse assign(UUID leadId, AssignLeadRequest request);

    /** Danh sách Sales đang hoạt động để chọn người phụ trách, sắp theo tên. */
    List<SalesUserResponse> listSalesUsers();
}
