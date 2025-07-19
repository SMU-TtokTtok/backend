package org.project.ttokttok.domain.admin.controller.dto.response;

import lombok.Builder;

@Builder
public record AdminLoginResponse(
        String clubId,
        String clubName
) {
    public static AdminLoginResponse of(final String clubId, final String clubName) {
        return AdminLoginResponse.builder()
                .clubId(clubId)
                .clubName(clubName)
                .build();
    }
}
