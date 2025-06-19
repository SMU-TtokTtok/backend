package org.project.ttokttok.domain.admin.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.project.ttokttok.domain.admin.service.dto.AdminLoginServiceRequest;

public record AdminLoginRequest(
        @NotBlank(message = "아이디가 비어 있습니다.")
        @Size(min = 8, message = "아이디는 최소 8글자여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호가 비어 있습니다.")
        @Size(min = 12, message = "비밀번호는 최소 12글자여야 합니다.")
        String password
) {
    public AdminLoginServiceRequest toServiceRequest() {
        return AdminLoginServiceRequest.builder()
                .username(username)
                .password(password)
                .build();
    }
}
