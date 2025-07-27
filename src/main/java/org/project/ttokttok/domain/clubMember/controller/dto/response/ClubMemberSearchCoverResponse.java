package org.project.ttokttok.domain.clubMember.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberSearchServiceResponse;

import java.util.List;

public record ClubMemberSearchCoverResponse(
        List<ClubMemberSearchResponse> clubMembers
) {
    public static ClubMemberSearchCoverResponse from(List<ClubMemberSearchServiceResponse> response) {
        return new ClubMemberSearchCoverResponse(
                response.stream()
                        .map(ClubMemberSearchResponse::from)
                        .toList()
        );
    }
}

@Builder
record ClubMemberSearchResponse(
        String id,
        Grade grade,
        String name,
        String major,
        MemberRole role
) {
    public static ClubMemberSearchResponse from(ClubMemberSearchServiceResponse response) {
        return ClubMemberSearchResponse.builder()
                .id(response.id())
                .grade(response.grade())
                .name(response.name())
                .major(response.major())
                .role(response.role())
                .build();
    }
}
