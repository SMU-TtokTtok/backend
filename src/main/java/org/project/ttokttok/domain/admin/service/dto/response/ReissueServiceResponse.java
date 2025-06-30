package org.project.ttokttok.domain.admin.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;

@Builder
public record ReissueServiceResponse(
        String accessToken,
        String refreshToken,
        Long refreshTTL
) {
    public static ReissueServiceResponse of(final TokenResponse tokens, final Long refreshTTL) {
        return ReissueServiceResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .refreshTTL(refreshTTL)
                .build();
    }
}
