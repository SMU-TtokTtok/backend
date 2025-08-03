package org.project.ttokttok.domain.clubMember.controller.dto.response;

import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberListResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberPageServiceResponse;

import java.util.List;

public record ClubMemberPageResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        List<ClubMemberListResponse> clubMembers
) {
    public static ClubMemberPageResponse from(ClubMemberPageServiceResponse response) {
        return new ClubMemberPageResponse(
                response.currentPage(),
                response.totalPage(),
                response.totalCount(),
                response.clubMembers()
        );
    }
}


