package org.project.ttokttok.domain.applicant.controller.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.exception.InvalidKindException;

@Getter
@RequiredArgsConstructor
public enum Kind {

    DOCUMENT("DOCUMENT"),
    INTERVIEW("INTERVIEW");

    final String value;

    public static boolean isDocument(String value) {
        return switch (value.toUpperCase()) {
            case "DOCUMENT" -> true;
            case "INTERVIEW" -> false;
            default -> throw new InvalidKindException();
        };
    }
}
