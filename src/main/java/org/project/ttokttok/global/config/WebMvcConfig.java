package org.project.ttokttok.global.config;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthUserInfoResolver authUserInfoResolver;

    // Spring Mvc 기능 중 하나.
    // AuthUserInfoResolver 클래스를 파라미터 리스트에 추가함으로써
    // 작동하도록 설정(추가 안하면 작동 안함)
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserInfoResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 허용
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true) // 자격 증명(쿠키, 인증 헤더 등) 허용
                .maxAge(3600); // pre-flight 요청 캐싱 시간
    }
}
