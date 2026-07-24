package org.project.ttokttok.global.auth.jwt.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.global.auth.jwt.exception.InvalidOnboardingTokenException;
import org.project.ttokttok.global.auth.jwt.exception.OnboardingTokenExpiredException;
import org.project.ttokttok.global.auth.oauth.dto.GoogleUserInfo;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnboardingTokenProviderTest {

    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvclRva2VuUHJvdmlkZXJUZXN0MTIzNDU2Nzg5MA==";
    private static final String TEST_ISSUER = "test-issuer";

    private OnboardingTokenProvider onboardingTokenProvider;
    private Key key;

    private static final GoogleUserInfo USER_INFO = GoogleUserInfo.builder()
            .sub("google-sub-123")
            .email("user@gmail.com")
            .emailVerified(true)
            .name("홍길동")
            .build();

    @BeforeEach
    void setUp() {
        onboardingTokenProvider = new OnboardingTokenProvider(TEST_ISSUER, TEST_SECRET);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
    }

    @Nested
    @DisplayName("generate 메서드")
    class GenerateTest {

        @Test
        @DisplayName("생성한 토큰을 파싱하면 구글 신원 정보가 그대로 복원된다")
        void generate_thenParse_roundTrip() {
            // given
            String token = onboardingTokenProvider.generate(USER_INFO);

            // when
            OnboardingTokenProvider.OnboardingClaims claims = onboardingTokenProvider.parse(token);

            // then
            assertThat(claims.sub()).isEqualTo("google-sub-123");
            assertThat(claims.email()).isEqualTo("user@gmail.com");
            assertThat(claims.name()).isEqualTo("홍길동");
            assertThat(claims.jti()).isNotBlank();
        }

        @Test
        @DisplayName("생성할 때마다 서로 다른 jti가 부여된다")
        void generate_eachToken_hasUniqueJti() {
            // given
            String token1 = onboardingTokenProvider.generate(USER_INFO);
            String token2 = onboardingTokenProvider.generate(USER_INFO);

            // when
            String jti1 = onboardingTokenProvider.parse(token1).jti();
            String jti2 = onboardingTokenProvider.parse(token2).jti();

            // then
            assertThat(jti1).isNotEqualTo(jti2);
        }
    }

    @Nested
    @DisplayName("parse 메서드")
    class ParseTest {

        @Test
        @DisplayName("만료된 토큰이면 OnboardingTokenExpiredException을 던진다")
        void parse_withExpiredToken_throwsExpired() {
            // given
            Date now = new Date();
            String expiredToken = Jwts.builder()
                    .setIssuer(TEST_ISSUER)
                    .setSubject("google-sub-123")
                    .setId("jti-1")
                    .setIssuedAt(new Date(now.getTime() - 20 * 60 * 1000L))
                    .setExpiration(new Date(now.getTime() - 1000L))
                    .claim("token_type", "onboarding")
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            // when & then
            assertThatThrownBy(() -> onboardingTokenProvider.parse(expiredToken))
                    .isInstanceOf(OnboardingTokenExpiredException.class);
        }

        @Test
        @DisplayName("서명이 위조된 토큰이면 InvalidOnboardingTokenException을 던진다")
        void parse_withTamperedToken_throwsInvalid() {
            // given
            String token = onboardingTokenProvider.generate(USER_INFO);
            String tampered = token.substring(0, token.length() - 4) + "abcd";

            // when & then
            assertThatThrownBy(() -> onboardingTokenProvider.parse(tampered))
                    .isInstanceOf(InvalidOnboardingTokenException.class);
        }

        @Test
        @DisplayName("token_type 클레임이 없는 액세스 토큰이면 InvalidOnboardingTokenException을 던진다")
        void parse_withAccessToken_throwsInvalid() {
            // given - 같은 키/이슈어로 서명된 액세스 토큰 (token_type 없음)
            Date now = new Date();
            String accessToken = Jwts.builder()
                    .setIssuer(TEST_ISSUER)
                    .setSubject("user@gmail.com")
                    .setIssuedAt(now)
                    .setExpiration(new Date(now.getTime() + 3600000L))
                    .claim("role", "ROLE_USER")
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            // when & then
            assertThatThrownBy(() -> onboardingTokenProvider.parse(accessToken))
                    .isInstanceOf(InvalidOnboardingTokenException.class);
        }

        @Test
        @DisplayName("발급자가 다른 토큰이면 InvalidOnboardingTokenException을 던진다")
        void parse_withWrongIssuer_throwsInvalid() {
            // given
            Date now = new Date();
            String wrongIssuerToken = Jwts.builder()
                    .setIssuer("wrong-issuer")
                    .setSubject("google-sub-123")
                    .setId("jti-1")
                    .setIssuedAt(now)
                    .setExpiration(new Date(now.getTime() + 600000L))
                    .claim("token_type", "onboarding")
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            // when & then
            assertThatThrownBy(() -> onboardingTokenProvider.parse(wrongIssuerToken))
                    .isInstanceOf(InvalidOnboardingTokenException.class);
        }

        @Test
        @DisplayName("null 또는 빈 토큰이면 InvalidOnboardingTokenException을 던진다")
        void parse_withNullOrBlank_throwsInvalid() {
            assertThatThrownBy(() -> onboardingTokenProvider.parse(null))
                    .isInstanceOf(InvalidOnboardingTokenException.class);
            assertThatThrownBy(() -> onboardingTokenProvider.parse("  "))
                    .isInstanceOf(InvalidOnboardingTokenException.class);
        }
    }
}
