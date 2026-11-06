package org.project.ttokttok.domain.clubmember.domain.dto;

import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubmember.domain.MemberRole;

public record ClubMemberInExcelResponse(
        Grade grade,
        String name,
        String major,
        MemberRole role
) {
}
