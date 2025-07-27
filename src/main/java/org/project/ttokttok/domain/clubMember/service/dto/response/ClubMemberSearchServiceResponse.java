package org.project.ttokttok.domain.clubMember.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;

@Builder
public record ClubMemberSearchServiceResponse(
        String id,
        Grade grade,
        String name,
        String major,
        MemberRole role
) {
    public static ClubMemberSearchServiceResponse of(String id,
                                                     Grade grade,
                                                     String name,
                                                     String major,
                                                     MemberRole role) {
        return ClubMemberSearchServiceResponse.builder()
                .id(id)
                .grade(grade)
                .name(name)
                .major(major)
                .role(role)
                .build();
    }
}
