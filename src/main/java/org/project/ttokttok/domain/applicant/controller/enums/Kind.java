package org.project.ttokttok.domain.applicant.controller.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Kind {
    
    DOCUMENT("DOCUMENT"),
    INTERVIEW("INTERVIEW");

    final String value;
}
