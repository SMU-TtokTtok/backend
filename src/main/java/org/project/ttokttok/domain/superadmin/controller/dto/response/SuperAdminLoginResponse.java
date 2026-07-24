package org.project.ttokttok.domain.superadmin.controller.dto.response;

public record SuperAdminLoginResponse(
        String accessToken,
        String refreshToken
) {
    public static SuperAdminLoginResponse of(String accessToken, String refreshToken) {
        return new SuperAdminLoginResponse(accessToken, refreshToken);
    }
}
