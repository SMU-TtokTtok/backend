package org.project.ttokttok.domain.applyform.domain.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Question(
        String title,
        String subTitle,
        QuestionType questionType,
        boolean isEssential,
        List<String> content
) {
}
