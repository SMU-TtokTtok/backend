package org.project.ttokttok.domain.applyform.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormCreateServiceRequest;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplyFormAdminServiceTest {

    @Mock
    private ApplyFormRepository applyFormRepository;

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private ApplyFormAdminService applyFormAdminService;

//    @Test
//    @DisplayName("createApplyForm(): 지원 폼을 성공적으로 생성")
//    void createApplyForm_Success() {
//
//    }

    @Test
    @DisplayName("createApplyForm(): 클럽이 존재하지 않으면 예외 발생")
    void createApplyForm_ClubNotFound() {
        // given
        String clubId = "club123";

        when(clubRepository.findById(clubId)).thenReturn(Optional.empty());

        ApplyFormCreateServiceRequest request = mock(ApplyFormCreateServiceRequest.class);
        when(request.clubId()).thenReturn(clubId);

        // when, then
        assertThrows(ClubNotFoundException.class, () -> {
            applyFormAdminService.createApplyForm(request);
        });
    }

    @Test
    @DisplayName("createApplyForm(): 관리자가 아니라면 예외가 발생한다.")
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
        assertThrows(NotClubAdminException.class, () -> {
            applyFormAdminService.createApplyForm(request);
        });
    }
}