package org.project.ttokttok.domain.applyform.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplyFormSchedulerTest {

    @Mock
    private ApplyFormRepository applyFormRepository;

    @InjectMocks
    private ApplyFormScheduler applyFormScheduler;

    @Test
    @DisplayName("updateExpiredApplyFormsStatus(): 마감된 지원폼들의 모집 상태를 종료시킨다")
    void updateExpiredApplyFormsStatus_EndsRecruitingForExpiredForms() {
        // given
        ApplyForm expiredForm1 = mock(ApplyForm.class);
        ApplyForm expiredForm2 = mock(ApplyForm.class);
        given(applyFormRepository.findExpiredApplyForms(any(LocalDate.class)))
                .willReturn(List.of(expiredForm1, expiredForm2));

        // when
        applyFormScheduler.updateExpiredApplyFormsStatus();

        // then
        verify(expiredForm1, times(1)).endRecruiting();
        verify(expiredForm2, times(1)).endRecruiting();
    }

    @Test
    @DisplayName("updateExpiredApplyFormsStatus(): 마감된 지원폼이 없으면 아무 것도 변경하지 않는다")
    void updateExpiredApplyFormsStatus_NoExpiredForms_DoesNothing() {
        // given
        given(applyFormRepository.findExpiredApplyForms(any(LocalDate.class)))
                .willReturn(List.of());

        // when
        applyFormScheduler.updateExpiredApplyFormsStatus();

        // then
        verify(applyFormRepository, times(1)).findExpiredApplyForms(any(LocalDate.class));
        verifyNoMoreInteractions(applyFormRepository);
    }

    @Test
    @DisplayName("updateExpiredApplyFormsStatus(): 조회 중 예외가 발생해도 스케줄러는 예외를 전파하지 않는다")
    void updateExpiredApplyFormsStatus_ExceptionIsSwallowed() {
        // given
        given(applyFormRepository.findExpiredApplyForms(any(LocalDate.class)))
                .willThrow(new RuntimeException("DB 오류"));

        // when, then
        assertThatCode(() -> applyFormScheduler.updateExpiredApplyFormsStatus())
                .doesNotThrowAnyException();
    }
}
