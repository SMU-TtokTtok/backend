package org.project.ttokttok.domain.applyform.controller.dto.response;

import org.project.ttokttok.domain.applyform.domain.json.Question;

import java.util.List;

public record BeforeQuestionsResponse(
        List<Question> beforeQuestions
) {
}
