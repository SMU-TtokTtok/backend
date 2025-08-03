package org.project.ttokttok.domain.applyform.service.dto.request;

import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.applyform.domain.json.Question;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Builder
public record ApplyFormCreateServiceRequest(
        String username,
        String clubId,
        boolean hasInterview,
        LocalDate recruitStartDate,
        LocalDate recruitEndDate,
        Set<Integer> applicableGrades,
        int maxApplyCount,
        LocalDate interviewStartDate,
        LocalDate interviewEndDate,
        String title,
        String subTitle,
        List<Question> questions
) {
}
