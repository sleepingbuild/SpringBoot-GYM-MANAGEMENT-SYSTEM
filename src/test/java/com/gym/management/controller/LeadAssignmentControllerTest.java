package com.gym.management.controller;

import com.gym.management.dto.request.AssignLeadRequest;
import com.gym.management.dto.response.LeadResponse;
import com.gym.management.dto.response.SalesUserResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.LeadAssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadAssignmentControllerTest {

    @Mock private LeadAssignmentService leadAssignmentService;
    @InjectMocks private LeadAssignmentController controller;

    @Test
    void salesUsers_returnsWhatTheServiceListsAndIsSuperAdminOnly() throws NoSuchMethodException {
        List<SalesUserResponse> sales = List.of(new SalesUserResponse(UUID.randomUUID(), "An", "0900000001"));
        when(leadAssignmentService.listSalesUsers()).thenReturn(sales);

        ApiResponse<List<SalesUserResponse>> response = controller.salesUsers();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(sales);
        assertThat(LeadAssignmentController.class.getMethod("salesUsers").getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasRole('SUPER_ADMIN')");
    }

    @Test
    void assign_passesIdAndRequestToTheServiceAndIsSuperAdminOnly() throws NoSuchMethodException {
        UUID leadId = UUID.randomUUID();
        AssignLeadRequest request = new AssignLeadRequest();
        request.setSalesId(UUID.randomUUID());
        LeadResponse lead = new LeadResponse();
        when(leadAssignmentService.assign(leadId, request)).thenReturn(lead);

        ApiResponse<LeadResponse> response = controller.assign(leadId, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(lead);
        verify(leadAssignmentService).assign(leadId, request);
        assertThat(LeadAssignmentController.class.getMethod("assign", UUID.class, AssignLeadRequest.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SUPER_ADMIN')");
    }
}
