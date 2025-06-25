package org.project.ttokttok.global.auth.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenExpiry {
    // 토큰 만료 시간 - 밀리초
    ACCESS_TOKEN_EXPIRY_TIME(15 * 60 * 1000L), // 15분
    REFRESH_TOKEN_EXPIRY_TIME(7 * 24 * 60 * 60 * 1000L); // 7일

    final Long expiry;
}
