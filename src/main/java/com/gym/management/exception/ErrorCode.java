package com.gym.management.exception;

import org.springframework.http.HttpStatus;

/**
 * Agent 1 - SHARED FILE.
 * Danh sách mã lỗi chuẩn hoá toàn hệ thống — khớp đúng bảng trong API_DESIGN.md.
 * Đã pre-populate đủ mã lỗi cho TẤT CẢ module theo REQUIREMENTS.md ngay từ đầu,
 * để Agent 2-6 KHÔNG cần sửa file SHARED này khi bắt đầu code — chỉ dùng luôn.
 * Nếu phát sinh nhu cầu mã lỗi mới ngoài danh sách: thêm qua PR riêng, tag [shared],
 * xin Agent 1 review.
 */
public enum ErrorCode {

    // ==== Auth (Agent 1) ====
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    AUTH_FORBIDDEN_ROLE(HttpStatus.FORBIDDEN),
    AUTH_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),

    // ==== Profile (Agent 1) ====
    PROFILE_INVALID_AGE(HttpStatus.BAD_REQUEST),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND),

    // ==== Membership, Package & Payment (Agent 2) ====
    PACKAGE_NOT_FOUND(HttpStatus.NOT_FOUND),
    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND),
    MEMBERSHIP_EXPIRED(HttpStatus.BAD_REQUEST),
    MEMBERSHIP_NO_SESSION(HttpStatus.BAD_REQUEST),
    MEMBERSHIP_PENDING_EXISTS(HttpStatus.CONFLICT),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAYMENT_DUPLICATE_TRANSACTION(HttpStatus.CONFLICT),
    PAYMENT_WEBHOOK_INVALID_SIGNATURE(HttpStatus.BAD_REQUEST), // tuỳ chọn, chỉ dùng nếu bổ sung cổng thanh toán thật sau v1.0

    // ==== Lịch làm việc PT & Booking (Agent 3) ====
    SCHEDULE_TIME_INVALID(HttpStatus.BAD_REQUEST),
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND),
    BOOKING_SLOT_TAKEN(HttpStatus.CONFLICT),
    BOOKING_OUT_OF_WORKING_HOURS(HttpStatus.BAD_REQUEST),
    BOOKING_TOO_SOON(HttpStatus.BAD_REQUEST),
    BOOKING_DOUBLE_BOOKED(HttpStatus.CONFLICT),
    BOOKING_WEEKLY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST),
    BOOKING_PAST_CANNOT_CANCEL(HttpStatus.BAD_REQUEST),
    BOOKING_NOT_CHECKED_IN(HttpStatus.BAD_REQUEST),

    // ==== Face Attendance & Chấm công (Agent 4) ====
    FACE_NOT_RECOGNIZED(HttpStatus.UNAUTHORIZED),
    FACE_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    FACE_DESCRIPTOR_INVALID(HttpStatus.BAD_REQUEST),
    FACE_IMAGE_INVALID(HttpStatus.BAD_REQUEST), // dùng cho luồng đăng ký hộ bằng ảnh tĩnh

    // ==== POS & Commission (Agent 5) ====
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST),
    CLASS_NOT_FOUND(HttpStatus.NOT_FOUND),   // tuỳ chọn — chỉ dùng nếu làm thêm Group X
    CLASS_FULL(HttpStatus.CONFLICT),          // tuỳ chọn — chỉ dùng nếu làm thêm Group X

    // ==== Leads/CRM (Agent 6) ====
    LEAD_NOT_FOUND(HttpStatus.NOT_FOUND),
    LEAD_ALREADY_CONVERTED(HttpStatus.CONFLICT),
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR), // tuỳ chọn — Zalo ZNS/SMS sau v1.0

    // ==== Generic ====
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
