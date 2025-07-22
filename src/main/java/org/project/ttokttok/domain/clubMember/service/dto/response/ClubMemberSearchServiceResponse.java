package org.project.ttokttok.domain.clubMember.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;

@Builder
public record ClubMemberSearchServiceResponse(
        Grade grade,
        String name,
        String major,
        MemberRole role
) {
    public static ClubMemberSearchServiceResponse of(Grade grade,
                                                     String name,
                                                     String major,
                                                     MemberRole role) {
        return ClubMemberSearchServiceResponse.builder()
                .grade(grade)
                .name(name)
                .major(major)
                .role(role)
                .build();
    }
}
