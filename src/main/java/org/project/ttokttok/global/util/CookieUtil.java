package org.project.ttokttok.global.util;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static org.project.ttokttok.global.auth.jwt.TokenProperties.ACCESS_TOKEN_COOKIE;
import static org.project.ttokttok.global.auth.jwt.TokenProperties.REFRESH_KEY;

@Component
public class CookieUtil {

    public static ResponseCookie createResponseCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                //.secure(true) // HTTPS에서만 전송
                .secure(false) // FIXME: 프론트 테스트 용으로 비활성화
                //.sameSite("Strict") // 동일 사이트에서만 쿠키 전송
                .sameSite("None") // 개발용
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public static ResponseCookie exireResponseCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                //.secure(true)
                .secure(false) // FIXME: 프론트 테스트 용으로 비활성화
                //.sameSite("Strict")
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
    }

    // 액세스 토큰과 리프레시 토큰을 모두 만료시키는 메서드
    public static ResponseCookie[] expireBothTokenCookies() {
        ResponseCookie expiredAccessCookie = exireResponseCookie(ACCESS_TOKEN_COOKIE.getValue());
        ResponseCookie expiredRefreshCookie = exireResponseCookie(REFRESH_KEY.getValue());

        return new ResponseCookie[] { expiredAccessCookie, expiredRefreshCookie };
    }
}
