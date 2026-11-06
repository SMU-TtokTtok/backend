package org.project.ttokttok.domain.applicant.service.dto.request;

import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import lombok.Builder;

@Builder
public record ApplicantStatusServiceRequest(
        String username,
        boolean isPassed,
        int page,
        int size,
        ApplicantPhase kind
) {
    public static ApplicantStatusServiceRequest of(String username,
                                                   boolean isPassed,
                                                   int page,
                                                   int size,
                                                   ApplicantPhase kind
    ) {
        return ApplicantStatusServiceRequest.builder()
                .username(username)
                .isPassed(isPassed)
                .page(page)
                .size(size)
                .kind(kind)
                .build();
    }
}


