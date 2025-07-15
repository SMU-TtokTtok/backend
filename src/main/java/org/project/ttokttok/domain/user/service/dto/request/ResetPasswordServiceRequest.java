package org.project.ttokttok.domain.user.service.dto.request;

import lombok.Builder;

@Builder
public record ResetPasswordServiceRequest(
        String email,
        String verificationCode,
        String newPassword,
        String newPasswordConfirm
) {
}
