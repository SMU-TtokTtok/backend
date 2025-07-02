package org.project.ttokttok.domain.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendVerificationRequest(
        @NotBlank(message="이메일이 비어 있습니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email
) {
}
