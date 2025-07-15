package org.project.ttokttok.domain.user.service.dto.response;

import lombok.Builder;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;

@Builder
public record LoginServiceResponse(
        String accessToken,
        String refreshToken,
        UserServiceResponse user
) {
    public static LoginServiceResponse from(final TokenResponse tokens, final UserServiceResponse user) {
        return LoginServiceResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .user(user)
                .build();
    }
}
