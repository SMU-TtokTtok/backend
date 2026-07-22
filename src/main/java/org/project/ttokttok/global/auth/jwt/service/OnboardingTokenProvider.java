package org.project.ttokttok.global.auth.jwt.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.global.auth.jwt.exception.InvalidOnboardingTokenException;
import org.project.ttokttok.global.auth.jwt.exception.OnboardingTokenExpiredException;
import org.project.ttokttok.global.auth.oauth.dto.GoogleUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * 구글 OAuth 2단계 온보딩용 단기 토큰 발급/검증 클래스
 *
 * 첫 구글 로그인(신규 사용자)과 온보딩 완료(약관 동의) 사이에서 검증된 구글 신원(sub, email)을
 * 상태 없이 전달한다. token_type=onboarding 클레임으로 액세스 토큰과 구분되며,
 * TokenProvider.validateToken 은 해당 클레임이 있는 토큰을 거부하므로 인증에 재사용될 수 없다.
 */
@Slf4j
@Service
public class OnboardingTokenProvider {

    public static final String TOKEN_TYPE_CLAIM = "token_type";
    public static final String TOKEN_TYPE_ONBOARDING = "onboarding";

    // 온보딩 토큰 유효 시간 10분 (약관 동의 화면 체류 시간)
    public static final long ONBOARDING_TOKEN_EXPIRY_TIME = 10 * 60 * 1000L;

    private final String issuer;
    private final Key key;

    public OnboardingTokenProvider(@Value("${jwt.issuer}") String issuer,
                                   @Value("${jwt.secret}") String secret) {
        this.issuer = issuer;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * 검증된 구글 신원 정보로 온보딩 토큰을 발급한다.
     *
     * @param userInfo 구글 ID 토큰 검증 결과 (sub, email, name)
     * @return 10분 TTL 의 온보딩 토큰 (jti 포함 - 일회성 사용 추적용)
     */
    public String generate(GoogleUserInfo userInfo) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ONBOARDING_TOKEN_EXPIRY_TIME);

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(userInfo.sub())
                .setId(UUID.randomUUID().toString()) // jti - 일회성 사용 추적
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ONBOARDING)
                .claim("email", userInfo.email())
                .claim("name", userInfo.name())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 온보딩 토큰을 검증하고 담긴 구글 신원 정보를 복원한다.
     *
     * @param token 온보딩 토큰
     * @return 토큰에 담긴 구글 신원 정보 (jti 포함)
     * @throws OnboardingTokenExpiredException 토큰 만료 시
     * @throws InvalidOnboardingTokenException 서명 위조, 발급자 불일치, 타입 불일치 등
     */
    public OnboardingClaims parse(String token) {
        Claims claims = parseClaims(token);
        validateClaims(claims);

        return new OnboardingClaims(
                claims.getSubject(),
                claims.get("email", String.class),
                claims.get("name", String.class),
                claims.getId()
        );
    }

    private Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidOnboardingTokenException();
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new OnboardingTokenExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("온보딩 토큰 검증 실패: {}", e.getMessage());
            throw new InvalidOnboardingTokenException();
        }
    }

    private void validateClaims(Claims claims) {
        // 액세스 토큰 등 다른 용도의 토큰이 온보딩 완료 API 에 재사용되는 것을 차단
        if (!TOKEN_TYPE_ONBOARDING.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new InvalidOnboardingTokenException();
        }
        if (!issuer.equals(claims.getIssuer())) {
            throw new InvalidOnboardingTokenException();
        }
    }

    /**
     * 온보딩 토큰에서 복원한 구글 신원 정보
     */
    public record OnboardingClaims(String sub, String email, String name, String jti) {
    }
}
