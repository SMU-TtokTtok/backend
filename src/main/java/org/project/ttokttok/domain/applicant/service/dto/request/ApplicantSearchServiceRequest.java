package org.project.ttokttok.domain.applicant.service.dto.request;

import lombok.Builder;

@Builder
public record ApplicantSearchServiceRequest(
        String username,
        String searchKeyword,
        String sortCriteria,
        boolean isEvaluating,
        int cursor,
        int size
) {
    public static ApplicantSearchServiceRequest of(String username,
                                                   String searchKeyword,
                                                   String sortCriteria,
                                                   boolean isEvaluating,
                                                   int cursor,
                                                   int size
    ) {
        return ApplicantSearchServiceRequest.builder()
                .username(username)
                .searchKeyword(searchKeyword)
                .sortCriteria(sortCriteria)
                .isEvaluating(isEvaluating)
                .cursor(cursor)
                .size(size)
                .build();
    }
}
