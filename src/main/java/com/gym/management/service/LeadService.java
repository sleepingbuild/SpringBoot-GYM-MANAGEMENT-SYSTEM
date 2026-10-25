package com.gym.management.service;

import com.gym.management.dto.request.ChangeLeadStatusRequest;
import com.gym.management.dto.request.ConvertLeadRequest;
import com.gym.management.dto.request.LeadRequest;
import com.gym.management.dto.response.LeadResponse;
import com.gym.management.dto.response.PageResponse;

import java.util.UUID;

public interface LeadService {

    LeadResponse create(LeadRequest request);

    PageResponse<LeadResponse> search(String status, String keyword, int page, int size, String sort);

    LeadResponse getById(UUID id);

    LeadResponse update(UUID id, LeadRequest request);

    LeadResponse changeStatus(UUID id, ChangeLeadStatusRequest request);

    /** Tạo user MEMBER thật từ lead; chỉ lead NEW/CONTACTED mới convert được. */
    LeadResponse convert(UUID id, ConvertLeadRequest request);
}
