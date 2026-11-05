package org.project.ttokttok.domain.applicant.domain.enums;

import org.project.ttokttok.domain.applicant.exception.InvalidKindException;

public enum ApplicantPhase {
    DOCUMENT, // 서류 단계
    INTERVIEW; // 면접 단계

    public static ApplicantPhase from(String value) {
        return switch (value.toUpperCase()) {
            case "DOCUMENT" -> DOCUMENT;
            case "INTERVIEW" -> INTERVIEW;
            default -> throw new InvalidKindException();
        };
    }
}
