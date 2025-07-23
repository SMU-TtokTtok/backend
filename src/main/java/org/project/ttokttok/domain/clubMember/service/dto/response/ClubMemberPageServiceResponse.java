package org.project.ttokttok.domain.clubMember.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberPageQueryResponse;

import java.util.List;

@Builder
public record ClubMemberPageServiceResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        List<ClubMember> clubMembers
) {
    public static ClubMemberPageServiceResponse from(ClubMemberPageQueryResponse response) {
        return ClubMemberPageServiceResponse.builder()
                .currentPage(response.currentPage())
                .totalPage(response.totalPage())
                .totalCount(response.totalCount())
                .clubMembers(response.clubMembers())
                .build();
    }
}
