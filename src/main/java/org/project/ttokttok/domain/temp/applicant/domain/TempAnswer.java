package org.project.ttokttok.domain.temp.applicant.domain;

import jakarta.annotation.Nullable;

public record TempAnswer(
        String questionId,

        @Nullable
        Object value
) {
}
