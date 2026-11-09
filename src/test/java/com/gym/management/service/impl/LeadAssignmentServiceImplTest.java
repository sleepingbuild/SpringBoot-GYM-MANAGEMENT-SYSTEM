package com.gym.management.service.impl;

import com.gym.management.constant.LeadStatus;
import com.gym.management.dto.request.AssignLeadRequest;
import com.gym.management.dto.response.LeadResponse;
import com.gym.management.dto.response.SalesUserResponse;
import com.gym.management.entity.Lead;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.LeadRepository;
import com.gym.management.repository.SalesUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeadAssignmentServiceImplTest {

    @Mock LeadRepository leadRepository;
    @Mock SalesUserRepository salesUserRepository;
    @InjectMocks LeadAssignmentServiceImpl service;

    private final UUID salesA = UUID.randomUUID();
    private final UUID salesB = UUID.randomUUID();
    private final UUID leadId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Lead lead(LeadStatus status, UUID assigned) {
        Lead l = new Lead();
        l.setId(leadId);
        l.setFullName("Nguyễn Văn A");
        l.setPhone("0912345678");
        l.setStatus(status);
        l.setAssignedSalesId(assigned);
        when(leadRepository.findByIdForUpdate(leadId)).thenReturn(Optional.of(l));
        return l;
    }

    private AssignLeadRequest request(UUID salesId) {
        AssignLeadRequest r = new AssignLeadRequest();
        r.setSalesId(salesId);
        return r;
    }

    @Test
    void assign_giveLeadToActiveSales() {
        Lead l = lead(LeadStatus.NEW, null);
        when(salesUserRepository.isActiveSales(salesB)).thenReturn(true);

        LeadResponse res = service.assign(leadId, request(salesB));

        assertEquals(salesB, res.getAssignedSalesId());
        assertEquals(salesB, l.getAssignedSalesId());
    }

    @Test
    void assign_canMoveLeadFromOneSalesToAnother() {
        Lead l = lead(LeadStatus.CONTACTED, salesA);
        when(salesUserRepository.isActiveSales(salesB)).thenReturn(true);

        service.assign(leadId, request(salesB));

        assertEquals(salesB, l.getAssignedSalesId());
        assertEquals(LeadStatus.CONTACTED, l.getStatus());
    }

    @Test
    void assign_withNullSalesIdUnassignsLead() {
        Lead l = lead(LeadStatus.CONTACTED, salesA);

        LeadResponse res = service.assign(leadId, request(null));

        assertNull(res.getAssignedSalesId());
        assertNull(l.getAssignedSalesId());
        verify(salesUserRepository, never()).isActiveSales(any());
    }

    @Test
    void assign_rejectsUserWhoIsNotAnActiveSales() {
        Lead l = lead(LeadStatus.NEW, salesA);
        when(salesUserRepository.isActiveSales(salesB)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(leadId, request(salesB)));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        assertEquals(salesA, l.getAssignedSalesId());
        verify(leadRepository, never()).save(any());
    }

    @Test
    void assign_convertedLeadCannotBeReassigned() {
        lead(LeadStatus.CONVERTED, salesA);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(leadId, request(salesB)));
        assertEquals(ErrorCode.LEAD_ALREADY_CONVERTED, ex.getErrorCode());
        verify(leadRepository, never()).save(any());
    }

    @Test
    void assign_lostLeadCannotBeReassigned() {
        lead(LeadStatus.LOST, salesA);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(leadId, request(salesB)));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verify(leadRepository, never()).save(any());
    }

    @Test
    void assign_unknownLeadIsNotFound() {
        when(leadRepository.findByIdForUpdate(leadId)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(leadId, request(salesB)));
        assertEquals(ErrorCode.LEAD_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void listSalesUsers_returnsWhatRepositoryFinds() {
        SalesUserResponse a = new SalesUserResponse(salesA, "An", "0900000001");
        when(salesUserRepository.findActiveSales()).thenReturn(List.of(a));
        assertEquals(List.of(a), service.listSalesUsers());
    }
}
