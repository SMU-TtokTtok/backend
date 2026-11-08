package org.project.ttokttok.domain.applyform.controller.dto.request;

import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;

import java.util.List;
import java.util.UUID;

/**
 * @param questionId 수정 요청에서 기존 질문을 식별하는 값. 새 질문이면 비워 둔다.
 */
public record QuestionRequestDto(
        String questionId,
        String title,
        String subTitle,
        QuestionType questionType,
        boolean isEssential,
        List<String> content
) {
    public Question toQuestion() {
        return toQuestion(UUID.randomUUID().toString());
    }

    /** 수정 요청용. 기존 질문은 받은 questionId 를 유지하고, 없으면 새 id 를 발급한다. */
    public Question toQuestionKeepingId() {
        return toQuestion(questionId == null || questionId.isBlank() ? UUID.randomUUID().toString() : questionId);
    }

    private Question toQuestion(String id) {
        return new Question(id, title, subTitle, questionType, isEssential, content);
    }
}
