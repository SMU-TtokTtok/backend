package org.project.ttokttok.global.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenExpiry {
    ACCESS_TOKEN_EXPIRY_TIME(60 * 60 * 1000L), // 1시간
    REFRESH_TOKEN_EXPIRY_TIME(7 * 24 * 60 * 60 * 1000L); // 7일

    final Long expiry;
}
