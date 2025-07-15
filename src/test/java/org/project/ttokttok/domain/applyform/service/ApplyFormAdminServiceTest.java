package org.project.ttokttok.domain.applyform.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormCreateServiceRequest;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyFormAdminServiceTest {

    @Mock
    private ApplyFormRepository applyFormRepository;

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private ApplyFormAdminService applyFormAdminService;

    @Test
    @DisplayName("createApplyForm(): 지원 폼 생성 성공")
    void createApplyFormSuccess() {

    }

    @Test
    @DisplayName("createApplyForm(): 클럽이 존재하지 않으면 예외 발생")
    void createApplyForm_ClubNotFound() {
        // given
        String clubId = "club123";

        when(clubRepository.findById(clubId)).thenReturn(Optional.empty());

        ApplyFormCreateServiceRequest request = mock(ApplyFormCreateServiceRequest.class);
        when(request.clubId()).thenReturn(clubId);

        // when, then
        assertThatThrownBy(() -> applyFormAdminService.createApplyForm(request))
                .isInstanceOf(ClubNotFoundException.class);
    }

    @Test
    @DisplayName("createApplyForm(): 관리자가 아니라면 예외 발생")
    void createApplyForm_NotClubAdmin() {
        // given
        String clubId = "club123";
        String adminUsername = "adminUser";
        String requestUsername = "requestUser"; // 다른 사용자명

        Admin admin = mock(Admin.class);
        when(admin.getUsername()).thenReturn(adminUsername);

        Club club = mock(Club.class);
        when(club.getAdmin()).thenReturn(admin);

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));

        ApplyFormCreateServiceRequest request = mock(ApplyFormCreateServiceRequest.class);
        when(request.clubId()).thenReturn(clubId);
        when(request.username()).thenReturn(requestUsername);

        // when, then
        assertThatThrownBy(() -> applyFormAdminService.createApplyForm(request))
                .isInstanceOf(NotClubAdminException.class);
    }

    @Test
    @DisplayName("updateApplyForm(): 지원 폼 생성 시 필수 필드가 누락되면 예외 발생")
    void updateApplyFormSuccess() {

    }
}