package org.project.ttokttok.domain.applicant.service.dto.request;

import lombok.Builder;

@Builder
public record ApplicantStatusServiceRequest(
        String username,
        boolean isPassed,
        int page,
        int size,
        String kind
) {
    public static ApplicantStatusServiceRequest of(String username,
                                                   boolean isPassed,
                                                   int page,
                                                   int size,
                                                   String kind
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


