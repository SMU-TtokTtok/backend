package org.project.ttokttok.domain.superadmin.service.dto.response;

import lombok.Builder;

@Builder
public record SuperAdminLoginServiceResponse(
        String accessToken,
        String refreshToken
) {
    public static SuperAdminLoginServiceResponse of(String accessToken, String refreshToken) {
        return SuperAdminLoginServiceResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
