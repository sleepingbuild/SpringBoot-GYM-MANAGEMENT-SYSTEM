package com.gym.management.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingRequestParameter_returns400WithValidationErrorCode() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleMissingParameter(new MissingServletRequestParameterException("date", "LocalDate"));

        assertEquals(400, res.getStatusCode().value());
        assertFalse(res.getBody().isSuccess());
        assertEquals("VALIDATION_ERROR", res.getBody().getErrorCode());
        assertTrue(res.getBody().getMessage().contains("date"));
    }

    @Test
    void unknownException_stillReturns500() {
        ResponseEntity<ApiResponse<Void>> res = handler.handleGenericException(new RuntimeException("boom"));

        assertEquals(500, res.getStatusCode().value());
        assertEquals("INTERNAL_SERVER_ERROR", res.getBody().getErrorCode());
    }
}
