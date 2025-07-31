package org.project.ttokttok.domain.applicant.service.dto.request;

import lombok.Builder;

@Builder
public record ApplicantPageServiceRequest(
        String username,
        String sortCriteria,
        boolean isEvaluating,
        int cursor,
        int size,
        String kind
) {
    public static ApplicantPageServiceRequest of(String username,
                                                 String sortCriteria,
                                                 boolean isEvaluating,
                                                 int cursor,
                                                 int size,
                                                 String kind) {
        return ApplicantPageServiceRequest.builder()
                .username(username)
                .sortCriteria(sortCriteria)
                .isEvaluating(isEvaluating)
                .cursor(cursor)
                .size(size)
                .kind(kind)
                .build();
    }
}