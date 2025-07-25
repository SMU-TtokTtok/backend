package org.project.ttokttok.domain.applicant.service.dto.request;

import lombok.Builder;

@Builder
public record SendResultMailServiceRequest(
        String passBody,
        String failBody
) {
}
