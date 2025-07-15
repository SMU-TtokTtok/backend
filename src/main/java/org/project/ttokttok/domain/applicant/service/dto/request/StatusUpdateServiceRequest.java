package org.project.ttokttok.domain.applicant.service.dto.request;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.domain.enums.Status;

@Builder
public record StatusUpdateServiceRequest(
        String username,
        String applicantId,
        Status status
) {
    public static StatusUpdateServiceRequest of(String username,
                                                String applicantId,
                                                Status status) {
        return StatusUpdateServiceRequest.builder()
                .username(username)
                .applicantId(applicantId)
                .status(status)
                .build();
    }
}
