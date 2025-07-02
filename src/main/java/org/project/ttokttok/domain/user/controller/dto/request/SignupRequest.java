package org.project.ttokttok.domain.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.project.ttokttok.global.annotation.validation.StrongPassword;
import org.project.ttokttok.domain.user.service.dto.request.SignupServiceRequest;

public record SignupRequest(
        @NotBlank(message = "이메일이 비어 있습니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증코드가 비어 있습니다.")
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자여야 합니다.")
        String verificationCode,

        @NotBlank(message = "비밀번호가 비어 있습니다.")
        @StrongPassword
        String password,

        @NotBlank(message = "비밀번호 확인이 비어 있습니다.")
        String passwordConfirm,

        @NotBlank(message = "이름이 비어 있습니다.")
        @Size(min = 2, max = 10, message = "이름은 2~10자여야 합니다.")
        String name,

        boolean termsAgreed
) {
    public SignupServiceRequest toServiceRequest() {
        return SignupServiceRequest.builder()
                .email(email)
                .verificationCode(verificationCode)
                .password(password)
                .passwordConfirm(passwordConfirm)
                .name(name)
                .termsAgreed(termsAgreed)
                .build();
    }
}
