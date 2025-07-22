package org.project.ttokttok.domain.clubMember.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberCountQueryResponse;

@Builder
public record ClubMemberCountServiceResponse(
        int totalCount,
        int firstGradeCount,
        int secondGradeCount,
        int thirdGradeCount,
        int fourthGradeCount
) {
    public static ClubMemberCountServiceResponse from(ClubMemberCountQueryResponse response) {
        return ClubMemberCountServiceResponse.builder()
                .totalCount(response.totalCount())
                .firstGradeCount(response.firstGradeCount())
                .secondGradeCount(response.secondGradeCount())
                .thirdGradeCount(response.thirdGradeCount())
                .fourthGradeCount(response.fourthGradeCount())
                .build();
    }
}
