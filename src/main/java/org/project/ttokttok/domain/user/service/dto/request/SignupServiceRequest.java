package org.project.ttokttok.domain.user.service.dto.request;

import lombok.Builder;

@Builder
public record SignupServiceRequest(
        String email,
        String verificationCode,
        String password,
        String passwordConfirm,
        String name,
        boolean termsAgreed
) {
}
