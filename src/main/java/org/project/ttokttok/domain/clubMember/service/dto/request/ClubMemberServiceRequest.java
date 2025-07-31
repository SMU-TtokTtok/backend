package org.project.ttokttok.domain.clubMember.service.dto.request;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;

@Builder
public record ClubMemberServiceRequest(
        Long studentNum,
        String name,
        String major,
        Grade grade,
        String email,
        String phoneNumber,
        Gender gender
) {
}
