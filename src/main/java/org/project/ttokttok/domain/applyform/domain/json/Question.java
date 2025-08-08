package org.project.ttokttok.domain.applyform.domain.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Question(
        String questionId,
        String title,
        String subTitle,
        QuestionType questionType,
        boolean isEssential,
        List<String> content
) {
}
