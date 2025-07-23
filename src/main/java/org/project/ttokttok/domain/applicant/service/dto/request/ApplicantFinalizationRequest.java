package org.project.ttokttok.domain.applicant.service.dto.request;

public record ApplicantFinalizationRequest(
        String username,
        String clubId
) {
    public static ApplicantFinalizationRequest of(String username, String clubId) {
        return new ApplicantFinalizationRequest(username, clubId);
    }
}
