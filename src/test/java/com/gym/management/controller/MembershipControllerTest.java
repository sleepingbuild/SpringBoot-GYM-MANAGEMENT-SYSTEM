package com.gym.management.controller;

import com.gym.management.service.MembershipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * H2 (frontend đợt 1): Lễ tân/Admin tra cứu gói của hội viên khác.
 * Logic nghiệp vụ nằm ở MembershipService (đã có test); ở đây chỉ chốt hai điều: controller truyền ĐÚNG
 * userId của người được tra cứu (không phải của người đang đăng nhập) và chỉ staff được gọi.
 */
@ExtendWith(MockitoExtension.class)
class MembershipControllerTest {

    private static final String STAFF_ONLY = "hasAnyRole('RECEPTIONIST','SUPER_ADMIN')";

    @Mock
    private MembershipService membershipService;

    @Test
    void getCurrentMembershipOf_shouldLookUpTheRequestedUser() {
        MembershipController controller = new MembershipController(membershipService);
        UUID userId = UUID.randomUUID();

        controller.getCurrentMembershipOf(userId);

        verify(membershipService).getCurrentMembershipResponse(userId);
        verifyNoMoreInteractions(membershipService);
    }

    @Test
    void getHistoryOf_shouldLookUpTheRequestedUserWithPaging() {
        MembershipController controller = new MembershipController(membershipService);
        UUID userId = UUID.randomUUID();

        controller.getHistoryOf(userId, 2, 10, "createdAt,desc");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(membershipService).getHistory(eq(userId), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        verifyNoMoreInteractions(membershipService);
    }

    @Test
    void lookupEndpointsOfOtherMembers_shouldBeRestrictedToStaff() throws NoSuchMethodException {
        Method current = MembershipController.class.getMethod("getCurrentMembershipOf", UUID.class);
        Method history = MembershipController.class.getMethod(
                "getHistoryOf", UUID.class, int.class, int.class, String.class);

        assertThat(current.getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ONLY);
        assertThat(history.getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ONLY);
    }

    @Test
    void ownEndpoints_shouldStayAvailableToMembers() throws NoSuchMethodException {
        Method me = MembershipController.class.getMethod("getMyCurrentMembership");

        assertThat(me.getAnnotation(PreAuthorize.class).value()).contains("'MEMBER'");
    }
}
