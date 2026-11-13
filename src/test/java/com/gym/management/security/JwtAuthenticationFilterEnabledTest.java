package com.gym.management.security;

import com.gym.management.repository.UserRepository;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tài khoản bị khoá (INACTIVE) thì token còn hạn cũng không được xác thực. */
class JwtAuthenticationFilterEnabledTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenBlacklistService blacklist = mock(TokenBlacklistService.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, userRepository, blacklist);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void runFilterWithToken(boolean enabled) throws ServletException, IOException {
        when(jwtTokenProvider.validateToken("t")).thenReturn(true);
        when(blacklist.isBlacklisted("t")).thenReturn(false);
        when(jwtTokenProvider.getEmailFromToken("t")).thenReturn("a@gym.local");
        when(userDetailsService.loadUserByUsername("a@gym.local"))
                .thenReturn(new GymUserDetails(UUID.randomUUID(), "a@gym.local", "hash", enabled, List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer t");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void lockedAccountIsNotAuthenticated() throws Exception {
        runFilterWithToken(false);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void activeAccountIsAuthenticated() throws Exception {
        runFilterWithToken(true);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
