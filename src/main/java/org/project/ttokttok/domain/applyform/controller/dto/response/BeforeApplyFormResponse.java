package org.project.ttokttok.domain.applyform.controller.dto.response;

import org.project.ttokttok.domain.applyform.service.dto.response.BeforeApplyFormServiceResponse;

public record BeforeApplyFormResponse(
        String formId,
        String createdAt
) {
    public static BeforeApplyFormResponse from(BeforeApplyFormServiceResponse response) {
        return new BeforeApplyFormResponse(
                response.formId(),
                response.createdAt().toString()
        );
    }
}
