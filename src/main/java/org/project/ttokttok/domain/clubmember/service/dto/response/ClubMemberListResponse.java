package org.project.ttokttok.domain.clubmember.service.dto.response;

import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubmember.domain.MemberRole;

public record ClubMemberListResponse(
        String memberId,
        String name,
        Grade grade,
        String major,
        MemberRole role
) {
}
