package org.project.ttokttok.domain.applicant.domain.enums;

public enum ApplicationHistorySort {
    LATEST, POPULAR, MEMBER_COUNT;

    public static ApplicationHistorySort from(String value) {
        if ("popular".equals(value)) {
            return POPULAR;
        }
        return "member_count".equals(value) ? MEMBER_COUNT : LATEST;
    }
}
