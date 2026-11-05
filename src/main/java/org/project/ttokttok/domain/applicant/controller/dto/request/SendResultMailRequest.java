package org.project.ttokttok.domain.applicant.controller.dto.request;

import jakarta.validation.Valid;
import org.project.ttokttok.domain.applicant.service.dto.request.SendResultMailServiceRequest;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;

public record SendResultMailRequest(
        @Valid
        MailFormatRequest pass,

        @Valid
        MailFormatRequest fail
) {
    public SendResultMailServiceRequest toServiceRequest() {
        return SendResultMailServiceRequest.builder()
                .pass(toContent(pass))
                .fail(toContent(fail))
                .build();
    }

    private static ResultMailContent toContent(MailFormatRequest format) {
        return format == null ? null : new ResultMailContent(format.title(), format.body());
    }
}

