package com.gym.management.exception;

import org.springframework.http.HttpStatus;

/**
 * Agent 1 - SHARED FILE.
 * Danh sách mã lỗi chuẩn hoá toàn hệ thống — xem bảng đầy đủ trong API_DESIGN.md.
 * Mỗi agent khi cần mã lỗi mới cho module của mình: thêm vào enum này qua PR
 * riêng có tag [shared], KHÔNG tự tạo enum lỗi cục bộ trong module.
 */
public enum ErrorCode {

    // ==== Auth (Agent 1) ====
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    AUTH_FORBIDDEN_ROLE(HttpStatus.FORBIDDEN),
    AUTH_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),

    // ==== Membership & Package (Agent 2) ====
    PACKAGE_NOT_FOUND(HttpStatus.NOT_FOUND),
    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND),
    MEMBERSHIP_EXPIRED(HttpStatus.BAD_REQUEST),
    MEMBERSHIP_NO_SESSION(HttpStatus.BAD_REQUEST),
    MEMBERSHIP_ALREADY_FROZEN(HttpStatus.BAD_REQUEST),
    MEMBERSHIP_FREEZE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST),

    // ==== Check-in (Agent 3) ====
    CHECKIN_USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    CHECKIN_NO_ACTIVE_PACKAGE(HttpStatus.BAD_REQUEST),
    CHECKIN_OUT_OF_TIME_RANGE(HttpStatus.BAD_REQUEST),
    CHECKIN_ANTI_PASSBACK(HttpStatus.BAD_REQUEST),
    FACE_NOT_RECOGNIZED(HttpStatus.UNAUTHORIZED),
    FACE_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    FACE_PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    FACE_IMAGE_INVALID(HttpStatus.BAD_REQUEST),

    // ==== Payment & POS (Agent 4) ====
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAYMENT_DUPLICATE_TRANSACTION(HttpStatus.CONFLICT),
    PAYMENT_WEBHOOK_INVALID_SIGNATURE(HttpStatus.BAD_REQUEST),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST),

    // ==== PT & Group X (Agent 5) ====
    PT_SCHEDULE_CONFLICT(HttpStatus.CONFLICT),
    PT_BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND),
    CLASS_NOT_FOUND(HttpStatus.NOT_FOUND),
    CLASS_FULL(HttpStatus.CONFLICT),

    // ==== Leads / Notification (Agent 6) ====
    LEAD_NOT_FOUND(HttpStatus.NOT_FOUND),
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR),

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
