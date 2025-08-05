package org.project.ttokttok.global.annotationresolver.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;

import static org.project.ttokttok.global.auth.jwt.TokenProperties.*;
import static org.project.ttokttok.global.auth.security.RootApiEndpoint.API_ADMIN;

@Configuration
@RequiredArgsConstructor
public class AuthUserInfoResolver implements HandlerMethodArgumentResolver {

    private final TokenProvider tokenProvider;

    // AuthUserInfo 라는 어노테이션이 파라미터에 달려있는지 확인하는 메서드.
    // spring Mvc 기능이다.
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUserInfo.class);
    }

    // 위쪽 메서드가 참일 때 작동하는 메서드
    // 요청 헤더에 있는 토큰을 추출하여 사용자 이름을 얻어온다.
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        // web 기본 요청 획득
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        // Cookies null 체크 추가
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        // 쿠키 값 토큰 분기 처리 추가
        String tokenFromCookie = getTokenFromCookie(request);

        // "Authorization" 헤더 값 받아옴.
        String token = Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(tokenFromCookie))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (token != null) {
            try {
                return tokenProvider.getUsernameFromToken(token);
            } catch (Exception e) {
                // 토큰이 만료되었거나 잘못된 경우 null 반환
                // (컨트롤러에서 null 체크하여 401 처리)
                return null;
            }
        }

        return null;
    }

    private String getTokenFromCookie(HttpServletRequest request) {
        return request.getRequestURI().contains(API_ADMIN.getValue()) ?
                ACCESS_TOKEN_COOKIE.getValue()
                : USER_ACCESS_TOKEN_COOKIE.getValue();
    }
}
