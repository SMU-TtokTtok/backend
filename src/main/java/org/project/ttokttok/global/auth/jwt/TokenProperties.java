package org.project.ttokttok.global.auth.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenProperties {
    //토큰 관련 상슈
    AUTH_HEADER("Authorization"),
    REFRESH_KEY("ttref"),
    ACCESS_TOKEN_COOKIE("ttac"); // 액세스 토큰 쿠키 이름 추가

    final String value;
}
