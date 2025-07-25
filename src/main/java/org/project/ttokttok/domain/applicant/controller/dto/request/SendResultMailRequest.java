package org.project.ttokttok.domain.applicant.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.project.ttokttok.domain.applicant.service.dto.request.SendResultMailServiceRequest;

public record SendResultMailRequest(
        @NotBlank(message = "합격 메일 본문은 필수입니다.")
        String passBody,

        @NotBlank(message = "불합격 메일 본문은 필수입니다.")
        String failBody
) {
    public SendResultMailServiceRequest toServiceRequest() {
        return SendResultMailServiceRequest.builder()
                .passBody(passBody)
                .failBody(failBody)
                .build();
    }
}
