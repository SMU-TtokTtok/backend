package org.project.ttokttok.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static org.project.ttokttok.global.auth.jwt.TokenProperties.ACCESS_TOKEN_COOKIE;
import static org.project.ttokttok.global.auth.jwt.TokenProperties.REFRESH_KEY;

@Component
public class CookieUtil {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    public ResponseCookie createResponseCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie expireResponseCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
    }

    // 액세스 토큰과 리프레시 토큰을 모두 만료시키는 메서드
    public ResponseCookie[] expireBothTokenCookies() {
        ResponseCookie expiredAccessCookie = expireResponseCookie(ACCESS_TOKEN_COOKIE.getValue());
        ResponseCookie expiredRefreshCookie = expireResponseCookie(REFRESH_KEY.getValue());

        return new ResponseCookie[] { expiredAccessCookie, expiredRefreshCookie };
    }

    // 정적 메서드 (기존 호환성 유지)
    public static ResponseCookie createResponseCookieStatic(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(isSecure())
                .sameSite(isSecure() ? "None" : "Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public static ResponseCookie expireResponseCookieStatic(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(isSecure())
                .sameSite(isSecure() ? "None" : "Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    // 환경에 따라 secure 설정 결정 (기존 로직 유지)
    private static boolean isSecure() {
        String profile = System.getProperty("spring.profiles.active", "dev");
        return "prod".equals(profile) || "production".equals(profile);
    }
}
