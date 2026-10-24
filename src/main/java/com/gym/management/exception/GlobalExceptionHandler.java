package com.gym.management.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Agent 1 - SHARED FILE.
 * Bắt toàn bộ exception ném ra từ Controller/Service của mọi module,
 * chuẩn hoá thành ApiResponse (xem API_DESIGN.md).
 * Các agent KHÔNG tự viết @ExceptionHandler cục bộ trong controller của mình
 * trừ khi thực sự cần xử lý đặc thù không thể generic hoá ở đây.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, ErrorCode.VALIDATION_ERROR));
    }

    /** Body JSON sai cú pháp, thiếu body, hoặc sai kiểu dữ liệu (vd: chữ vào trường số). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Request body không đọc được: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Dữ liệu gửi lên không hợp lệ hoặc sai định dạng", ErrorCode.VALIDATION_ERROR));
    }

    /** Tham số URL/query sai kiểu (vd: id không phải UUID, ngày sai định dạng). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Tham số '{}' sai kiểu: {}", ex.getName(), ex.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Tham số '" + ex.getName() + "' có giá trị không hợp lệ", ErrorCode.VALIDATION_ERROR));
    }

    /** Vi phạm ràng buộc validation khi dùng @Validated ở class hoặc tham số. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, ErrorCode.VALIDATION_ERROR));
    }

    /** Vi phạm unique/khoá ngoại ở DB (vd: hai request cùng lúc chèn trùng). Trả 409, không lộ chi tiết SQL. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Dữ liệu bị trùng hoặc vi phạm ràng buộc", "DATA_CONFLICT"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(ErrorCode.AUTH_INVALID_CREDENTIALS.getHttpStatus())
                .body(ApiResponse.error("Email hoặc mật khẩu không đúng", ErrorCode.AUTH_INVALID_CREDENTIALS));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(ErrorCode.FACE_IMAGE_INVALID.getHttpStatus())
                .body(ApiResponse.error("File tải lên vượt quá dung lượng cho phép", ErrorCode.FACE_IMAGE_INVALID));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(ErrorCode.AUTH_FORBIDDEN_ROLE.getHttpStatus())
                .body(ApiResponse.error("Bạn không có quyền thực hiện thao tác này", ErrorCode.AUTH_FORBIDDEN_ROLE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Đã có lỗi xảy ra, vui lòng thử lại sau", ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
