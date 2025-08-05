package org.project.ttokttok.global.annotationresolver.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
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
        String requestURI = request.getRequestURI();
        
        log.debug("[AuthUserInfoResolver] 사용자 정보 추출 시작 - URI: {}", requestURI);

        // Authorization 헤더에서 토큰 추출
        String authorization = request.getHeader("Authorization");
        String token = null;
        
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7); // "Bearer " 제거
            log.debug("[AuthUserInfoResolver] Bearer 토큰 추출 완료 - URI: {}, 토큰 길이: {}", requestURI, token.length());
        } else {
            log.warn("[AuthUserInfoResolver] Authorization 헤더가 없거나 Bearer로 시작하지 않음 - URI: {}, Authorization: {}", 
                    requestURI, authorization);
        }

        if (token != null) {
            try {
                String username = tokenProvider.getUsernameFromToken(token);
                log.debug("[AuthUserInfoResolver] 사용자 정보 추출 완료 - URI: {}, username: {}", requestURI, username);
                return username;
            } catch (Exception e) {
                // 토큰이 만료되었거나 잘못된 경우 null 반환
                // (컨트롤러에서 null 체크하여 401 처리)
                log.error("[AuthUserInfoResolver] 토큰에서 사용자 정보 추출 실패 - URI: {}, error: {}", 
                        requestURI, e.getMessage(), e);
                return null;
            }
        }

        log.debug("[AuthUserInfoResolver] 토큰이 없어 null 반환 - URI: {}", requestURI);
        return null;
    }


}
