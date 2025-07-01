package org.project.ttokttok.domain.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.project.ttokttok.domain.user.service.dto.request.LoginServiceRequest;

public record LoginRequest(
        @NotBlank(message = "이메일이 비어 있습니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호가 비어 있습니다.")
        String password,

        boolean rememberMe  // 로그인 유지 옵션
) {
    public LoginServiceRequest toServiceRequest() {
        return LoginServiceRequest.builder()
                .email(email)
                .password(password)
                .rememberMe(rememberMe)
                .build();
    }
}
