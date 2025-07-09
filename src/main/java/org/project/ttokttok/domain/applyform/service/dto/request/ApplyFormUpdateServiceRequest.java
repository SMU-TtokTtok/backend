package org.project.ttokttok.domain.applyform.service.dto.request;

import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.applyform.domain.json.Question;

import java.util.List;

@Builder
public record ApplyFormUpdateServiceRequest(
    String username,
    String clubId,
    String applyFormId,
    JsonNullable<String> title,
    JsonNullable<String> subtitle,
    JsonNullable<List<Question>> questions
) {
}
