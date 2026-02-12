package org.project.ttokttok.global.auth.jwt.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.global.auth.jwt.dto.request.TokenRequest;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.dto.response.UserProfileResponse;
import org.project.ttokttok.global.auth.jwt.exception.InvalidIssuerException;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.project.ttokttok.global.auth.jwt.TokenExpiry.ACCESS_TOKEN_EXPIRY_TIME;

@Slf4j
@Service
public class TokenProvider {

    // JWT 발급 및 검증을 위한 클래스

    @Value("${jwt.issuer}")
    private String issuer;

    // JWT 암호화 / 복호화에 사용
    private final Key key;
    private final RefreshTokenRedisService refreshTokenRedisService;

    public TokenProvider(@Value("${jwt.secret}") String secret, RefreshTokenRedisService refreshTokenRedisService) {
        // 시크릿값 base64 디코딩
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.refreshTokenRedisService = refreshTokenRedisService;
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        // 토큰이 null이거나 빈 문자열인 경우 즉시 false 반환
        if (token == null || token.trim().isEmpty()) {
            log.debug("JWT 토큰이 비어있습니다.");
            return false;
        }

        // 블랙리스트 확인
        if (refreshTokenRedisService.isAccessTokenBlacklisted(token)) {
            log.warn("블랙리스트에 등록된 토큰입니다.");
            return false;
        }

        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            // 유효한 이슈어인지 검증
            isValidIssuer(jws.getBody().getIssuer());

            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 토큰 입니다.", e);
        } catch (SignatureException e) {
            log.warn("JWT 서명 검증에 실패하였습니다.", e);
        } catch (InvalidIssuerException e) {
            log.warn("유효하지 않은 이슈어(발급자) 입니다.", e);
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰이 만료되었습니다.", e);
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있습니다.", e);
        } catch (NullPointerException e) {
            log.warn("헤더 값이 잘못되었습니다.", e);
        }

        return false;
    }

    // 토큰 생성
    public TokenResponse generateToken(TokenRequest request) {
        String accessToken = generateAccessToken(request.username(), request.role());
        String refreshToken = generateRefreshToken();

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 토큰에서 사용자 정보 추출
    public UserProfileResponse getUserProfile(String token) {
        return UserProfileResponse.builder()
                .username(getUsernameFromToken(token))
                .role(getRoleFromToken(token))
                .build();
    }

    // 토큰 재발급
    public TokenResponse reissueToken(String refreshToken, Role role) {
        // 토큰 값을 기반으로 사용자 이름 혹은 이메일을 조회
        String username = refreshTokenRedisService.getUsernameFromRefreshToken(refreshToken);
        String accessToken = generateAccessToken(username, role);

        return TokenResponse.of(accessToken, refreshToken);
    }

    // 토큰에서 사용자 이름을 받아옴.
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // 토큰에서 사용자 역할 추출
    private String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    // JWT 서명 키 반환
    public Key getKey() {
        return this.key;
    }

    // 유효한 이슈어(발급자)인지 검증
    private void isValidIssuer(String issuer) {
        if (!issuer.equals(this.issuer))
            throw new InvalidIssuerException();
    }

    // 액세스 토큰 생성 로직
    private String generateAccessToken(String username, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRY_TIME.getExpiry());
        // 토큰 삽입은 그냥 인코딩된 issuer.
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("role", role.toString())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 리프레시 토큰은 Redis에 저장하고, 키값이 있기에 순수한 랜덤 문자열로 반환
    private String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    // 클레임 추출 - jwt 복호화
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}