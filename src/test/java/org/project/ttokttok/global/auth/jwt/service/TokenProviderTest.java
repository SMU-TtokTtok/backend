package org.project.ttokttok.global.auth.jwt.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.global.auth.jwt.dto.request.TokenRequest;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.dto.response.UserProfileResponse;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    @Mock
    private RefreshTokenRedisService refreshTokenRedisService;

    private TokenProvider tokenProvider;

    // 테스트용 시크릿 키 (Base64 인코딩된 256비트 이상의 키)
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvclRva2VuUHJvdmlkZXJUZXN0MTIzNDU2Nzg5MA==";
    private static final String TEST_ISSUER = "test-issuer";
    private static final String TEST_USERNAME = "testuser@example.com";
    private static final Role TEST_ROLE = Role.ROLE_USER;

    private Key key;

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider(TEST_SECRET, refreshTokenRedisService);
        ReflectionTestUtils.setField(tokenProvider, "issuer", TEST_ISSUER);

        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    // ===== validateToken 테스트 =====

    @Nested
    @DisplayName("validateToken 메서드")
    class ValidateTokenTest {

        @Test
        @DisplayName("유효한 토큰이면 true를 반환한다")
        void validateToken_withValidToken_returnsTrue() {
            // given
            String validToken = createValidToken(TEST_USERNAME, TEST_ROLE.toString());
            when(refreshTokenRedisService.isAccessTokenBlacklisted(validToken)).thenReturn(false);

            // when
            boolean result = tokenProvider.validateToken(validToken);

            // then
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("토큰이 null이거나 빈 문자열이면 false를 반환한다")
        void validateToken_withNullOrEmptyToken_returnsFalse(String token) {
            // when
            boolean result = tokenProvider.validateToken(token);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("블랙리스트에 등록된 토큰이면 false를 반환한다")
        void validateToken_withBlacklistedToken_returnsFalse() {
            // given
            String blacklistedToken = createValidToken(TEST_USERNAME, TEST_ROLE.toString());
            when(refreshTokenRedisService.isAccessTokenBlacklisted(blacklistedToken)).thenReturn(true);

            // when
            boolean result = tokenProvider.validateToken(blacklistedToken);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰이면 false를 반환한다")
        void validateToken_withExpiredToken_returnsFalse() {
            // given
            String expiredToken = createExpiredToken(TEST_USERNAME, TEST_ROLE.toString());
            when(refreshTokenRedisService.isAccessTokenBlacklisted(expiredToken)).thenReturn(false);

            // when
            boolean result = tokenProvider.validateToken(expiredToken);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("잘못된 서명의 토큰이면 false를 반환한다")
        void validateToken_withInvalidSignature_returnsFalse() {
            // given
            String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0In0.invalidsignature";
            when(refreshTokenRedisService.isAccessTokenBlacklisted(invalidToken)).thenReturn(false);

            // when
            boolean result = tokenProvider.validateToken(invalidToken);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("잘못된 issuer의 토큰이면 false를 반환한다")
        void validateToken_withInvalidIssuer_returnsFalse() {
            // given
            String tokenWithWrongIssuer = createTokenWithWrongIssuer(TEST_USERNAME, TEST_ROLE.toString());
            when(refreshTokenRedisService.isAccessTokenBlacklisted(tokenWithWrongIssuer)).thenReturn(false);

            // when
            boolean result = tokenProvider.validateToken(tokenWithWrongIssuer);

            // then
            assertThat(result).isFalse();
        }
    }

    // ===== generateToken 테스트 =====

    @Nested
    @DisplayName("generateToken 메서드")
    class GenerateTokenTest {

        @Test
        @DisplayName("유효한 요청이 주어지면 액세스 토큰과 리프레시 토큰을 생성한다")
        void generateToken_withValidRequest_returnsTokens() {
            // given
            TokenRequest request = new TokenRequest(TEST_USERNAME, TEST_ROLE);

            // when
            TokenResponse response = tokenProvider.generateToken(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
        }

        @Test
        @DisplayName("생성된 액세스 토큰은 유효하다")
        void generateToken_generatedAccessToken_isValid() {
            // given
            TokenRequest request = new TokenRequest(TEST_USERNAME, TEST_ROLE);
            TokenResponse response = tokenProvider.generateToken(request);
            when(refreshTokenRedisService.isAccessTokenBlacklisted(response.accessToken())).thenReturn(false);

            // when
            boolean isValid = tokenProvider.validateToken(response.accessToken());

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("생성된 리프레시 토큰은 UUID 형식이다")
        void generateToken_refreshToken_isUUIDFormat() {
            // given
            TokenRequest request = new TokenRequest(TEST_USERNAME, TEST_ROLE);

            // when
            TokenResponse response = tokenProvider.generateToken(request);

            // then
            assertThat(response.refreshToken()).matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            );
        }
    }

    // ===== getUserProfile 테스트 =====

    @Nested
    @DisplayName("getUserProfile 메서드")
    class GetUserProfileTest {

        @Test
        @DisplayName("유효한 토큰에서 사용자 정보를 추출한다")
        void getUserProfile_withValidToken_returnsUserProfile() {
            // given
            String token = createValidToken(TEST_USERNAME, TEST_ROLE.toString());

            // when
            UserProfileResponse profile = tokenProvider.getUserProfile(token);

            // then
            assertThat(profile).isNotNull();
            assertThat(profile.username()).isEqualTo(TEST_USERNAME);
            assertThat(profile.role()).isEqualTo(TEST_ROLE.toString());
        }
    }

    // ===== reissueToken 테스트 =====

    @Nested
    @DisplayName("reissueToken 메서드")
    class ReissueTokenTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 새로운 액세스 토큰을 발급한다")
        void reissueToken_withValidRefreshToken_returnsNewAccessToken() {
            // given
            String refreshToken = "valid-refresh-token";
            when(refreshTokenRedisService.getUsernameFromRefreshToken(refreshToken)).thenReturn(TEST_USERNAME);

            // when
            TokenResponse response = tokenProvider.reissueToken(refreshToken, TEST_ROLE);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isEqualTo(refreshToken);
        }

        @Test
        @DisplayName("재발급된 액세스 토큰은 유효하다")
        void reissueToken_reissuedAccessToken_isValid() {
            // given
            String refreshToken = "valid-refresh-token";
            when(refreshTokenRedisService.getUsernameFromRefreshToken(refreshToken)).thenReturn(TEST_USERNAME);

            TokenResponse response = tokenProvider.reissueToken(refreshToken, TEST_ROLE);
            when(refreshTokenRedisService.isAccessTokenBlacklisted(response.accessToken())).thenReturn(false);

            // when
            boolean isValid = tokenProvider.validateToken(response.accessToken());

            // then
            assertThat(isValid).isTrue();
        }
    }

    // ===== getUsernameFromToken 테스트 =====

    @Nested
    @DisplayName("getUsernameFromToken 메서드")
    class GetUsernameFromTokenTest {

        @Test
        @DisplayName("유효한 토큰에서 사용자 이름을 추출한다")
        void getUsernameFromToken_withValidToken_returnsUsername() {
            // given
            String token = createValidToken(TEST_USERNAME, TEST_ROLE.toString());

            // when
            String username = tokenProvider.getUsernameFromToken(token);

            // then
            assertThat(username).isEqualTo(TEST_USERNAME);
        }
    }

    // ===== Helper Methods =====

    private String createValidToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600000L); // 1시간 후 만료

        return Jwts.builder()
                .setIssuer(TEST_ISSUER)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("role", role)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String createExpiredToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() - 1000L); // 이미 만료됨

        return Jwts.builder()
                .setIssuer(TEST_ISSUER)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("role", role)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String createTokenWithWrongIssuer(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600000L);

        return Jwts.builder()
                .setIssuer("wrong-issuer")
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("role", role)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}