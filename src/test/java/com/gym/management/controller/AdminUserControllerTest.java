package com.gym.management.controller;

import com.gym.management.dto.request.AdminUserCreateRequest;
import com.gym.management.dto.request.AdminUserRolesRequest;
import com.gym.management.dto.request.AdminUserStatusRequest;
import com.gym.management.dto.request.AdminUserUpdateRequest;
import com.gym.management.dto.response.AdminUserResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.UserAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock private UserAdminService userAdminService;
    @InjectMocks private AdminUserController controller;

    private final AdminUserResponse sample =
            new AdminUserResponse(UUID.randomUUID(), "An", "an@gym.local", null, "ACTIVE", List.of("TRAINER"));

    @Test
    void everyEndpointIsSuperAdminOnly() {
        List<Method> endpoints = Arrays.stream(AdminUserController.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(GetMapping.class) || m.isAnnotationPresent(PostMapping.class)
                        || m.isAnnotationPresent(PutMapping.class))
                .toList();

        assertThat(endpoints).hasSize(5);
        for (Method m : endpoints) {
            assertThat(m.getAnnotation(PreAuthorize.class).value()).as(m.getName()).isEqualTo("hasRole('SUPER_ADMIN')");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_forwardsAllFilters() {
        PageResponse<AdminUserResponse> page = mock(PageResponse.class);
        when(userAdminService.search("STAFF", "an", "ACTIVE", 1, 10)).thenReturn(page);

        ApiResponse<PageResponse<AdminUserResponse>> response = controller.search("STAFF", "an", "ACTIVE", 1, 10);

        assertThat(response.getData()).isSameAs(page);
    }

    @Test
    void createStaff_delegatesToTheService() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        when(userAdminService.createStaff(request)).thenReturn(sample);

        assertThat(controller.createStaff(request).getData()).isSameAs(sample);
    }

    @Test
    void update_delegatesToTheService() {
        UUID id = UUID.randomUUID();
        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        when(userAdminService.update(id, request)).thenReturn(sample);

        assertThat(controller.update(id, request).getData()).isSameAs(sample);
    }

    @Test
    void setRoles_passesTheCallerAsActor() {
        UUID actor = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        AdminUserRolesRequest request = new AdminUserRolesRequest();
        request.setRoles(Set.of("TRAINER"));
        when(userAdminService.setRoles(actor, id, Set.of("TRAINER"))).thenReturn(sample);

        assertThat(controller.setRoles(actor, id, request).getData()).isSameAs(sample);
        verify(userAdminService).setRoles(actor, id, Set.of("TRAINER"));
    }

    @Test
    void setStatus_passesTheCallerAsActor() {
        UUID actor = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        AdminUserStatusRequest request = new AdminUserStatusRequest();
        request.setStatus("INACTIVE");
        when(userAdminService.setStatus(actor, id, "INACTIVE")).thenReturn(sample);

        assertThat(controller.setStatus(actor, id, request).getData()).isSameAs(sample);
    }
}
