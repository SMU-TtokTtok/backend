package org.project.ttokttok.domain.applicant.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.exception.InvalidPhaseTransitionException;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ApplicantTest {

    private Applicant createApplicant(ApplyForm applyForm) {
        return Applicant.createApplicant(
                "test@sangmyung.kr",
                "홍길동",
                21,
                "소프트웨어학과",
                "hong@gmail.com",
                "010-1234-5678",
                StudentStatus.ENROLLED,
                Grade.SECOND_GRADE,
                Gender.MALE,
                applyForm
        );
    }

    @Nested
    @DisplayName("createApplicant() 테스트")
    class CreateApplicantTest {

        @Test
        @DisplayName("지원자 정보로 Applicant를 생성하면 초기 단계는 DOCUMENT이다")
        void createApplicant_Success() {
            // given
            ApplyForm applyForm = mock(ApplyForm.class);

            // when
            Applicant applicant = createApplicant(applyForm);

            // then
            assertThat(applicant.getName()).isEqualTo("홍길동");
            assertThat(applicant.getCurrentPhase()).isEqualTo(ApplicantPhase.DOCUMENT);
            assertThat(applicant.getDocumentPhase()).isNull();
            assertThat(applicant.getInterviewPhase()).isNull();
        }
    }

    @Nested
    @DisplayName("submitDocument() 테스트")
    class SubmitDocumentTest {

        @Test
        @DisplayName("서류를 제출하면 DocumentPhase가 생성되고 DOCUMENT 단계가 된다")
        void submitDocument_Success() {
            // given
            Applicant applicant = createApplicant(mock(ApplyForm.class));
            List<Answer> answers = List.of();

            // when
            applicant.submitDocument(answers);

            // then
            assertThat(applicant.getDocumentPhase()).isNotNull();
            assertThat(applicant.isInDocumentPhase()).isTrue();
        }

        @Test
        @DisplayName("이미 면접 단계인 지원자가 서류를 다시 제출하면 InvalidPhaseTransitionException이 발생한다")
        void submitDocument_Fail_WhenAlreadyInInterviewPhase() {
            // given
            ApplyForm applyForm = mock(ApplyForm.class);
            Applicant applicant = createApplicant(applyForm);
            applicant.submitDocument(List.of());
            applicant.updateToInterviewPhase(LocalDate.of(2026, 8, 1));

            // when & then
            assertThatThrownBy(() -> applicant.submitDocument(List.of()))
                    .isInstanceOf(InvalidPhaseTransitionException.class);
        }
    }

    @Nested
    @DisplayName("서류 평가 결과 테스트")
    class DocumentEvaluationTest {

        @Test
        @DisplayName("passDocumentEvaluation() 호출 시 서류 상태가 PASS가 된다")
        void passDocumentEvaluation_Success() {
            // given
            Applicant applicant = createApplicant(mock(ApplyForm.class));
            applicant.submitDocument(List.of());

            // when
            applicant.passDocumentEvaluation();

            // then
            assertThat(applicant.getDocumentPhase().getStatus()).isEqualTo(PhaseStatus.PASS);
        }

        @Test
        @DisplayName("면접 단계가 없는 지원자는 failDocumentEvaluation() 호출 시 서류 상태가 FAIL이 된다")
        void failDocumentEvaluation_Success() {
            // given
            ApplyForm applyForm = mock(ApplyForm.class);
            given(applyForm.isHasInterview()).willReturn(false);
            Applicant applicant = createApplicant(applyForm);
            applicant.submitDocument(List.of());

            // when
            applicant.failDocumentEvaluation();

            // then
            assertThat(applicant.getDocumentPhase().getStatus()).isEqualTo(PhaseStatus.FAIL);
        }

        @Test
        @DisplayName("이미 면접 단계에 존재하는 지원자는 failDocumentEvaluation() 호출 시 IllegalArgumentException이 발생한다")
        void failDocumentEvaluation_Fail_WhenAlreadyInInterviewPhase() {
            // given
            ApplyForm applyForm = mock(ApplyForm.class);
            given(applyForm.isHasInterview()).willReturn(true);
            Applicant applicant = createApplicant(applyForm);
            applicant.submitDocument(List.of());
            applicant.updateToInterviewPhase(LocalDate.of(2026, 8, 1));

            // when & then
            assertThatThrownBy(applicant::failDocumentEvaluation)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 면접 단계에 존재하는 지원자입니다.");
        }

        @Test
        @DisplayName("setDocumentEvaluating() 호출 시 서류 상태가 EVALUATING이 된다")
        void setDocumentEvaluating_Success() {
            // given
            Applicant applicant = createApplicant(mock(ApplyForm.class));
            applicant.submitDocument(List.of());
            applicant.passDocumentEvaluation();

            // when
            applicant.setDocumentEvaluating();

            // then
            assertThat(applicant.getDocumentPhase().getStatus()).isEqualTo(PhaseStatus.EVALUATING);
        }

        @Test
        @DisplayName("이미 면접 단계에 존재하는 지원자는 setDocumentEvaluating() 호출 시 IllegalArgumentException이 발생한다")
        void setDocumentEvaluating_Fail_WhenAlreadyInInterviewPhase() {
            // given
            ApplyForm applyForm = mock(ApplyForm.class);
            given(applyForm.isHasInterview()).willReturn(true);
            Applicant applicant = createApplicant(applyForm);
            applicant.submitDocument(List.of());
            applicant.updateToInterviewPhase(LocalDate.of(2026, 8, 1));

            // when & then
            assertThatThrownBy(applicant::setDocumentEvaluating)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 면접 단계에 존재하는 지원자입니다.");
        }
    }

    @Nested
    @DisplayName("면접 단계 테스트")
    class InterviewPhaseTransitionTest {

        @Test
        @DisplayName("updateToInterviewPhase() 호출 시 InterviewPhase가 생성되고 INTERVIEW 단계가 된다")
        void updateToInterviewPhase_Success() {
            // given
            Applicant applicant = createApplicant(mock(ApplyForm.class));
            LocalDate interviewDate = LocalDate.of(2026, 8, 1);

            // when
            applicant.updateToInterviewPhase(interviewDate);

            // then
            assertThat(applicant.getInterviewPhase()).isNotNull();
            assertThat(applicant.getInterviewPhase().getInterviewDate()).isEqualTo(interviewDate);
            assertThat(applicant.isInInterviewPhase()).isTrue();
        }

        @Test
        @DisplayName("passInterview() 호출 시 면접 상태가 PASS가 된다")
        void passInterview_Success() {
            // given
            Applicant applicant = createApplicant(mock(ApplyForm.class));
            applicant.updateToInterviewPhase(LocalDate.of(2026, 8, 1));

            // when
            applicant.passInterview();

            // then
            assertThat(applicant.getInterviewPhase().getStatus()).isEqualTo(PhaseStatus.PASS);
        }

        @Test
        @DisplayName("failInterview() 호출 시 면접 상태가 FAIL이 된다")
        void failInterview_Success() {
            // given
            Applicant applicant = createApplicant(mock(ApplyForm.class));
            applicant.updateToInterviewPhase(LocalDate.of(2026, 8, 1));

            // when
            applicant.failInterview();

            // then
            assertThat(applicant.getInterviewPhase().getStatus()).isEqualTo(PhaseStatus.FAIL);
        }

        @Test
        @DisplayName("setInterviewEvaluating() 호출 시 면접 상태가 EVALUATING이 된다")
        void setInterviewEvaluating_Success() {
            // given
            Applicant applicant = createApplicant(mock(ApplyForm.class));
            applicant.updateToInterviewPhase(LocalDate.of(2026, 8, 1));
            applicant.passInterview();

            // when
            applicant.setInterviewEvaluating();

            // then
            assertThat(applicant.getInterviewPhase().getStatus()).isEqualTo(PhaseStatus.EVALUATING);
        }
    }

    @Nested
    @DisplayName("편의 메서드 테스트")
    class ConvenienceMethodTest {

        @Test
        @DisplayName("hasInterviewPhase()는 ApplyForm의 면접 전형 존재 여부를 그대로 반환한다")
        void hasInterviewPhase_DelegatesToApplyForm() {
            // given
            ApplyForm applyFormWithInterview = mock(ApplyForm.class);
            given(applyFormWithInterview.isHasInterview()).willReturn(true);
            Applicant applicantWithInterview = createApplicant(applyFormWithInterview);

            ApplyForm applyFormWithoutInterview = mock(ApplyForm.class);
            given(applyFormWithoutInterview.isHasInterview()).willReturn(false);
            Applicant applicantWithoutInterview = createApplicant(applyFormWithoutInterview);

            // when & then
            assertThat(applicantWithInterview.hasInterviewPhase()).isTrue();
            assertThat(applicantWithoutInterview.hasInterviewPhase()).isFalse();
        }

        @Test
        @DisplayName("isInDocumentPhase()와 isInInterviewPhase()는 currentPhase에 따라 결정된다")
        void isInDocumentPhase_And_isInInterviewPhase() {
            // given
            Applicant applicant = createApplicant(mock(ApplyForm.class));

            // then: 생성 직후는 DOCUMENT 단계
            assertThat(applicant.isInDocumentPhase()).isTrue();
            assertThat(applicant.isInInterviewPhase()).isFalse();

            // when: 면접 단계로 전환
            applicant.updateToInterviewPhase(LocalDate.of(2026, 8, 1));

            // then
            assertThat(applicant.isInDocumentPhase()).isFalse();
            assertThat(applicant.isInInterviewPhase()).isTrue();
        }
    }
}
