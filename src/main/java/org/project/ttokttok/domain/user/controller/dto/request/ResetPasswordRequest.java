package org.project.ttokttok.domain.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.project.ttokttok.domain.user.service.dto.request.ResetPasswordServiceRequest;
import org.project.ttokttok.global.annotation.validation.StrongPassword;

public record ResetPasswordRequest(
        @NotBlank(message = "이메일이 비어 있습니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증코드가 비어 있습니다.")
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자여야 합니다.")
        String verificationCode,

        @NotBlank(message = "새 비밀번호가 비어 있습니다.")
        @StrongPassword
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인이 비어 있습니다.")
        String newPasswordConfirm
) {
    public ResetPasswordServiceRequest toServiceRequest() {
        return ResetPasswordServiceRequest.builder()
                .email(email)
                .verificationCode(verificationCode)
                .newPassword(newPassword)
                .newPasswordConfirm(newPasswordConfirm)
                .build();
    }
}
