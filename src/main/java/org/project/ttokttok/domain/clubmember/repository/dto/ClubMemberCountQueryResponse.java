package org.project.ttokttok.domain.clubmember.repository.dto;

import lombok.Builder;

@Builder
public record ClubMemberCountQueryResponse(
        int totalCount,
        int firstGradeCount,
        int secondGradeCount,
        int thirdGradeCount,
        int fourthGradeCount
) {
}
