package org.project.ttokttok.domain.admin.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.project.ttokttok.domain.admin.service.dto.request.AdminResetPasswordServiceRequest;

public record AdminResetPasswordRequest(
        @NotBlank(message = "관리자 아이디가 비어 있습니다.")
        @Size(min = 8, message = "관리자 아이디는 최소 8글자여야 합니다.")
        String username,

        @NotBlank(message = "새 비밀번호가 비어 있습니다.")
        @Size(min = 12, message = "새 비밀번호는 최소 12글자여야 합니다.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인이 비어 있습니다.")
        String newPasswordConfirm
) {
    public AdminResetPasswordServiceRequest toServiceRequest() {
        return AdminResetPasswordServiceRequest.builder()
                .username(username)
                .newPassword(newPassword)
                .newPasswordConfirm(newPasswordConfirm)
                .build();
    }
}
