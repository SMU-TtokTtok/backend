package org.project.ttokttok.global.util;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieUtil {

    // 리프레시 토큰을 내려주는 응답 쿠키를 생성, 삭제 할 때 활용(로그인 or 로그아웃)
    private static final String ROOT = "/";
    private static final String EXPIRE_COOKIE_VALUE = "";

    // 쿠키 생성
    public static ResponseCookie createResponseCookie(String key, String content, Duration expiry) {
        return ResponseCookie.from(key, content)
                .httpOnly(true)
                .secure(true)
                .path(ROOT)
                .maxAge(expiry)
                .build();
    }

    // 쿠키 만료 시키기
    public static ResponseCookie exireResponseCookie(String key) {
        return createResponseCookie(key, EXPIRE_COOKIE_VALUE, Duration.ZERO);
    }
}
