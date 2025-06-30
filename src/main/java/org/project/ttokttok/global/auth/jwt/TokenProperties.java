package org.project.ttokttok.global.auth.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenProperties {
    // 토큰 관련 문자열 상수
    AUTH_HEADER("Authorization"),
    BEARER_PREFIX("Bearer "),
    REFRESH_KEY("ttref");

    final String value;
}
