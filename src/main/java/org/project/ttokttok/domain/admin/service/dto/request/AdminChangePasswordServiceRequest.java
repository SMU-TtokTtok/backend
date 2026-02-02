package org.project.ttokttok.domain.admin.service.dto.request;

import lombok.Builder;

@Builder
public record AdminChangePasswordServiceRequest(
        String currentPassword,
        String newPassword,
        String newPasswordConfirm
) {
}
