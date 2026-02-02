package org.project.ttokttok.domain.admin.service.dto.request;

import lombok.Builder;

@Builder
public record AdminResetPasswordServiceRequest(
        String username,
        String newPassword,
        String newPasswordConfirm
) {
}
