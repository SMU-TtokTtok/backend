package org.project.ttokttok.domain.temp.applyform.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.temp.applyform.controller.dto.request.TempApplyFormSaveRequest;
import org.project.ttokttok.domain.temp.applyform.domain.TempApplyForm;
import org.project.ttokttok.domain.temp.applyform.repository.TempApplyFormRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TempApplyFormServiceTest {

    @Mock
    private TempApplyFormRepository tempApplyFormRepository;

    @InjectMocks
    private TempApplyFormService tempApplyFormService;

    private TempApplyFormSaveRequest buildRequest(String clubId) {
        return new TempApplyFormSaveRequest(
                clubId,
                "임시 지원폼 제목",
                "부제목",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                true,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 5),
                3,
                Set.of(ApplicableGrade.FIRST_GRADE),
                List.of(new Question("q1", "이름", null, QuestionType.SHORT_ANSWER, true, null))
        );
    }

    @Test
    @DisplayName("saveTempApplyForm(): 기존 임시 지원폼이 없으면 새로 생성한다")
    void saveTempApplyForm_CreatesNew_WhenNoExisting() {
        // given
        String clubId = "club-1";
        TempApplyFormSaveRequest request = buildRequest(clubId);

        given(tempApplyFormRepository.findByClubId(clubId)).willReturn(Optional.empty());

        TempApplyForm saved = mock(TempApplyForm.class);
        given(saved.getId()).willReturn("temp-form-1");
        given(tempApplyFormRepository.save(any(TempApplyForm.class))).willReturn(saved);

        // when
        String result = tempApplyFormService.saveTempApplyForm(request);

        // then
        assertThat(result).isEqualTo("temp-form-1");

        ArgumentCaptor<TempApplyForm> captor = ArgumentCaptor.forClass(TempApplyForm.class);
        verify(tempApplyFormRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getClubId()).isEqualTo(clubId);
        assertThat(captor.getValue().getTitle()).isEqualTo("임시 지원폼 제목");
    }

    @Test
    @DisplayName("saveTempApplyForm(): 기존 임시 지원폼이 있으면 업데이트한다")
    void saveTempApplyForm_UpdatesExisting_WhenPresent() {
        // given
        String clubId = "club-1";
        TempApplyFormSaveRequest request = buildRequest(clubId);

        TempApplyForm existing = mock(TempApplyForm.class);
        given(existing.getId()).willReturn("temp-form-existing");
        given(tempApplyFormRepository.findByClubId(clubId)).willReturn(Optional.of(existing));

        // when
        String result = tempApplyFormService.saveTempApplyForm(request);

        // then
        assertThat(result).isEqualTo("temp-form-existing");
        verify(existing, times(1)).update(
                request.title(), request.subTitle(), request.applyStartDate(), request.applyEndDate(),
                true, request.interviewStartDate(), request.interviewEndDate(),
                request.maxApplyCount(), request.grades(), request.formJson()
        );
        verify(tempApplyFormRepository, never()).save(any(TempApplyForm.class));
    }

    @Test
    @DisplayName("saveTempApplyForm(): hasInterview이 null이면 false로 저장된다")
    void saveTempApplyForm_NullHasInterview_DefaultsToFalse() {
        // given
        String clubId = "club-1";
        TempApplyFormSaveRequest request = new TempApplyFormSaveRequest(
                clubId, null, null, null, null,
                null, null, null, null, null, null
        );

        given(tempApplyFormRepository.findByClubId(clubId)).willReturn(Optional.empty());
        TempApplyForm saved = mock(TempApplyForm.class);
        given(saved.getId()).willReturn("temp-form-2");
        given(tempApplyFormRepository.save(any(TempApplyForm.class))).willReturn(saved);

        // when
        tempApplyFormService.saveTempApplyForm(request);

        // then
        ArgumentCaptor<TempApplyForm> captor = ArgumentCaptor.forClass(TempApplyForm.class);
        verify(tempApplyFormRepository).save(captor.capture());
        assertThat(captor.getValue().isHasInterview()).isFalse();
    }
}
