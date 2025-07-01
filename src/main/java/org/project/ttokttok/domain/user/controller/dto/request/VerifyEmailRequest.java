package org.project.ttokttok.domain.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank(message = "이메일이 비어 있습니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증코드가 비어 있습니다.")
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자여야 합니다.")
        String code
) {
}
