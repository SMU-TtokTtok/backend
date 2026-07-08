package org.project.ttokttok.domain.applicant.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InterviewPhaseTest {

    @Nested
    @DisplayName("create() 테스트")
    class CreateTest {

        @Test
        @DisplayName("지원자와 면접일로 면접 단계를 생성하면 상태는 EVALUATING이다")
        void create_Success() {
            // given
            Applicant applicant = mock(Applicant.class);
            LocalDate interviewDate = LocalDate.of(2026, 8, 1);

            // when
            InterviewPhase interviewPhase = InterviewPhase.create(applicant, interviewDate);

            // then
            assertThat(interviewPhase.getApplicant()).isEqualTo(applicant);
            assertThat(interviewPhase.getInterviewDate()).isEqualTo(interviewDate);
            assertThat(interviewPhase.getStatus()).isEqualTo(PhaseStatus.EVALUATING);
        }
    }

    @Nested
    @DisplayName("updateStatus() 테스트")
    class UpdateStatusTest {

        @Test
        @DisplayName("상태를 PASS로 변경할 수 있다")
        void updateStatus_Pass() {
            // given
            InterviewPhase interviewPhase = InterviewPhase.create(mock(Applicant.class), LocalDate.of(2026, 8, 1));

            // when
            interviewPhase.updateStatus(PhaseStatus.PASS);

            // then
            assertThat(interviewPhase.getStatus()).isEqualTo(PhaseStatus.PASS);
        }

        @Test
        @DisplayName("상태를 FAIL로 변경할 수 있다")
        void updateStatus_Fail() {
            // given
            InterviewPhase interviewPhase = InterviewPhase.create(mock(Applicant.class), LocalDate.of(2026, 8, 1));

            // when
            interviewPhase.updateStatus(PhaseStatus.FAIL);

            // then
            assertThat(interviewPhase.getStatus()).isEqualTo(PhaseStatus.FAIL);
        }
    }

    @Nested
    @DisplayName("updateInterviewDate() 테스트")
    class UpdateInterviewDateTest {

        @Test
        @DisplayName("면접일을 변경할 수 있다")
        void updateInterviewDate_Success() {
            // given
            InterviewPhase interviewPhase = InterviewPhase.create(mock(Applicant.class), LocalDate.of(2026, 8, 1));
            LocalDate newDate = LocalDate.of(2026, 9, 15);

            // when
            interviewPhase.updateInterviewDate(newDate);

            // then
            assertThat(interviewPhase.getInterviewDate()).isEqualTo(newDate);
        }
    }
}
