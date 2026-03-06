package org.project.ttokttok.domain.applyform.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormCreateServiceRequest;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormUpdateServiceRequest;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.temp.applyform.repository.TempApplyFormRepository;
import org.project.ttokttok.global.auth.ClubHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplyFormAdminServiceTest {

    @Mock
    private ApplyFormRepository applyFormRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private TempApplyFormRepository tempApplyFormRepository;

    @InjectMocks
    private ApplyFormAdminService applyFormAdminService;

    @Test
    @DisplayName("createApplyForm(): 지원 폼 생성 성공")
    void createApplyFormSuccess() {
        // given
        String clubId = "club123";
        Club mockClub = mock(Club.class);
        when(mockClub.getId()).thenReturn(clubId);

        ApplyFormCreateServiceRequest request = ApplyFormCreateServiceRequest.builder()
                .clubId(clubId)
                .username("adminUser")
                .recruitStartDate(LocalDate.now())
                .recruitEndDate(LocalDate.now().plusDays(7))
                .applicableGrades(Set.of(1, 2))
                .maxApplyCount(10)
                .title("테스트 지원서")
                .questions(List.of())
                .build();

        when(applyFormRepository.existsByClubIdAndStatus(eq(clubId), any())).thenReturn(false);
        when(tempApplyFormRepository.findByClubId(clubId)).thenReturn(Optional.empty());
        
        ApplyForm mockSavedForm = mock(ApplyForm.class);
        when(mockSavedForm.getId()).thenReturn("form123");
        when(applyFormRepository.save(any(ApplyForm.class))).thenReturn(mockSavedForm);

        // AOP를 대신하여 수동으로 ClubHolder 설정
        ClubHolder.setClub(mockClub);

        try {
            // when
            String result = applyFormAdminService.createApplyForm(request);

            // then
            assertThat(result).isEqualTo("form123");
            verify(applyFormRepository).save(any(ApplyForm.class));
            verify(tempApplyFormRepository).findByClubId(clubId);
        } finally {
            ClubHolder.clear();
        }
    }

    @Test
    @DisplayName("updateApplyForm(): 지원 폼 수정 성공")
    void updateApplyFormSuccess() {
        // given
        String formId = "form123";
        Club mockClub = mock(Club.class);
        ApplyForm mockApplyForm = mock(ApplyForm.class);
        
        when(applyFormRepository.findById(formId)).thenReturn(Optional.of(mockApplyForm));
        
        ApplyFormUpdateServiceRequest request = ApplyFormUpdateServiceRequest.builder()
                .applyFormId(formId)
                .title(JsonNullable.of("새 제목"))
                .subTitle(JsonNullable.undefined())
                .questions(JsonNullable.undefined())
                .build();

        // AOP를 대신하여 수동으로 ClubHolder 설정
        ClubHolder.setClub(mockClub);

        try {
            // when
            applyFormAdminService.updateApplyForm(request);

            // then
            verify(mockApplyForm).updateFormContent(eq("새 제목"), isNull(), isNull());
        } finally {
            ClubHolder.clear();
        }
    }
}