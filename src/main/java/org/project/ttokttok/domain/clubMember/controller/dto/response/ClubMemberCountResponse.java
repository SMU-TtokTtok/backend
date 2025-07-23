package org.project.ttokttok.domain.clubMember.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberCountServiceResponse;

@Builder
public record ClubMemberCountResponse(
        int totalCount,
        int firstGradeCount,
        int secondGradeCount,
        int thirdGradeCount,
        int fourthGradeCount
) {
    public static ClubMemberCountResponse from(ClubMemberCountServiceResponse response) {
        return ClubMemberCountResponse.builder()
                .totalCount(response.totalCount())
                .firstGradeCount(response.firstGradeCount())
                .secondGradeCount(response.secondGradeCount())
                .thirdGradeCount(response.thirdGradeCount())
                .fourthGradeCount(response.fourthGradeCount())
                .build();
    }
}
