package com.gym.management.controller;

import com.gym.management.dto.request.ChangeLeadStatusRequest;
import com.gym.management.dto.request.ConvertLeadRequest;
import com.gym.management.dto.request.LeadRequest;
import com.gym.management.dto.response.LeadResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@Tag(name = "Leads/CRM", description = "Quản lý khách tiềm năng (Sales, Super Admin)")
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SALES','SUPER_ADMIN')")
    @Operation(summary = "Danh sách lead",
            description = "Super Admin thấy mọi lead. Sales chỉ thấy lead được gán cho mình hoặc chưa gán. "
                    + "Lọc theo status (NEW, CONTACTED, CONVERTED, LOST) và keyword (tên hoặc số điện thoại). "
                    + "sort hỗ trợ: createdAt, updatedAt, fullName, status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Danh sách phân trang")
    public ApiResponse<PageResponse<LeadResponse>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(leadService.search(status, keyword, page, size, sort));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SALES','SUPER_ADMIN')")
    @Operation(summary = "Tạo lead",
            description = "Số điện thoại được chuẩn hoá về dạng 0xxxxxxxxx. Nếu số đã có ở lead khác, "
                    + "lead vẫn được tạo và response có duplicatePhone = true. Sales tạo lead thì tự được gán lead đó.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Đã tạo lead")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    public ApiResponse<LeadResponse> create(@Valid @RequestBody LeadRequest request) {
        return ApiResponse.success("Tạo lead thành công", leadService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALES','SUPER_ADMIN')")
    @Operation(summary = "Chi tiết lead", description = "Sales không thấy lead của người khác (trả 404).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chi tiết lead")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "LEAD_NOT_FOUND")
    public ApiResponse<LeadResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(leadService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALES','SUPER_ADMIN')")
    @Operation(summary = "Cập nhật thông tin lead",
            description = "Sửa họ tên, số điện thoại, nguồn, ghi chú. Lead đã CONVERTED không sửa được.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã cập nhật")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "LEAD_NOT_FOUND")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "LEAD_ALREADY_CONVERTED")
    public ApiResponse<LeadResponse> update(@PathVariable UUID id, @Valid @RequestBody LeadRequest request) {
        return ApiResponse.success("Cập nhật lead thành công", leadService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SALES','SUPER_ADMIN')")
    @Operation(summary = "Đổi trạng thái lead",
            description = "Chuyển hợp lệ: NEW → CONTACTED | LOST, CONTACTED → LOST. "
                    + "LOST và CONVERTED là trạng thái cuối. Không đặt CONVERTED ở đây (dùng /convert).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã đổi trạng thái")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Chuyển trạng thái không hợp lệ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "LEAD_ALREADY_CONVERTED")
    public ApiResponse<LeadResponse> changeStatus(@PathVariable UUID id,
                                                  @Valid @RequestBody ChangeLeadStatusRequest request) {
        return ApiResponse.success("Đổi trạng thái lead thành công", leadService.changeStatus(id, request));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('SALES','SUPER_ADMIN')")
    @Operation(summary = "Chuyển lead thành hội viên",
            description = "Tạo tài khoản MEMBER từ lead (cần email và mật khẩu tạm), đặt lead = CONVERTED. "
                    + "Không mua gói ở bước này (dùng /member-packages) và không sinh hoa hồng.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã chuyển đổi")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "LEAD_NOT_FOUND")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
            description = "LEAD_ALREADY_CONVERTED hoặc email/số điện thoại đã được dùng")
    public ApiResponse<LeadResponse> convert(@PathVariable UUID id, @Valid @RequestBody ConvertLeadRequest request) {
        return ApiResponse.success("Chuyển đổi lead thành hội viên thành công", leadService.convert(id, request));
    }
}
