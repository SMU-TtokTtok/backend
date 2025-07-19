package org.project.ttokttok.domain.admin.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;

@Builder
public record AdminLoginServiceResponse(
        String accessToken,
        String refreshToken,
        String clubId,
        String clubName
) {
    public static AdminLoginServiceResponse of(final TokenResponse tokens, String clubId, String clubName) {
        return AdminLoginServiceResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .clubId(clubId)
                .clubName(clubName)
                .build();
    }
}
