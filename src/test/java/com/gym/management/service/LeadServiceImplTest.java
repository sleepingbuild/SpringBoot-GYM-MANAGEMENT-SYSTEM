package com.gym.management.service;

import com.gym.management.constant.LeadStatus;
import com.gym.management.dto.request.ChangeLeadStatusRequest;
import com.gym.management.dto.request.ConvertLeadRequest;
import com.gym.management.dto.request.LeadRequest;
import com.gym.management.dto.response.LeadResponse;
import com.gym.management.entity.Lead;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.LeadRepository;
import com.gym.management.security.CurrentUserId;
import com.gym.management.security.CurrentUserRole;
import com.gym.management.service.impl.LeadServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeadServiceImplTest {

    @Mock LeadRepository leadRepository;
    @Mock AuthService authService;
    @InjectMocks LeadServiceImpl service;

    private MockedStatic<CurrentUserId> idMock;
    private MockedStatic<CurrentUserRole> roleMock;

    private final UUID salesA = UUID.randomUUID();
    private final UUID salesB = UUID.randomUUID();
    private final UUID leadId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        idMock = Mockito.mockStatic(CurrentUserId.class);
        roleMock = Mockito.mockStatic(CurrentUserRole.class);
        when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        idMock.close();
        roleMock.close();
    }

    private void loginAs(UUID userId, boolean admin) {
        idMock.when(CurrentUserId::get).thenReturn(userId);
        roleMock.when(() -> CurrentUserRole.has("SUPER_ADMIN")).thenReturn(admin);
    }

    private Lead lead(LeadStatus status, UUID assigned) {
        Lead l = new Lead();
        l.setId(leadId);
        l.setFullName("Nguyễn Văn A");
        l.setPhone("0912345678");
        l.setStatus(status);
        l.setAssignedSalesId(assigned);
        when(leadRepository.findByIdForUpdate(leadId)).thenReturn(Optional.of(l));
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(l));
        return l;
    }

    private User userWithId(UUID id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private LeadRequest request(String phone) {
        LeadRequest r = new LeadRequest();
        r.setFullName("  Trần Thị B ");
        r.setPhone(phone);
        r.setSource("Facebook");
        return r;
    }

    private ConvertLeadRequest convertRequest() {
        ConvertLeadRequest r = new ConvertLeadRequest();
        r.setEmail(" Member@Example.com ");
        r.setPassword("tempPass123");
        return r;
    }

    private ChangeLeadStatusRequest statusRequest(String s) {
        ChangeLeadStatusRequest r = new ChangeLeadStatusRequest();
        r.setStatus(s);
        return r;
    }

    // ---------- tạo lead ----------

    @Test
    void create_normalizesPhoneAndAssignsSalesToSelf() {
        loginAs(salesA, false);
        LeadResponse res = service.create(request("+84 912-345-678"));
        assertEquals("0912345678", res.getPhone());
        assertEquals("Trần Thị B", res.getFullName());
        assertEquals("NEW", res.getStatus());
        assertEquals(salesA, res.getAssignedSalesId());
        assertFalse(res.getDuplicatePhone());
    }

    @Test
    void create_byAdminLeavesLeadUnassigned() {
        loginAs(UUID.randomUUID(), true);
        assertNull(service.create(request("0912345678")).getAssignedSalesId());
    }

    @Test
    void create_warnsWhenPhoneAlreadyExistsButStillCreates() {
        loginAs(salesA, false);
        when(leadRepository.existsByPhone("0912345678")).thenReturn(true);
        LeadResponse res = service.create(request("0912345678"));
        assertTrue(res.getDuplicatePhone());
        verify(leadRepository).save(any(Lead.class));
    }

    @Test
    void create_rejectsInvalidPhone() {
        loginAs(salesA, false);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request("12345")));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verify(leadRepository, never()).save(any());
    }

    // ---------- phân quyền ----------

    @Test
    void sales_cannotSeeLeadAssignedToSomeoneElse() {
        loginAs(salesA, false);
        lead(LeadStatus.NEW, salesB);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(leadId));
        assertEquals(ErrorCode.LEAD_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void sales_canSeeOwnAndUnassignedLeads() {
        loginAs(salesA, false);
        lead(LeadStatus.NEW, salesA);
        assertEquals(leadId, service.getById(leadId).getId());
        lead(LeadStatus.NEW, null);
        assertEquals(leadId, service.getById(leadId).getId());
    }

    @Test
    void admin_canSeeAnyLead() {
        loginAs(UUID.randomUUID(), true);
        lead(LeadStatus.NEW, salesB);
        assertEquals(salesB, service.getById(leadId).getAssignedSalesId());
    }

    @Test
    void sales_claimsUnassignedLeadWhenChangingStatus() {
        loginAs(salesA, false);
        lead(LeadStatus.NEW, null);
        LeadResponse res = service.changeStatus(leadId, statusRequest("contacted"));
        assertEquals("CONTACTED", res.getStatus());
        assertEquals(salesA, res.getAssignedSalesId());
    }

    @Test
    void sales_cannotChangeStatusOfOthersLead() {
        loginAs(salesA, false);
        lead(LeadStatus.NEW, salesB);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(leadId, statusRequest("CONTACTED")));
        assertEquals(ErrorCode.LEAD_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void search_rejectsUnsupportedSortProperty() {
        loginAs(salesA, false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.search(null, null, 0, 20, "passwordHash,asc"));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    // ---------- chuyển trạng thái ----------

    @Test
    void changeStatus_validTransition() {
        loginAs(salesA, false);
        lead(LeadStatus.CONTACTED, salesA);
        assertEquals("LOST", service.changeStatus(leadId, statusRequest("LOST")).getStatus());
    }

    @Test
    void changeStatus_invalidTransitionIsRejected() {
        loginAs(salesA, false);
        lead(LeadStatus.CONTACTED, salesA);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(leadId, statusRequest("NEW")));
        assertEquals(ErrorCode.LEAD_INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(leadRepository, never()).save(any());
    }

    @Test
    void changeStatus_cannotSetConvertedDirectly() {
        loginAs(salesA, false);
        lead(LeadStatus.CONTACTED, salesA);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(leadId, statusRequest("CONVERTED")));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void changeStatus_cannotLeaveConverted() {
        loginAs(salesA, false);
        lead(LeadStatus.CONVERTED, salesA);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(leadId, statusRequest("CONTACTED")));
        assertEquals(ErrorCode.LEAD_ALREADY_CONVERTED, ex.getErrorCode());
    }

    @Test
    void update_isRejectedForConvertedLead() {
        loginAs(salesA, false);
        lead(LeadStatus.CONVERTED, salesA);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(leadId, request("0912345678")));
        assertEquals(ErrorCode.LEAD_ALREADY_CONVERTED, ex.getErrorCode());
    }

    // ---------- convert ----------

    @Test
    void convert_success_createsMemberAndMarksConverted() {
        loginAs(salesA, false);
        Lead l = lead(LeadStatus.CONTACTED, salesA);
        UUID newUserId = UUID.randomUUID();
        when(authService.createMemberAccount("Nguyễn Văn A", "member@example.com", "0912345678", "tempPass123"))
                .thenReturn(userWithId(newUserId));

        LeadResponse res = service.convert(leadId, convertRequest());

        assertEquals("CONVERTED", res.getStatus());
        assertEquals(newUserId, res.getConvertedUserId());
        assertEquals(LeadStatus.CONVERTED, l.getStatus());
        assertNull(res.getTemporaryPassword());
    }

    @Test
    void convert_generatesRandomPasswordWhenOmitted() {
        loginAs(salesA, false);
        lead(LeadStatus.CONTACTED, salesA);
        when(authService.createMemberAccount(any(), any(), any(), any()))
                .thenReturn(userWithId(UUID.randomUUID()));
        ConvertLeadRequest req = new ConvertLeadRequest();
        req.setEmail("member@example.com");

        LeadResponse res = service.convert(leadId, req);

        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(authService).createMemberAccount(any(), any(), any(), password.capture());
        assertEquals(12, password.getValue().length());
        assertEquals(password.getValue(), res.getTemporaryPassword());
    }

    @Test
    void convert_fromNewIsAllowed() {
        loginAs(salesA, false);
        lead(LeadStatus.NEW, salesA);
        when(authService.createMemberAccount(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(userWithId(UUID.randomUUID()));
        assertEquals("CONVERTED", service.convert(leadId, convertRequest()).getStatus());
    }

    @Test
    void convert_alreadyConvertedThrowsAndCreatesNoAccount() {
        loginAs(salesA, false);
        lead(LeadStatus.CONVERTED, salesA);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.convert(leadId, convertRequest()));
        assertEquals(ErrorCode.LEAD_ALREADY_CONVERTED, ex.getErrorCode());
        verify(authService, never()).createMemberAccount(any(), any(), any(), any());
    }

    @Test
    void convert_lostLeadCannotBeConverted() {
        loginAs(salesA, false);
        lead(LeadStatus.LOST, salesA);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.convert(leadId, convertRequest()));
        assertEquals(ErrorCode.LEAD_INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(authService, never()).createMemberAccount(any(), any(), any(), any());
    }

    @Test
    void convert_duplicateEmailPropagatesAndLeavesLeadUntouched() {
        loginAs(salesA, false);
        Lead l = lead(LeadStatus.CONTACTED, salesA);
        when(authService.createMemberAccount(any(), eq("member@example.com"), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, "Email đã được sử dụng"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.convert(leadId, convertRequest()));

        assertEquals(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, ex.getErrorCode());
        assertEquals(LeadStatus.CONTACTED, l.getStatus());
        assertNull(l.getConvertedUserId());
        verify(leadRepository, never()).save(any());
    }

    @Test
    void convert_salesCannotConvertOthersLead() {
        loginAs(salesA, false);
        lead(LeadStatus.CONTACTED, salesB);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.convert(leadId, convertRequest()));
        assertEquals(ErrorCode.LEAD_NOT_FOUND, ex.getErrorCode());
        verify(authService, never()).createMemberAccount(any(), any(), any(), any());
    }
}