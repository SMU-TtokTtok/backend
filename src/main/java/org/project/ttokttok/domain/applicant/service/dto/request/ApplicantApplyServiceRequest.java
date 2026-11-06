package org.project.ttokttok.domain.applicant.service.dto.request;

import java.util.List;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.service.answer.AnswerInput;

public record ApplicantApplyServiceRequest(
        String name,
        Integer age,
        String major,
        String email,
        String phone,
        StudentStatus studentStatus,
        Grade grade,
        Gender gender,
        List<AnswerInput> answers
) {
}
