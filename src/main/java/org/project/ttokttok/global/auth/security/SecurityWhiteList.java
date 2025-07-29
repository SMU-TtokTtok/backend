package org.project.ttokttok.global.auth.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SecurityWhiteList {
    // 시큐리티 설정이 필요없는 URL들 추가하여 사용

    // 필요한 endPoint 추가하여 사용
    // 토큰을 못쓰는 요청들에만 넣어넣으면 됨
    ALLOW_URLS(new String[]{
            "/api/admin/auth/login",
            "/api/user/auth/send-verification",
            "/api/user/auth/verify-email",
            "/api/user/auth/signup",
            "/api/user/auth/login",
            "/api/user/auth/send-reset-code",
            "/api/user/auth/reset-password",
            "/api/admin/auth/reissue",
            "/health",
            "/api/applies", // 테스트용
            "/api/admin/auth/join" // todo: 추후 삭제 예정 - 관리자 가입 API 엔드포인트
    }),

    SWAGGER_URLS(new String[]{
            "/swagger",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api-docs",
            "/api-docs/**"
    });

    final String[] endPoints;
}
