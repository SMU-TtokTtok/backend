package org.project.ttokttok.domain.applicant.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MailFormatRequest(
        @NotBlank(message = "합격 메일 제목은 필수입니다.")
        String title,

        @NotBlank(message = "합격 메일 본문은 필수입니다.")
        String body
) {
}
