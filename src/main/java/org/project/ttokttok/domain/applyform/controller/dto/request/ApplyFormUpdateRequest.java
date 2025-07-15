package org.project.ttokttok.domain.applyform.controller.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormUpdateServiceRequest;

import java.util.List;

public record ApplyFormUpdateRequest(
    JsonNullable<String> title,
    JsonNullable<String> subtitle,
    JsonNullable<List<Question>> questions
) {
    public ApplyFormUpdateServiceRequest toServiceRequest(String username, String formId) {
        return ApplyFormUpdateServiceRequest.builder()
            .username(username)
            .applyFormId(formId)
            .title(title)
            .subtitle(subtitle)
            .questions(questions)
            .build();
    }
}
