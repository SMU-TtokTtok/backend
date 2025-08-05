package org.project.ttokttok.global.auth.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.global.auth.jwt.dto.response.UserProfileResponse;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

import static org.project.ttokttok.global.auth.jwt.TokenProperties.ACCESS_TOKEN_COOKIE;
import static org.project.ttokttok.global.auth.jwt.TokenProperties.USER_ACCESS_TOKEN_COOKIE;
import static org.project.ttokttok.global.auth.security.RootApiEndpoint.API_ADMIN;
import static org.project.ttokttok.global.auth.security.SecurityWhiteList.ALLOW_URLS;
import static org.project.ttokttok.global.auth.security.SecurityWhiteList.SWAGGER_URLS;

@Slf4j
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    // jwt 인증 필터
    // 스프링 시큐리티의 필터 적용 전에 요청을 가로채 검증 진행.
    // 시큐리티 필터 측 이전에 검증하기에, 토큰이 존재하지 않으면 예외가 발생함.

    private final TokenProvider tokenProvider;
    private final JwtAuthenticationManager jwtAuthManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.debug("[JWT 인증 필터 실행] URI: {}", request.getRequestURI());

        // 쿠키에서 액세스 토큰 추출
        final String token = getAccessTokenFromCookie(request);

        // 토큰이 존재하고 올바른 토큰일 시, 스프링 시큐리티 컨텍스트에 인증 설정.
        if (token != null && tokenProvider.validateToken(token)) {
            UserProfileResponse profile = tokenProvider.getUserProfile(token);
            Authentication authentication = jwtAuthManager.getAuthentication(profile.username(), profile.role());

            log.info("[JWT 인증 완료] username(email)={}, role={}", profile.username(), profile.role());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 필터 인증을 계속 진행함.
        filterChain.doFilter(request, response);
    }

    // 쿠키에서 액세스 토큰 추출 (관리자용 또는 사용자용)
    private String getAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String requestURI = request.getRequestURI();

            if (requestURI.contains(API_ADMIN.getValue())) {
                return getTokenFromCookies(cookies, ACCESS_TOKEN_COOKIE.getValue());
            } else {
                return getTokenFromCookies(cookies, USER_ACCESS_TOKEN_COOKIE.getValue());
            }
        }
        return null;
    }

    private String getTokenFromCookies(Cookie[] cookies, String cookieName) {
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // 특정 경로를 필터링하지 않도록 설정
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();

        return Arrays.stream(ALLOW_URLS.getEndPoints()).anyMatch(requestURI::startsWith) ||
                Arrays.stream(SWAGGER_URLS.getEndPoints()).anyMatch(requestURI::startsWith);
    }
}
