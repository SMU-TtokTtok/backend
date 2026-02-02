package org.project.ttokttok.domain.applyform.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormUpdateServiceRequest;

import java.util.List;

public record ApplyFormUpdateRequest(
        @Schema(description = "선택적 필드 (null 가능)", nullable = true, example = "값 또는 null")
        JsonNullable<String> title,

        @Schema(description = "선택적 필드 (null 가능)", nullable = true, example = "값 또는 null")
        JsonNullable<String> subTitle,

        @Schema(description = "선택적 필드 (null 가능)", nullable = true, example = "값 또는 null")
        JsonNullable<List<QuestionRequestDto>> questions
) {
    public ApplyFormUpdateServiceRequest toServiceRequest(String username, String formId) {
        return ApplyFormUpdateServiceRequest.builder()
                .username(username)
                .applyFormId(formId)
                .title(title)
                .subTitle(subTitle)
                .questions(questionRequestPresent(questions))
                .build();
    }

    private JsonNullable<List<Question>> questionRequestPresent(JsonNullable<List<QuestionRequestDto>> questions) {
        if (questions.isPresent()) {
            return JsonNullable.of(questions.get().stream()
                    .map(QuestionRequestDto::toQuestion)
                    .toList());
        }

        return JsonNullable.undefined();
    }
}
