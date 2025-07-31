package org.project.ttokttok.domain.applicant.service.dto.request;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;

@Builder
public record StatusUpdateServiceRequest(
        String username,
        String applicantId,
        PhaseStatus status,
        String kind
) {
    public static StatusUpdateServiceRequest of(String username,
                                                String applicantId,
                                                PhaseStatus status,
                                                String kind) {
        return StatusUpdateServiceRequest.builder()
                .username(username)
                .applicantId(applicantId)
                .status(status)
                .build();
    }
}
