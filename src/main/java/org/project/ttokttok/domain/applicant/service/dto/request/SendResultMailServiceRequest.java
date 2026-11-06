package org.project.ttokttok.domain.applicant.service.dto.request;

import lombok.Builder;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;

@Builder
public record SendResultMailServiceRequest(
        ResultMailContent pass,
        ResultMailContent fail
) {
}
