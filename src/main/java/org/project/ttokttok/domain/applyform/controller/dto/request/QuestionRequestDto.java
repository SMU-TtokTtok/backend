package org.project.ttokttok.domain.applyform.controller.dto.request;

import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;

import java.util.List;
import java.util.UUID;

public record QuestionRequestDto(
        String title,
        String subTitle,
        QuestionType questionType,
        boolean isEssential,
        List<String> content
) {
    public Question toQuestion() {
        return new Question(
                UUID.randomUUID().toString(),
                title,
                subTitle,
                questionType,
                isEssential,
                content
        );
    }
}
