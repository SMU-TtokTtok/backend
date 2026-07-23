package org.project.ttokttok.global.auth.oauth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

/**
 * 구글 ID 토큰 검증용 JwtDecoder 설정
 *
 * NimbusJwtDecoder 가 구글 JWKS(공개키) 를 가져와 서명을 검증하고(캐시/키 회전 내장, lazy 로딩),
 * 커스텀 밸리데이터로 발급자(iss)와 대상(aud=우리 client-id)을 검증한다.
 * aud 검증은 다른 앱용으로 발급된 구글 토큰의 재사용 공격을 차단한다.
 */
@Configuration
public class GoogleJwtDecoderConfig {

    // 구글 문서상 iss 는 두 형식이 모두 존재한다
    private static final List<String> GOOGLE_ISSUERS =
            List.of("https://accounts.google.com", "accounts.google.com");

    // 시계 오차 허용 범위
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    // 구글 JWKS 조회 타임아웃 - 무제한 대기로 인한 커넥션/스레드 고갈 방지
    private static final int JWKS_CONNECT_TIMEOUT_MS = 2_000;
    private static final int JWKS_READ_TIMEOUT_MS = 3_000;

    @Bean
    public JwtDecoder googleJwtDecoder(
            @Value("${google.oauth.jwk-set-uri}") String jwkSetUri,
            @Value("${google.oauth.client-id}") String clientId) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .restOperations(jwkSetRestTemplate())
                .build();

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(CLOCK_SKEW),
                googleIssuerValidator(),
                audienceValidator(clientId)
        ));

        return decoder;
    }

    // 타임아웃이 설정된 RestTemplate - JWKS 엔드포인트 지연 시 무제한 블로킹 방지
    private RestTemplate jwkSetRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(JWKS_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(JWKS_READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    private OAuth2TokenValidator<Jwt> googleIssuerValidator() {
        return jwt -> {
            String issuer = jwt.getClaimAsString("iss");
            if (issuer != null && GOOGLE_ISSUERS.contains(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_issuer", "구글이 발급한 토큰이 아닙니다.", null));
        };
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(String clientId) {
        return jwt -> {
            if (jwt.getAudience() != null && jwt.getAudience().contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_audience", "이 서비스용으로 발급된 토큰이 아닙니다.", null));
        };
    }
}
