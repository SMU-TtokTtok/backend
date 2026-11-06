package org.project.ttokttok.domain.applicant.domain.json;

import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;

import java.util.List;

public record Answer(
        String title,
        String subTitle,
        QuestionType questionType,
        boolean isEssential,
        List<String> content,
        Object value
) {
}
