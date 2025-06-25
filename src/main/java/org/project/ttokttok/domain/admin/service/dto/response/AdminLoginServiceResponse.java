package org.project.ttokttok.domain.admin.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;

@Builder
public record AdminLoginServiceResponse(
        String accessToken,
        String refreshToken
) {
    public static AdminLoginServiceResponse from(final TokenResponse tokens) {
        return AdminLoginServiceResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .build();
    }
}
