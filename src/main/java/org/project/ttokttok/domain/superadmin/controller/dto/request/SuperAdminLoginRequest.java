package org.project.ttokttok.domain.superadmin.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.project.ttokttok.domain.superadmin.service.dto.request.SuperAdminLoginServiceRequest;

public record SuperAdminLoginRequest(
        @NotBlank(message = "아이디가 비어 있습니다.")
        String username,

        @NotBlank(message = "비밀번호가 비어 있습니다.")
        String password
) {
    public SuperAdminLoginServiceRequest toServiceRequest() {
        return SuperAdminLoginServiceRequest.builder()
                .username(username)
                .password(password)
                .build();
    }
}
