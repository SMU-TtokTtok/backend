package org.project.ttokttok.domain.applicant.service.dto.request;

import lombok.Builder;

@Builder
public record ApplicantStatusServiceRequest(
        String username,
        boolean isPassed,
        int page,
        int size
) {
    public static ApplicantStatusServiceRequest of(String username,
                                                   boolean isPassed,
                                                   int page,
                                                   int size
    ) {
        return ApplicantStatusServiceRequest.builder()
                .username(username)
                .isPassed(isPassed)
                .page(page)
                .size(size)
                .build();
    }
}


