package org.project.ttokttok.domain.applyform.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ApplyFormTest {

    private final Club club = mock(Club.class);

    private ApplyForm createDefaultApplyForm() {
        return ApplyForm.createApplyForm(
                club,
                true,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 5),
                1,
                new HashSet<>(Set.of(ApplicableGrade.FIRST_GRADE, ApplicableGrade.SECOND_GRADE)),
                "동아리 지원폼",
                "부제목",
                List.of(new Question("q1", "이름", null, QuestionType.SHORT_ANSWER, true, null))
        );
    }

    @Nested
    @DisplayName("createApplyForm(): 지원폼 생성 테스트")
    class CreateApplyFormTest {

        @Test
        @DisplayName("모든 값을 정상적으로 전달하면 지원폼이 생성된다")
        void createApplyForm_Success() {
            // when
            ApplyForm applyForm = createDefaultApplyForm();

            // then
            assertThat(applyForm.getClub()).isEqualTo(club);
            assertThat(applyForm.isHasInterview()).isTrue();
            assertThat(applyForm.getApplyStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(applyForm.getApplyEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
            assertThat(applyForm.getMaxApplyCount()).isEqualTo(1);
            assertThat(applyForm.getGrades()).containsExactlyInAnyOrder(
                    ApplicableGrade.FIRST_GRADE, ApplicableGrade.SECOND_GRADE
            );
            assertThat(applyForm.getTitle()).isEqualTo("동아리 지원폼");
            assertThat(applyForm.getSubTitle()).isEqualTo("부제목");
            assertThat(applyForm.getFormJson()).hasSize(1);
        }

        @Test
        @DisplayName("생성 직후 상태는 ACTIVE이고 모집중 상태이다")
        void createApplyForm_DefaultStatus() {
            // when
            ApplyForm applyForm = createDefaultApplyForm();

            // then
            assertThat(applyForm.getStatus()).isEqualTo(ApplyFormStatus.ACTIVE);
            assertThat(applyForm.isRecruiting()).isTrue();
        }

        @Test
        @DisplayName("학년 정보가 null이면 빈 학년 목록으로 생성된다")
        void createApplyForm_NullGrades() {
            // when
            ApplyForm applyForm = ApplyForm.createApplyForm(
                    club, false,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    null, null,
                    1, null,
                    "제목", "부제목", List.of()
            );

            // then
            assertThat(applyForm.getGrades()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateApplyInfo(): 지원 정보 수정 테스트")
    class UpdateApplyInfoTest {

        @Test
        @DisplayName("전달된 값으로 지원 정보를 수정한다")
        void updateApplyInfo_Success() {
            // given
            ApplyForm applyForm = createDefaultApplyForm();

            // when
            applyForm.updateApplyInfo(
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    5,
                    Set.of(ApplicableGrade.THIRD_GRADE)
            );

            // then
            assertThat(applyForm.getApplyStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(applyForm.getApplyEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
            assertThat(applyForm.getMaxApplyCount()).isEqualTo(5);
            assertThat(applyForm.getGrades()).containsExactly(ApplicableGrade.THIRD_GRADE);
        }

        @Test
        @DisplayName("null이 전달된 필드는 기존 값을 유지한다")
        void updateApplyInfo_NullValues_KeepsOriginal() {
            // given
            ApplyForm applyForm = createDefaultApplyForm();
            LocalDate originalStart = applyForm.getApplyStartDate();
            LocalDate originalEnd = applyForm.getApplyEndDate();
            Integer originalMaxApplyCount = applyForm.getMaxApplyCount();

            // when
            applyForm.updateApplyInfo(null, null, null, null);

            // then
            assertThat(applyForm.getApplyStartDate()).isEqualTo(originalStart);
            assertThat(applyForm.getApplyEndDate()).isEqualTo(originalEnd);
            assertThat(applyForm.getMaxApplyCount()).isEqualTo(originalMaxApplyCount);
            assertThat(applyForm.getGrades()).containsExactlyInAnyOrder(
                    ApplicableGrade.FIRST_GRADE, ApplicableGrade.SECOND_GRADE
            );
        }
    }

    @Nested
    @DisplayName("updateFormContent(): 지원폼 내용 수정 테스트")
    class UpdateFormContentTest {

        @Test
        @DisplayName("전달된 값으로 제목, 부제목, 질문을 수정한다")
        void updateFormContent_Success() {
            // given
            ApplyForm applyForm = createDefaultApplyForm();
            List<Question> newQuestions = List.of(
                    new Question("q2", "전공", null, QuestionType.SHORT_ANSWER, false, null)
            );

            // when
            applyForm.updateFormContent("새로운 제목", "새로운 부제목", newQuestions);

            // then
            assertThat(applyForm.getTitle()).isEqualTo("새로운 제목");
            assertThat(applyForm.getSubTitle()).isEqualTo("새로운 부제목");
            assertThat(applyForm.getFormJson()).isEqualTo(newQuestions);
        }

        @Test
        @DisplayName("null이 전달된 필드는 기존 값을 유지한다")
        void updateFormContent_NullValues_KeepsOriginal() {
            // given
            ApplyForm applyForm = createDefaultApplyForm();
            String originalTitle = applyForm.getTitle();
            String originalSubTitle = applyForm.getSubTitle();
            List<Question> originalFormJson = applyForm.getFormJson();

            // when
            applyForm.updateFormContent(null, null, null);

            // then
            assertThat(applyForm.getTitle()).isEqualTo(originalTitle);
            assertThat(applyForm.getSubTitle()).isEqualTo(originalSubTitle);
            assertThat(applyForm.getFormJson()).isEqualTo(originalFormJson);
        }
    }

    @Nested
    @DisplayName("상태/모집 여부 변경 테스트")
    class StatusToggleTest {

        @Test
        @DisplayName("updateFormStatus(): ACTIVE 상태를 호출하면 INACTIVE로 바뀐다")
        void updateFormStatus_ActiveToInactive() {
            // given
            ApplyForm applyForm = createDefaultApplyForm();
            assertThat(applyForm.getStatus()).isEqualTo(ApplyFormStatus.ACTIVE);

            // when
            applyForm.updateFormStatus();

            // then
            assertThat(applyForm.getStatus()).isEqualTo(ApplyFormStatus.INACTIVE);
        }

        @Test
        @DisplayName("updateFormStatus(): 다시 호출하면 ACTIVE로 되돌아간다")
        void updateFormStatus_TogglesBack() {
            // given
            ApplyForm applyForm = createDefaultApplyForm();

            // when
            applyForm.updateFormStatus();
            applyForm.updateFormStatus();

            // then
            assertThat(applyForm.getStatus()).isEqualTo(ApplyFormStatus.ACTIVE);
        }

        @Test
        @DisplayName("toggleRecruiting(): 모집 상태를 반전시킨다")
        void toggleRecruiting_Success() {
            // given
            ApplyForm applyForm = createDefaultApplyForm();
            assertThat(applyForm.isRecruiting()).isTrue();

            // when
            applyForm.toggleRecruiting();

            // then
            assertThat(applyForm.isRecruiting()).isFalse();

            // when
            applyForm.toggleRecruiting();

            // then
            assertThat(applyForm.isRecruiting()).isTrue();
        }

        @Test
        @DisplayName("endRecruiting(): 모집 상태를 false로 고정한다")
        void endRecruiting_Success() {
            // given
            ApplyForm applyForm = createDefaultApplyForm();

            // when
            applyForm.endRecruiting();

            // then
            assertThat(applyForm.isRecruiting()).isFalse();

            // when - 이미 false인 상태에서 다시 호출해도 false를 유지한다
            applyForm.endRecruiting();

            // then
            assertThat(applyForm.isRecruiting()).isFalse();
        }
    }
}
