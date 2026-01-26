package org.project.ttokttok.domain.temp.applicant.controller.dto.request;

import jakarta.annotation.Nullable;

public record TempAnswer(
        String questionId,

        @Nullable
        Object value
) {
}
