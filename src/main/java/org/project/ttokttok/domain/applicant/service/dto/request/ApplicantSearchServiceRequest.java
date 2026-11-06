package org.project.ttokttok.domain.applicant.service.dto.request;

import org.project.ttokttok.domain.applicant.domain.enums.Sort;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import lombok.Builder;

@Builder
public record ApplicantSearchServiceRequest(
        String username,
        String searchKeyword,
        Sort sortCriteria,
        boolean isEvaluating,
        int cursor,
        int size,
        ApplicantPhase kind
) {
    public static ApplicantSearchServiceRequest of(String username,
                                                   String searchKeyword,
                                                   Sort sortCriteria,
                                                   boolean isEvaluating,
                                                   int cursor,
                                                   int size,
                                                   ApplicantPhase kind
    ) {
        return ApplicantSearchServiceRequest.builder()
                .username(username)
                .searchKeyword(searchKeyword)
                .sortCriteria(sortCriteria)
                .isEvaluating(isEvaluating)
                .cursor(cursor)
                .size(size)
                .kind(kind)
                .build();
    }
}
