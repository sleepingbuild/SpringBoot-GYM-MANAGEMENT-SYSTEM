package com.gym.management.controller;

import com.gym.management.dto.request.AssignLeadRequest;
import com.gym.management.dto.response.LeadResponse;
import com.gym.management.dto.response.SalesUserResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.LeadAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Giao lead cho Sales phụ trách (chỉ Super Admin). Cùng nhóm Swagger "Leads/CRM" với LeadController. */
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@Tag(name = "Leads/CRM", description = "Quản lý khách tiềm năng (Sales, Super Admin)")
public class LeadAssignmentController {

    private final LeadAssignmentService leadAssignmentService;

    @GetMapping("/sales-users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Danh sách Sales để giao lead",
            description = "Chỉ Super Admin. Trả các tài khoản Sales đang hoạt động, sắp theo tên, để chọn người phụ trách lead.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Danh sách Sales đang hoạt động")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Chỉ Super Admin")
    public ApiResponse<List<SalesUserResponse>> salesUsers() {
        return ApiResponse.success(leadAssignmentService.listSalesUsers());
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Giao lead cho Sales",
            description = "Chỉ Super Admin. salesId là một Sales đang hoạt động; để trống thì bỏ giao (lead về trạng thái chưa gán, "
                    + "mọi Sales đều thấy). Lead CONVERTED không giao lại được (409 LEAD_ALREADY_CONVERTED), lead LOST cũng không "
                    + "(400). Sales không còn thấy lead đã giao cho người khác.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã giao lead")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "salesId không phải Sales đang hoạt động, hoặc lead đã mất")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Chỉ Super Admin")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "LEAD_NOT_FOUND")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "LEAD_ALREADY_CONVERTED")
    public ApiResponse<LeadResponse> assign(@PathVariable UUID id, @RequestBody AssignLeadRequest request) {
        return ApiResponse.success("Giao lead thành công", leadAssignmentService.assign(id, request));
    }
}
