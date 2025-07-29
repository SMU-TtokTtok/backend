package org.project.ttokttok.domain.applicant.controller.dto.request;

import jakarta.validation.Valid;
import org.project.ttokttok.domain.applicant.service.dto.request.SendResultMailServiceRequest;

public record SendResultMailRequest(
        @Valid
        MailFormatRequest pass,

        @Valid
        MailFormatRequest fail
) {
    public SendResultMailServiceRequest toServiceRequest() {
        return SendResultMailServiceRequest.builder()
                .pass(pass)
                .fail(fail)
                .build();
    }
}

