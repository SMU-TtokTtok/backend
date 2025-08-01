package org.project.ttokttok.domain.applicant.domain.dto;

import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;

import java.time.LocalDate;

// 지원자 페이지 내 들어갈 정보 DTO
public record ApplicantSimpleInfoDto(
        String id,
        Grade grade,
        String name,
        String major,
        PhaseStatus status,
        LocalDate interviewDate
) {
}

