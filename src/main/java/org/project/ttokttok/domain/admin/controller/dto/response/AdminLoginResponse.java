package org.project.ttokttok.domain.admin.controller.dto.response;

import lombok.Builder;

@Builder
public record AdminLoginResponse(
        String clubId,
        String clubName,
        String accessToken,
        String refreshToken
) {
    public static AdminLoginResponse of(final String clubId, final String clubName, final String accessToken, final String refreshToken) {
        return AdminLoginResponse.builder()
                .clubId(clubId)
                .clubName(clubName)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
