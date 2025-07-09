package org.project.ttokttok.domain.applyform.service.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record BeforeApplyFormServiceResponse(
        String formId,
        LocalDate createdAt
) {
    public static BeforeApplyFormServiceResponse of(String formId, LocalDate createdAt) {
        return BeforeApplyFormServiceResponse.builder()
                .formId(formId)
                .createdAt(createdAt)
                .build();
    }
}
