package com.gym.management.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gym.management.entity.Lead;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeadResponse {

    private UUID id;
    private String fullName;
    private String phone;
    private String source;
    private String status;
    private UUID assignedSalesId;
    private UUID convertedUserId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Chỉ có giá trị khi tạo lead: true nếu số điện thoại đã có ở lead khác. */
    private Boolean duplicatePhone;

    /** Chỉ có khi convert mà không truyền mật khẩu: mật khẩu sinh ngẫu nhiên, hiển thị một lần duy nhất. */
    private String temporaryPassword;

    public static LeadResponse from(Lead lead) {
        LeadResponse r = new LeadResponse();
        r.id = lead.getId();
        r.fullName = lead.getFullName();
        r.phone = lead.getPhone();
        r.source = lead.getSource();
        r.status = lead.getStatus().name();
        r.assignedSalesId = lead.getAssignedSalesId();
        r.convertedUserId = lead.getConvertedUserId();
        r.notes = lead.getNotes();
        r.createdAt = lead.getCreatedAt();
        r.updatedAt = lead.getUpdatedAt();
        return r;
    }
}
