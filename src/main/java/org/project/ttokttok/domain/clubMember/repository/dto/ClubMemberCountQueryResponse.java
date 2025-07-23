package org.project.ttokttok.domain.clubMember.repository.dto;

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
