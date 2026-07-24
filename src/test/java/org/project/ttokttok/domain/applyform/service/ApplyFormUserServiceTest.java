package org.project.ttokttok.domain.applyform.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.exception.ActiveApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.applyform.service.dto.response.ActiveApplyFormServiceResponse;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplyFormUserServiceTest {

    @Mock
    private ApplyFormRepository applyFormRepository;

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private ApplyFormUserService applyFormUserService;

    @Test
    @DisplayName("getActiveApplyForm(): 활성화된 지원폼을 정상적으로 조회한다")
    void getActiveApplyForm_Success() {
        // given
        String clubId = "club-1";
        List<Question> questions = List.of(
                new Question("q1", "이름", null, QuestionType.SHORT_ANSWER, true, null)
        );

        ApplyForm applyForm = mock(ApplyForm.class);
        given(applyForm.getId()).willReturn("form-1");
        given(applyForm.getTitle()).willReturn("동아리 지원폼");
        given(applyForm.getSubTitle()).willReturn("2026년 상반기 모집");
        given(applyForm.getFormJson()).willReturn(questions);

        given(clubRepository.existsById(clubId)).willReturn(true);
        given(applyFormRepository.findByClubIdAndStatus(clubId, ApplyFormStatus.ACTIVE))
                .willReturn(Optional.of(applyForm));

        // when
        ActiveApplyFormServiceResponse response = applyFormUserService.getActiveApplyForm(clubId);

        // then
        assertThat(response.formId()).isEqualTo("form-1");
        assertThat(response.title()).isEqualTo("동아리 지원폼");
        assertThat(response.subTitle()).isEqualTo("2026년 상반기 모집");
        assertThat(response.questions()).isEqualTo(questions);
    }

    @Test
    @DisplayName("getActiveApplyForm(): 동아리가 존재하지 않으면 ClubNotFoundException이 발생한다")
    void getActiveApplyForm_ClubNotFound() {
        // given
        String clubId = "non-existent-club";
        given(clubRepository.existsById(clubId)).willReturn(false);

        // when, then
        assertThatThrownBy(() -> applyFormUserService.getActiveApplyForm(clubId))
                .isInstanceOf(ClubNotFoundException.class);

        verify(applyFormRepository, never()).findByClubIdAndStatus(anyString(), any());
    }

    @Test
    @DisplayName("getActiveApplyForm(): 활성화된 지원폼이 없으면 ActiveApplyFormNotFoundException이 발생한다")
    void getActiveApplyForm_ActiveFormNotFound() {
        // given
        String clubId = "club-1";
        given(clubRepository.existsById(clubId)).willReturn(true);
        given(applyFormRepository.findByClubIdAndStatus(clubId, ApplyFormStatus.ACTIVE))
                .willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> applyFormUserService.getActiveApplyForm(clubId))
                .isInstanceOf(ActiveApplyFormNotFoundException.class);
    }
}
