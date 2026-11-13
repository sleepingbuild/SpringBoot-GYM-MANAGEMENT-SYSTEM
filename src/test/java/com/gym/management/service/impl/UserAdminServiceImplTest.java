package com.gym.management.service.impl;

import com.gym.management.dto.request.AdminUserCreateRequest;
import com.gym.management.dto.request.AdminUserUpdateRequest;
import com.gym.management.dto.response.AdminUserResponse;
import com.gym.management.entity.Role;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.RoleRepository;
import com.gym.management.repository.UserAdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    @Mock private UserAdminRepository userAdminRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserAdminServiceImpl service;

    private static Role role(String name) {
        Role r = new Role();
        r.setName(name);
        return r;
    }

    private static User user(UUID id, String name, String email, String status, String... roleNames) {
        User u = new User();
        u.setId(id);
        u.setFullName(name);
        u.setEmail(email);
        u.setStatus(status);
        for (String roleName : roleNames) {
            u.getRoles().add(role(roleName));
        }
        return u;
    }

    private static AdminUserCreateRequest createRequest(Set<String> roles) {
        AdminUserCreateRequest r = new AdminUserCreateRequest();
        r.setFullName("  Nguyen Van A ");
        r.setEmail(" a@gym.local ");
        r.setPhone("  ");
        r.setPassword("Password123");
        r.setRoles(roles);
        return r;
    }

    @Test
    void search_rejectsUnknownGroup() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.search("ADMINS", "", "", 0, 20));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verifyNoInteractions(userAdminRepository);
    }

    @Test
    void search_rejectsUnknownStatus() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.search("STAFF", "", "BANNED", 0, 20));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verifyNoInteractions(userAdminRepository);
    }

    @Test
    void createStaff_rejectsMemberRole() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createStaff(createRequest(Set.of("MEMBER"))));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verify(userAdminRepository, never()).save(any());
    }

    @Test
    void createStaff_rejectsUnknownRole() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createStaff(createRequest(Set.of("BOSS"))));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void createStaff_rejectsDuplicateEmail() {
        when(userAdminRepository.existsByEmail("a@gym.local")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createStaff(createRequest(Set.of("TRAINER"))));

        assertEquals(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, ex.getErrorCode());
        verify(userAdminRepository, never()).save(any());
    }

    @Test
    void createStaff_savesTrimmedAccountWithHashedPasswordAndRoles() {
        when(userAdminRepository.existsByEmail("a@gym.local")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hash");
        when(roleRepository.findByName("TRAINER")).thenReturn(Optional.of(role("TRAINER")));
        when(userAdminRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse response = service.createStaff(createRequest(Set.of(" trainer ")));

        assertEquals("Nguyen Van A", response.fullName());
        assertEquals("a@gym.local", response.email());
        assertNull(response.phone());
        assertEquals(List.of("TRAINER"), response.roles());
    }

    @Test
    void update_rejectsPhoneUsedByAnotherAccount() {
        UUID id = UUID.randomUUID();
        when(userAdminRepository.findById(id)).thenReturn(Optional.of(user(id, "A", "a@gym.local", "ACTIVE", "MEMBER")));
        when(userAdminRepository.existsByPhoneAndIdNot("0900000001", id)).thenReturn(true);
        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setFullName("A");
        request.setPhone("0900000001");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(id, request));

        assertEquals(ErrorCode.AUTH_PHONE_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void setRoles_notFound() {
        UUID id = UUID.randomUUID();
        when(userAdminRepository.findById(id)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.setRoles(UUID.randomUUID(), id, Set.of("TRAINER")));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void setRoles_rejectsChangingOwnRoles() {
        UUID me = UUID.randomUUID();
        when(userAdminRepository.findById(me)).thenReturn(Optional.of(user(me, "Admin", "admin@gym.local", "ACTIVE", "SUPER_ADMIN")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.setRoles(me, me, Set.of("TRAINER")));

        assertEquals(ErrorCode.USER_SELF_CHANGE_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void setRoles_rejectsMemberMixedWithStaffRole() {
        UUID id = UUID.randomUUID();
        when(userAdminRepository.findById(id)).thenReturn(Optional.of(user(id, "A", "a@gym.local", "ACTIVE", "MEMBER")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.setRoles(UUID.randomUUID(), id, Set.of("MEMBER", "TRAINER")));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void setRoles_replacesRolesOfTheTargetAccount() {
        UUID id = UUID.randomUUID();
        when(userAdminRepository.findById(id)).thenReturn(Optional.of(user(id, "A", "a@gym.local", "ACTIVE", "RECEPTIONIST")));
        when(roleRepository.findByName("SALES")).thenReturn(Optional.of(role("SALES")));

        AdminUserResponse response = service.setRoles(UUID.randomUUID(), id, Set.of("SALES"));

        assertEquals(List.of("SALES"), response.roles());
    }

    @Test
    void setStatus_rejectsLockingOwnAccount() {
        UUID me = UUID.randomUUID();
        when(userAdminRepository.findById(me)).thenReturn(Optional.of(user(me, "Admin", "admin@gym.local", "ACTIVE", "SUPER_ADMIN")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.setStatus(me, me, "INACTIVE"));

        assertEquals(ErrorCode.USER_SELF_CHANGE_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void setStatus_rejectsUnknownStatus() {
        UUID id = UUID.randomUUID();
        when(userAdminRepository.findById(id)).thenReturn(Optional.of(user(id, "A", "a@gym.local", "ACTIVE", "MEMBER")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.setStatus(UUID.randomUUID(), id, "BANNED"));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void setStatus_locksAndUnlocksAnotherAccount() {
        UUID id = UUID.randomUUID();
        User target = user(id, "A", "a@gym.local", "ACTIVE", "MEMBER");
        when(userAdminRepository.findById(id)).thenReturn(Optional.of(target));

        assertEquals("INACTIVE", service.setStatus(UUID.randomUUID(), id, "inactive").status());
        assertEquals("ACTIVE", service.setStatus(UUID.randomUUID(), id, "ACTIVE").status());
    }
}
