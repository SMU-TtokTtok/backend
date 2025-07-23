package org.project.ttokttok.domain.applicant.controller.dto.response;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantFinalizeServiceResponse;

@Builder
public record ApplicantFinalizeResponse(
        int passedCount,
        int totalFinalizedCount
) {
    public static ApplicantFinalizeResponse from(ApplicantFinalizeServiceResponse response) {
        return ApplicantFinalizeResponse.builder()
                .passedCount(response.passedCount())
                .totalFinalizedCount(response.totalFinalizedCount())
                .build();
    }
}
