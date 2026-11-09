package org.project.ttokttok.domain.applyform.domain;

import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.exception.ApplyFormRuleException;
import org.project.ttokttok.global.exception.ErrorMessage;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 지원서가 한 건이라도 제출된 폼의 질문 수정 규칙.
 *
 * <p>제출된 답변에는 질문 스냅샷이 저장되어 제출 완료 건은 깨지지 않는다. 이 규칙은 아직 작성 중인
 * 지원자가 바뀐 폼에 기존 답을 그대로 제출했을 때 실패하게 만드는 변경만 막는다.
 * 질문 제목, 부제목, 순서, 선택지 추가, 필수에서 선택으로의 변경은 허용한다.
 */
public final class ApplyFormEditLockPolicy {

    private ApplyFormEditLockPolicy() {
    }

    public static void validate(List<Question> current, List<Question> updated) {
        Map<String, Question> updatedById = indexById(updated);
        current.forEach(old -> validateKept(old, updatedById.get(old.questionId())));

        Map<String, Question> currentById = indexById(current);
        updated.stream()
                .filter(question -> !currentById.containsKey(question.questionId()))
                .forEach(ApplyFormEditLockPolicy::validateAddedIsOptional);
    }

    private static Map<String, Question> indexById(List<Question> questions) {
        return questions.stream()
                .collect(Collectors.toMap(Question::questionId, Function.identity(), (first, second) -> second));
    }

    private static void validateKept(Question old, Question kept) {
        if (kept == null) {
            throw locked(old, "을 삭제할 수 없습니다.");
        }
        if (old.questionType() != kept.questionType()) {
            throw locked(old, "의 유형을 바꿀 수 없습니다.");
        }
        if (!old.isEssential() && kept.isEssential()) {
            throw locked(old, "을 필수 질문으로 바꿀 수 없습니다.");
        }
        if (isChoice(old.questionType()) && !hasAllChoices(old, kept)) {
            throw locked(old, "의 기존 선택지를 삭제하거나 이름을 바꿀 수 없습니다.");
        }
    }

    private static void validateAddedIsOptional(Question added) {
        if (added.isEssential()) {
            throw locked(added, "을 필수 질문으로 추가할 수 없습니다.");
        }
    }

    private static boolean isChoice(QuestionType type) {
        return type == QuestionType.CHECKBOX || type == QuestionType.RADIO;
    }

    private static boolean hasAllChoices(Question old, Question kept) {
        if (old.content() == null) {
            return true;
        }
        return kept.content() != null && kept.content().containsAll(old.content());
    }

    private static ApplyFormRuleException locked(Question question, String reasonSuffix) {
        return new ApplyFormRuleException(ErrorMessage.APPLY_FORM_EDIT_LOCKED,
                "지원서가 제출된 폼이라 '" + question.title() + "' 질문" + reasonSuffix);
    }
}
