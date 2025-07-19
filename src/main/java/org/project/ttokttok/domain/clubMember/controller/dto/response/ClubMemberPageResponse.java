package org.project.ttokttok.domain.clubMember.controller.dto.response;

import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberPageServiceResponse;

import java.util.List;

public record ClubMemberPageResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        int firstGradeCount,
        int secondGradeCount,
        int thirdGradeCount,
        int fourthGradeCount,
        List<ClubMemberListResponse> clubMembers
) {
    public static ClubMemberPageResponse from(ClubMemberPageServiceResponse response) {
        List<ClubMemberListResponse> memberListResponses = response.clubMembers().stream()
                .map(member -> new ClubMemberListResponse(
                        member.getId(),
                        member.getUser().getName(),
                        member.getGrade(),
                        member.getMajor(),
                        member.getRole()))
                .toList();

        return new ClubMemberPageResponse(
                response.currentPage(),
                response.totalPage(),
                response.totalCount(),
                response.firstGradeCount(),
                response.secondGradeCount(),
                response.thirdGradeCount(),
                response.fourthGradeCount(),
                memberListResponses
        );
    }
}

record ClubMemberListResponse(
        String memberId,
        String name,
        Grade grade,
        String major,
        MemberRole role
){
}


