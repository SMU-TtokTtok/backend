package org.project.ttokttok.global.auth.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

import static org.project.ttokttok.global.auth.jwt.TokenProperties.AUTH_HEADER;
import static org.project.ttokttok.global.auth.jwt.TokenProperties.BEARER_PREFIX;

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

        log.info("[JWT 인증 필터 실행]");

        // 토큰 헤더에서 액세스 토큰 추출
        final String AuthorizationHeader = request.getHeader(AUTH_HEADER.getValue());
        final String token = getAccessToken(AuthorizationHeader);

        // 올바른 토큰일 시, 스프링 시큐리티 컨텍스트에 인증 설정.
        if (tokenProvider.validateToken(token)) {
            UserProfileResponse profile = tokenProvider.getUserProfile(token);
            Authentication authentication = jwtAuthManager.getAuthentication(profile.username(), profile.role());

            log.info("[JWT 인증 완료] username(email)={}, role={}", profile.username(), profile.role());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 필터 인증을 계속 진행함.
        filterChain.doFilter(request, response);
    }

    // 액세스 토큰 접두사 추출
    private String getAccessToken(String header) {
        if (header != null && header.startsWith(BEARER_PREFIX.getValue())) {
            return header.substring(BEARER_PREFIX.getValue().length());
        }

        return null;
    }
}
