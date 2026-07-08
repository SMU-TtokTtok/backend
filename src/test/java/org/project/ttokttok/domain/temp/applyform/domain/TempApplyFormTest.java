package org.project.ttokttok.domain.temp.applyform.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TempApplyFormTest {

    @Nested
    @DisplayName("create(): 임시 지원폼 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("전달된 값으로 임시 지원폼을 생성한다")
        void create_Success() {
            // given
            List<Question> questions = List.of(
                    new Question("q1", "이름", null, QuestionType.SHORT_ANSWER, true, null)
            );

            // when
            TempApplyForm tempApplyForm = TempApplyForm.create(
                    "club-1", "임시 지원폼", "부제목",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    true, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
                    3, Set.of(ApplicableGrade.FIRST_GRADE), questions
            );

            // then
            assertThat(tempApplyForm.getClubId()).isEqualTo("club-1");
            assertThat(tempApplyForm.getTitle()).isEqualTo("임시 지원폼");
            assertThat(tempApplyForm.getSubTitle()).isEqualTo("부제목");
            assertThat(tempApplyForm.getApplyStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(tempApplyForm.getApplyEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
            assertThat(tempApplyForm.isHasInterview()).isTrue();
            assertThat(tempApplyForm.getMaxApplyCount()).isEqualTo(3);
            assertThat(tempApplyForm.getGrades()).containsExactly(ApplicableGrade.FIRST_GRADE);
            assertThat(tempApplyForm.getFormJson()).isEqualTo(questions);
        }

        @Test
        @DisplayName("학년과 질문 목록이 null이면 빈 컬렉션으로 생성된다")
        void create_NullCollections_DefaultsToEmpty() {
            // when
            TempApplyForm tempApplyForm = TempApplyForm.create(
                    "club-1", null, null,
                    null, null,
                    false, null, null,
                    null, null, null
            );

            // then
            assertThat(tempApplyForm.getGrades()).isEmpty();
            assertThat(tempApplyForm.getFormJson()).isEmpty();
        }
    }

    @Nested
    @DisplayName("update(): 임시 지원폼 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("전달된 값으로 임시 지원폼 내용을 모두 교체한다")
        void update_Success() {
            // given
            TempApplyForm tempApplyForm = TempApplyForm.create(
                    "club-1", "기존 제목", "기존 부제목",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    false, null, null,
                    1, Set.of(ApplicableGrade.FIRST_GRADE), List.of()
            );

            List<Question> newQuestions = List.of(
                    new Question("q2", "전공", null, QuestionType.SHORT_ANSWER, false, null)
            );

            // when
            tempApplyForm.update(
                    "새로운 제목", "새로운 부제목",
                    LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                    true, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5),
                    5, Set.of(ApplicableGrade.THIRD_GRADE), newQuestions
            );

            // then
            assertThat(tempApplyForm.getTitle()).isEqualTo("새로운 제목");
            assertThat(tempApplyForm.getSubTitle()).isEqualTo("새로운 부제목");
            assertThat(tempApplyForm.getApplyStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(tempApplyForm.getApplyEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
            assertThat(tempApplyForm.isHasInterview()).isTrue();
            assertThat(tempApplyForm.getInterviewStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
            assertThat(tempApplyForm.getMaxApplyCount()).isEqualTo(5);
            assertThat(tempApplyForm.getGrades()).containsExactly(ApplicableGrade.THIRD_GRADE);
            assertThat(tempApplyForm.getFormJson()).isEqualTo(newQuestions);
        }

        @Test
        @DisplayName("학년과 질문 목록에 null을 전달하면 빈 컬렉션으로 초기화된다")
        void update_NullCollections_ResetsToEmpty() {
            // given
            TempApplyForm tempApplyForm = TempApplyForm.create(
                    "club-1", "기존 제목", "기존 부제목",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    false, null, null,
                    1, Set.of(ApplicableGrade.FIRST_GRADE), List.of(
                            new Question("q1", "이름", null, QuestionType.SHORT_ANSWER, true, null)
                    )
            );

            // when
            tempApplyForm.update(
                    "새로운 제목", null,
                    null, null,
                    false, null, null,
                    null, null, null
            );

            // then
            assertThat(tempApplyForm.getGrades()).isEmpty();
            assertThat(tempApplyForm.getFormJson()).isEmpty();
        }
    }
}
