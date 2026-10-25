package com.gym.management.service.impl;

import com.gym.management.dto.request.RegisterRequest;
import com.gym.management.entity.Role;
import com.gym.management.entity.RoleName;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.RoleRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.security.JwtTokenProvider;
import com.gym.management.security.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplMemberAccountTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks private AuthServiceImpl authService;

    @Test
    void createMemberAccount_success_trimsPhoneAndAssignsMemberRole() {
        Role memberRole = mock(Role.class);
        when(userRepository.existsByEmail("a@gym.vn")).thenReturn(false);
        when(userRepository.existsByPhone("0901234567")).thenReturn(false);
        when(roleRepository.findByName(RoleName.MEMBER.name())).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode("Abcd1234")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = authService.createMemberAccount("Nguyen A", "a@gym.vn", "  0901234567  ", "Abcd1234");

        assertEquals("Nguyen A", user.getFullName());
        assertEquals("a@gym.vn", user.getEmail());
        assertEquals("0901234567", user.getPhone());
        assertEquals("hashed", user.getPasswordHash());
        assertTrue(user.getRoles().contains(memberRole));
    }

    @Test
    void createMemberAccount_blankPhone_becomesNullAndSkipsPhoneCheck() {
        Role memberRole = mock(Role.class);
        when(userRepository.existsByEmail("b@gym.vn")).thenReturn(false);
        when(roleRepository.findByName(RoleName.MEMBER.name())).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode("Abcd1234")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = authService.createMemberAccount("Nguyen B", "b@gym.vn", "   ", "Abcd1234");

        assertNull(user.getPhone());
        verify(userRepository, never()).existsByPhone(any());
    }

    @Test
    void createMemberAccount_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.existsByEmail("a@gym.vn")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.createMemberAccount("Nguyen A", "a@gym.vn", "0901234567", "Abcd1234"));

        assertEquals(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, ex.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createMemberAccount_duplicatePhone_throwsAndDoesNotSave() {
        when(userRepository.existsByEmail("c@gym.vn")).thenReturn(false);
        when(userRepository.existsByPhone("0909999999")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.createMemberAccount("Nguyen C", "c@gym.vn", "0909999999", "Abcd1234"));

        assertEquals(ErrorCode.AUTH_PHONE_ALREADY_EXISTS, ex.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_duplicatePhone_throws() {
        when(userRepository.existsByEmail("d@gym.vn")).thenReturn(false);
        when(userRepository.existsByPhone("0908888888")).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen D");
        request.setEmail("d@gym.vn");
        request.setPhone("0908888888");
        request.setPassword("Abcd1234");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request));

        assertEquals(ErrorCode.AUTH_PHONE_ALREADY_EXISTS, ex.getErrorCode());
    }
}
