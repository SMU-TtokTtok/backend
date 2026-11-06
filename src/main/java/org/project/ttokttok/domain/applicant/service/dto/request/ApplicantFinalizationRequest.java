package org.project.ttokttok.domain.applicant.service.dto.request;

import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
public record ApplicantFinalizationRequest(
        String username,
        String clubId,
        ApplicantPhase kind
) {
    public static ApplicantFinalizationRequest of(String username, String clubId, ApplicantPhase kind) {
        return new ApplicantFinalizationRequest(username, clubId, kind);
    }
}
