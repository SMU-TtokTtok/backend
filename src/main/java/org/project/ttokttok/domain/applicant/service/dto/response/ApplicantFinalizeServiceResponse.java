package org.project.ttokttok.domain.applicant.service.dto.response;

import lombok.Builder;

@Builder
public record ApplicantFinalizeServiceResponse(
        int passedCount,
        int totalFinalizedCount
) {
    public static ApplicantFinalizeServiceResponse of(int passedCount, int totalFinalizedCount) {
        return ApplicantFinalizeServiceResponse.builder()
                .passedCount(passedCount)
                .totalFinalizedCount(totalFinalizedCount)
                .build();
    }
}
