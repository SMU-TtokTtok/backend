package org.project.ttokttok.global.auth.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SecurityWhiteList {
    // 시큐리티 설정이 필요없는 URL들 추가하여 사용

    // 필요한 endPoint 추가하여 사용
    ALLOW_URLS(new String[]{
            "/api/admin/auth/login"
    }),

    SWAGGER_URLS(new String[]{
            "/swagger-ui.html",
    });

    final String[] endPoints;
}
