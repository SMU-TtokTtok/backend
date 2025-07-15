package org.project.ttokttok.global.auth.jwt.dto.request;

import lombok.Builder;
import org.project.ttokttok.global.entity.Role;

@Builder
public record TokenRequest(
        String username,
        Role role
) {
    public static TokenRequest of(String username, Role role) {
        return TokenRequest.builder()
                .username(username)
                .role(role)
                .build();
    }
}
