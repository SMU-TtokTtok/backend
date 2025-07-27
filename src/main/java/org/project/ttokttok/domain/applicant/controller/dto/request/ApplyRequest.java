package org.project.ttokttok.domain.applicant.controller.dto.request;

import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;

public record ApplyRequest(
        String name,
        Integer age,
        String major,
        String email,
        String phone,
        StudentStatus studentStatus,
        Gender gender

) {
}