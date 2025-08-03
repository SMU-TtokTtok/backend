package org.project.ttokttok.domain.clubMember.service.dto.response;

import lombok.Builder;

import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberPageQueryResponse;

import java.util.List;

@Builder
public record ClubMemberPageServiceResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        List<ClubMemberListResponse> clubMembers
) {
    public static ClubMemberPageServiceResponse from(ClubMemberPageQueryResponse response) {
        List<ClubMemberListResponse> memberListResponses = getClubMemberListResponses(response);

        return ClubMemberPageServiceResponse.builder()
                .currentPage(response.currentPage())
                .totalPage(response.totalPage())
                .totalCount(response.totalCount())
                .clubMembers(memberListResponses)
                .build();
    }

    private static List<ClubMemberListResponse> getClubMemberListResponses(ClubMemberPageQueryResponse response) {
        return response.clubMembers().stream()
                .map(member -> new ClubMemberListResponse(
                        member.getId(),
                        member.getUser().getName(),
                        member.getGrade(),
                        member.getMajor(),
                        member.getRole()))
                .toList();
    }
}
