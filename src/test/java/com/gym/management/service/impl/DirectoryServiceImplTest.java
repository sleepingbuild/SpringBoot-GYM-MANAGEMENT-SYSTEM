package com.gym.management.service.impl;

import com.gym.management.dto.response.BranchResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.dto.response.TrainerResponse;
import com.gym.management.dto.response.UserSummaryResponse;
import com.gym.management.entity.Branch;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.BranchRepository;
import com.gym.management.repository.UserDirectoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectoryServiceImplTest {

    @Mock private UserDirectoryRepository userDirectoryRepository;
    @Mock private BranchRepository branchRepository;

    @InjectMocks private DirectoryServiceImpl service;

    private User member(String name, String email, String phone) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setFullName(name);
        u.setEmail(email);
        u.setPhone(phone);
        return u;
    }

    @Test
    void searchMembers_rejectsRolesOtherThanMember() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.searchMembers("TRAINER", "a", 0, 20));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verifyNoInteractions(userDirectoryRepository);
    }

    @Test
    void searchMembers_mapsUsersAndKeepsOnlyPublicFields() {
        User u = member("Nguyen A", "a@gym.test", "0901234567");
        when(userDirectoryRepository.searchActiveByRole(eq("MEMBER"), anyString(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(u), inv.getArgument(2), 1));

        PageResponse<UserSummaryResponse> res = service.searchMembers("member", null, 0, 20);

        assertEquals(1, res.getContent().size());
        assertEquals(new UserSummaryResponse(u.getId(), "Nguyen A", "0901234567", "a@gym.test"), res.getContent().get(0));
        assertEquals(1, res.getTotalElements());
    }

    @Test
    void searchMembers_trimsAndEscapesLikeWildcards() {
        when(userDirectoryRepository.searchActiveByRole(anyString(), anyString(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<User>(List.of(), inv.getArgument(2), 0));

        service.searchMembers("MEMBER", "  50%_a[b!  ", 0, 20);

        ArgumentCaptor<String> q = ArgumentCaptor.forClass(String.class);
        verify(userDirectoryRepository).searchActiveByRole(eq("MEMBER"), q.capture(), any(Pageable.class));
        assertEquals("50!%!_a![b!!", q.getValue());
    }

    @Test
    void searchMembers_nullQueryBecomesEmptyString() {
        when(userDirectoryRepository.searchActiveByRole(anyString(), anyString(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<User>(List.of(), inv.getArgument(2), 0));

        service.searchMembers("MEMBER", null, 0, 20);

        verify(userDirectoryRepository).searchActiveByRole(eq("MEMBER"), eq(""), any(Pageable.class));
    }

    @Test
    void searchMembers_clampsPageSizeAndPageAndFixesSortOrder() {
        when(userDirectoryRepository.searchActiveByRole(anyString(), anyString(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<User>(List.of(), inv.getArgument(2), 0));

        service.searchMembers("MEMBER", "", -3, 500);

        ArgumentCaptor<Pageable> p = ArgumentCaptor.forClass(Pageable.class);
        verify(userDirectoryRepository).searchActiveByRole(anyString(), anyString(), p.capture());
        assertEquals(0, p.getValue().getPageNumber());
        assertEquals(50, p.getValue().getPageSize());
        assertEquals(Sort.by("fullName").ascending().and(Sort.by("id").ascending()), p.getValue().getSort());
    }

    @Test
    void searchMembers_truncatesVeryLongQuery() {
        when(userDirectoryRepository.searchActiveByRole(anyString(), anyString(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<User>(List.of(), inv.getArgument(2), 0));

        service.searchMembers("MEMBER", "x".repeat(500), 0, 20);

        ArgumentCaptor<String> q = ArgumentCaptor.forClass(String.class);
        verify(userDirectoryRepository).searchActiveByRole(anyString(), q.capture(), any(Pageable.class));
        assertEquals(100, q.getValue().length());
    }

    @Test
    void listTrainers_returnsRepositoryResultForTrainerRole() {
        List<TrainerResponse> trainers = List.of(new TrainerResponse(UUID.randomUUID(), "PT Một", null));
        when(userDirectoryRepository.findActiveByRole("TRAINER")).thenReturn(trainers);

        assertEquals(trainers, service.listTrainers());
    }

    @Test
    void listBranches_keepsOnlyActiveBranches() {
        Branch open = new Branch();
        open.setId(UUID.randomUUID());
        open.setName("Chi nhánh 1");
        Branch closed = new Branch();
        closed.setId(UUID.randomUUID());
        closed.setName("Chi nhánh cũ");
        closed.setStatus("CLOSED");
        when(branchRepository.findAll(any(Sort.class))).thenReturn(List.of(open, closed));

        List<BranchResponse> res = service.listBranches();

        assertEquals(List.of(new BranchResponse(open.getId(), "Chi nhánh 1")), res);
        verify(branchRepository, never()).findAll();
    }

    @Test
    void escapeLike_escapesBangFirstThenWildcards() {
        assertEquals("a!!b!%c!_d![e", DirectoryServiceImpl.escapeLike("a!b%c_d[e"));
    }
}
