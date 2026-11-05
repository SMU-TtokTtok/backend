package org.project.ttokttok.domain.temp.applyform.service.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.json.Question;

public record TempApplyFormSaveServiceRequest(
        String clubId,
        String title,
        String subTitle,
        LocalDate applyStartDate,
        LocalDate applyEndDate,
        Boolean hasInterview,
        LocalDate interviewStartDate,
        LocalDate interviewEndDate,
        Integer maxApplyCount,
        Set<ApplicableGrade> grades,
        List<Question> formJson
) {
}
