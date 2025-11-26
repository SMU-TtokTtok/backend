package org.project.ttokttok.global.auth.jwt;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.user.service.UserAuthService;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

// TODO: 파일명을 UserAuthServiceTest.java 로 변경하고
// src/test/java/org/project/ttokttok/domain/user/service/ 경로로 이동해주세요.
@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest { // Renamed class

    @InjectMocks
    private UserAuthService userAuthService;

    @Mock
    private RefreshTokenRedisService refreshTokenRedisService;

    @Mock
    private TokenProvider tokenProvider;

    private SecretKey key;

    @BeforeEach
    void setUp() {
        String secret = "this-is-a-sample-secret-key-for-testing-purpose-only-and-should-be-long-enough";
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("로그아웃 시 액세스 토큰이 블랙리스트에 추가되고 리프레시 토큰이 삭제되어야 한다")
    void logout_withValidTokens_shouldBlacklistTokenAndRemoveRefreshToken() {
        // Given
        long now = System.currentTimeMillis();
        String accessToken = Jwts.builder()
                .setSubject("test@example.com")
                .setExpiration(new Date(now + 3600_000)) // 1 hour
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        String refreshToken = "valid-refresh-token";

        when(tokenProvider.getKey()).thenReturn(key);

        // When
        userAuthService.logout(refreshToken, accessToken);

        // Then
        verify(refreshTokenRedisService).logout(eq(refreshToken), eq(accessToken), anyLong());
    }
}
