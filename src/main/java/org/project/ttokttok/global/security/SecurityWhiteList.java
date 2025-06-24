package org.project.ttokttok.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SecurityWhiteList {
    // 필요한 endPoint 추가하여 사용
    ALLOW_URLS(new String[]{
            "/api/admin/auth/login"
    }),

    SWAGGER_URLS(new String[]{
            "/swagger-ui.html",
    });

    final String[] endPoints;
}
