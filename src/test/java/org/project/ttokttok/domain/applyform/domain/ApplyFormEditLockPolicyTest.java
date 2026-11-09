package org.project.ttokttok.domain.applyform.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.exception.ApplyFormRuleException;
import org.project.ttokttok.global.exception.ErrorMessage;

class ApplyFormEditLockPolicyTest {

    private static Question text(String id, String title, boolean essential) {
        return new Question(id, title, null, QuestionType.LONG_ANSWER, essential, List.of(""));
    }

    private static Question choice(String id, QuestionType type, boolean essential, String... choices) {
        return new Question(id, "선택 질문", null, type, essential, List.of(choices));
    }

    private void assertLocked(List<Question> current, List<Question> updated, String messagePart) {
        assertThatThrownBy(() -> ApplyFormEditLockPolicy.validate(current, updated))
                .isInstanceOf(ApplyFormRuleException.class)
                .hasMessageContaining(messagePart)
                .extracting(e -> ((ApplyFormRuleException) e).getStatus())
                .isEqualTo(ErrorMessage.APPLY_FORM_EDIT_LOCKED.getStatus());
    }

    @Test
    @DisplayName("질문을 삭제하면 질문 제목이 담긴 메시지로 거부한다")
    void 질문_삭제_거부() {
        List<Question> current = List.of(text("q1", "지원 동기", true), text("q2", "자기소개", false));

        assertLocked(current, List.of(text("q2", "자기소개", false)), "'지원 동기' 질문을 삭제할 수 없습니다.");
    }

    @Test
    @DisplayName("질문 유형을 바꾸면 거부한다")
    void 질문_유형_변경_거부() {
        List<Question> current = List.of(text("q1", "지원 동기", true));
        List<Question> updated = List.of(new Question("q1", "지원 동기", null, QuestionType.SHORT_ANSWER, true, List.of("")));

        assertLocked(current, updated, "유형을 바꿀 수 없습니다");
    }

    @Test
    @DisplayName("선택 질문을 필수로 바꾸면 거부한다")
    void 선택에서_필수_변경_거부() {
        assertLocked(List.of(text("q1", "지원 동기", false)), List.of(text("q1", "지원 동기", true)),
                "필수 질문으로 바꿀 수 없습니다");
    }

    @Test
    @DisplayName("필수 질문을 선택으로 바꾸는 것은 허용한다")
    void 필수에서_선택_변경_허용() {
        assertThatCode(() -> ApplyFormEditLockPolicy.validate(
                List.of(text("q1", "지원 동기", true)), List.of(text("q1", "지원 동기", false))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("기존 선택지를 지우면 거부한다")
    void 선택지_삭제_거부() {
        List<Question> current = List.of(choice("q1", QuestionType.RADIO, true, "월", "화"));

        assertLocked(current, List.of(choice("q1", QuestionType.RADIO, true, "월")), "기존 선택지");
    }

    @Test
    @DisplayName("기존 선택지 이름을 바꾸면 거부한다")
    void 선택지_이름변경_거부() {
        List<Question> current = List.of(choice("q1", QuestionType.CHECKBOX, true, "월", "화"));

        assertLocked(current, List.of(choice("q1", QuestionType.CHECKBOX, true, "월", "수")), "기존 선택지");
    }

    @Test
    @DisplayName("선택지를 추가하고 순서를 바꾸는 것은 허용한다")
    void 선택지_추가_순서변경_허용() {
        assertThatCode(() -> ApplyFormEditLockPolicy.validate(
                List.of(choice("q1", QuestionType.CHECKBOX, true, "월", "화")),
                List.of(choice("q1", QuestionType.CHECKBOX, true, "수", "화", "월"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("텍스트 질문의 content 가 [\"\"] 에서 비어도 선택지 검사를 하지 않는다")
    void 텍스트_질문_content_무시() {
        Question changed = new Question("q1", "지원 동기", null, QuestionType.LONG_ANSWER, true, null);

        assertThatCode(() -> ApplyFormEditLockPolicy.validate(List.of(text("q1", "지원 동기", true)), List.of(changed)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("새로 추가한 질문이 필수이면 거부한다")
    void 필수_질문_추가_거부() {
        List<Question> current = List.of(text("q1", "지원 동기", true));
        List<Question> updated = List.of(text("q1", "지원 동기", true), text("q2", "특기", true));

        assertLocked(current, updated, "'특기' 질문을 필수 질문으로 추가할 수 없습니다.");
    }

    @Test
    @DisplayName("새로 추가한 질문이 선택이면 허용한다")
    void 선택_질문_추가_허용() {
        assertThatCode(() -> ApplyFormEditLockPolicy.validate(
                List.of(text("q1", "지원 동기", true)),
                List.of(text("q1", "지원 동기", true), text("q2", "특기", false))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("질문 제목, 부제목, 순서 변경은 허용한다")
    void 제목_부제목_순서_변경_허용() {
        List<Question> current = List.of(text("q1", "지원 동기", true), text("q2", "자기소개", false));
        List<Question> updated = List.of(
                new Question("q2", "새 제목", "새 부제목", QuestionType.LONG_ANSWER, false, List.of("")),
                new Question("q1", "바뀐 제목", "설명", QuestionType.LONG_ANSWER, true, List.of("")));

        assertThatCode(() -> ApplyFormEditLockPolicy.validate(current, updated)).doesNotThrowAnyException();
    }
}
