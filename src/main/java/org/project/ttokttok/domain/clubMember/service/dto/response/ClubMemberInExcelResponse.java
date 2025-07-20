package org.project.ttokttok.domain.clubMember.service.dto.response;

import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;

public record ClubMemberInExcelResponse(
        Grade grade,
        String name,
        String major,
        MemberRole role
) {
}
