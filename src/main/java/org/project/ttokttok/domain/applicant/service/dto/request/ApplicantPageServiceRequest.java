package org.project.ttokttok.domain.applicant.service.dto.request;

import org.project.ttokttok.domain.applicant.domain.enums.Sort;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import lombok.Builder;

@Builder
public record ApplicantPageServiceRequest(
        String username,
        Sort sortCriteria,
        boolean isEvaluating,
        int cursor,
        int size,
        ApplicantPhase kind
) {
    public static ApplicantPageServiceRequest of(String username,
                                                 Sort sortCriteria,
                                                 boolean isEvaluating,
                                                 int cursor,
                                                 int size,
                                                 ApplicantPhase kind) {
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