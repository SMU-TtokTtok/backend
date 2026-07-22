package org.project.ttokttok.global.auth.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.global.auth.oauth.dto.GoogleUserInfo;
import org.project.ttokttok.global.auth.oauth.exception.InvalidGoogleTokenException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GoogleIdTokenVerifierTest {

    @Mock
    private JwtDecoder googleJwtDecoder;

    @InjectMocks
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Test
    @DisplayName("유효한 ID 토큰이면 구글 사용자 정보를 추출한다")
    void verify_withValidToken_returnsUserInfo() {
        // given
        Jwt jwt = Jwt.withTokenValue("id-token")
                .header("alg", "RS256")
                .subject("google-sub-123")
                .claim("email", "user@gmail.com")
                .claim("email_verified", true)
                .claim("name", "홍길동")
                .build();
        given(googleJwtDecoder.decode("id-token")).willReturn(jwt);

        // when
        GoogleUserInfo userInfo = googleIdTokenVerifier.verify("id-token");

        // then
        assertThat(userInfo.sub()).isEqualTo("google-sub-123");
        assertThat(userInfo.email()).isEqualTo("user@gmail.com");
        assertThat(userInfo.emailVerified()).isTrue();
        assertThat(userInfo.name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("email_verified 클레임이 없으면 미검증으로 취급한다")
    void verify_withoutEmailVerifiedClaim_treatsAsUnverified() {
        // given
        Jwt jwt = Jwt.withTokenValue("id-token")
                .header("alg", "RS256")
                .subject("google-sub-123")
                .claim("email", "user@gmail.com")
                .build();
        given(googleJwtDecoder.decode("id-token")).willReturn(jwt);

        // when
        GoogleUserInfo userInfo = googleIdTokenVerifier.verify("id-token");

        // then
        assertThat(userInfo.emailVerified()).isFalse();
    }

    @Test
    @DisplayName("디코더 검증 실패(서명/만료/iss/aud)면 InvalidGoogleTokenException을 던진다")
    void verify_withDecoderFailure_throwsInvalidGoogleToken() {
        // given
        given(googleJwtDecoder.decode("bad-token")).willThrow(new JwtException("invalid"));

        // when & then
        assertThatThrownBy(() -> googleIdTokenVerifier.verify("bad-token"))
                .isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    @DisplayName("null 또는 빈 토큰이면 InvalidGoogleTokenException을 던진다")
    void verify_withNullOrBlank_throwsInvalidGoogleToken() {
        assertThatThrownBy(() -> googleIdTokenVerifier.verify(null))
                .isInstanceOf(InvalidGoogleTokenException.class);
        assertThatThrownBy(() -> googleIdTokenVerifier.verify(" "))
                .isInstanceOf(InvalidGoogleTokenException.class);
    }
}
