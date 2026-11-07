package com.gym.management.service.impl;

import com.gym.management.constant.LeadStatus;
import com.gym.management.dto.request.AssignLeadRequest;
import com.gym.management.dto.response.LeadResponse;
import com.gym.management.dto.response.SalesUserResponse;
import com.gym.management.entity.Lead;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.LeadRepository;
import com.gym.management.repository.SalesUserRepository;
import com.gym.management.service.LeadAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadAssignmentServiceImpl implements LeadAssignmentService {

    private final LeadRepository leadRepository;
    private final SalesUserRepository salesUserRepository;

    @Override
    @Transactional
    public LeadResponse assign(UUID leadId, AssignLeadRequest request) {
        Lead lead = leadRepository.findByIdForUpdate(leadId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAD_NOT_FOUND, "Không tìm thấy lead"));
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new BusinessException(ErrorCode.LEAD_ALREADY_CONVERTED, "Lead đã chuyển đổi, không thể giao lại");
        }
        if (lead.getStatus() == LeadStatus.LOST) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Lead đã mất, không thể giao lại");
        }
        UUID salesId = request == null ? null : request.getSalesId();
        if (salesId != null && !salesUserRepository.isActiveSales(salesId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Người được giao phải là Sales đang hoạt động");
        }
        lead.setAssignedSalesId(salesId);
        return LeadResponse.from(leadRepository.save(lead));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesUserResponse> listSalesUsers() {
        return salesUserRepository.findActiveSales();
    }
}
