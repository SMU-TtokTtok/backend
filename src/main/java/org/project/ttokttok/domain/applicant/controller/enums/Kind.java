package org.project.ttokttok.domain.applicant.controller.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.exception.InvalidKindException;

@Getter
@RequiredArgsConstructor
public enum Kind {

    DOCUMENT("DOCUMENT"),
    INTERVIEW("INTERVIEW");

    final String value;

    public static ApplicantPhase toApplicantPhase(String value) {
        return switch (value.toUpperCase()) {
            case "DOCUMENT" -> ApplicantPhase.DOCUMENT;
            case "INTERVIEW" -> ApplicantPhase.INTERVIEW;
            default -> throw new InvalidKindException();
        };
    }
}
