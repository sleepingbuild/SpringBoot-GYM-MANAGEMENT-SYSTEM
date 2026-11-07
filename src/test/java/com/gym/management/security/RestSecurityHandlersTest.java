package com.gym.management.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestSecurityHandlersTest {

    // findAndRegisterModules: ApiResponse có trường Instant nên cần module java.time như ObjectMapper của Spring Boot
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void entryPoint_returns401WithJsonBodyAndTokenInvalidCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(mapper)
                .commence(new MockHttpServletRequest(), response, new BadCredentialsException("x"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = mapper.readTree(response.getContentAsString());
        assertFalse(body.get("success").asBoolean());
        assertEquals("AUTH_TOKEN_INVALID", body.get("errorCode").asText());
        assertEquals("Chưa đăng nhập hoặc phiên đã hết hạn", body.get("message").asText());
    }

    @Test
    void accessDeniedHandler_returns403WithJsonBodyAndForbiddenRoleCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(mapper)
                .handle(new MockHttpServletRequest(), response, new AccessDeniedException("x"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = mapper.readTree(response.getContentAsString());
        assertFalse(body.get("success").asBoolean());
        assertEquals("AUTH_FORBIDDEN_ROLE", body.get("errorCode").asText());
    }
}
