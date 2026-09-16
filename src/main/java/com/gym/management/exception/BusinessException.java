package com.gym.management.exception;

import lombok.Getter;

/**
 * Agent 1 - SHARED FILE.
 * Exception nghiệp vụ dùng chung — mọi agent ném exception này thay vì
 * RuntimeException trần, để GlobalExceptionHandler trả đúng ApiResponse + HTTP status.
 *
 * Ví dụ: throw new BusinessException(ErrorCode.MEMBERSHIP_EXPIRED, "Gói tập đã hết hạn ngày 12/09/2026");
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
