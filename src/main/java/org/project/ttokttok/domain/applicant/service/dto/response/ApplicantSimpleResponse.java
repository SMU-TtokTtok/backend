package org.project.ttokttok.domain.applicant.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;

import java.time.LocalDate;

@Builder
public record ApplicantSimpleResponse(
        String id,
        Grade grade,
        String name,
        String major,
        PhaseStatus status,
        LocalDate interviewDate
) {
}