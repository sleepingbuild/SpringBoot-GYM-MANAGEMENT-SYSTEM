package com.gym.management.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FaceAttendanceResponse {
    private UUID matchedUserId;
    private String matchedUserName;
    private String action;         // "CHECK_IN" / "CHECK_OUT" / "NONE"
    private String targetType;     // "BOOKING" / "STAFF_ATTENDANCE" / null
    private LocalDateTime scanTime;
    private boolean bookingLinked;
    private boolean duplicateScan; // true nếu bị chặn do quét lại quá sớm (quyết định A.3)

    // ---- Thêm theo CHANGE_PT_ATTENDANCE_AND_PAY.md (luồng PT quét mặt theo buổi) ----
    private UUID bookingId;          // buổi vừa được điểm danh — FE cần để gọi PUT /bookings/{id}/early-leave-reason
    private boolean earlyLeave;      // PT quét ra trước giờ kết thúc buổi
    private boolean reasonRequired;  // FE hiện ô nhập lý do (true khi earlyLeave)
    private String message;          // thông báo thân thiện cho FE (VD: không có buổi nào cần điểm danh) — KHÔNG phải lỗi
}
