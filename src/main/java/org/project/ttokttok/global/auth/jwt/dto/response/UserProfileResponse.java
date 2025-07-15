package org.project.ttokttok.global.auth.jwt.dto.response;

import lombok.Builder;

@Builder
public record UserProfileResponse(
        String username,
        String role
) {
}
