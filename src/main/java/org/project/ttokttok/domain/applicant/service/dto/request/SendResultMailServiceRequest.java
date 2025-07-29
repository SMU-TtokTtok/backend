package org.project.ttokttok.domain.applicant.service.dto.request;

import lombok.Builder;
import org.project.ttokttok.domain.applicant.controller.dto.request.MailFormatRequest;

@Builder
public record SendResultMailServiceRequest(
        MailFormatRequest pass,
        MailFormatRequest fail
) {
}
