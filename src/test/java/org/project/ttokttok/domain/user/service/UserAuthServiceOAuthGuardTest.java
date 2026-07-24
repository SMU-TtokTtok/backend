package org.project.ttokttok.domain.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.exception.OAuthOnlyAccountException;
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.domain.user.service.dto.request.LoginServiceRequest;
import org.project.ttokttok.domain.user.service.dto.request.ResetPasswordServiceRequest;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * OAuth 전용 계정(비밀번호 없음)이 기존 비밀번호 기반 경로에 접근할 때의 가드 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserAuthServiceOAuthGuardTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRedisService refreshTokenRedisService;

    @InjectMocks
    private UserAuthService userAuthService;

    private static final String GMAIL = "user@gmail.com";

    private User oauthOnlyUser() {
        return User.signUpWithGoogle(GMAIL, "홍길동", "google-sub-123");
    }

    @Test
    @DisplayName("OAuth 전용 계정으로 비밀번호 로그인을 시도하면 OAuthOnlyAccountException을 던진다 (NPE 아님)")
    void login_withOAuthOnlyAccount_throwsOAuthOnlyException() {
        // given
        given(userRepository.findByEmail(GMAIL)).willReturn(Optional.of(oauthOnlyUser()));
        LoginServiceRequest request = new LoginServiceRequest(GMAIL, "AnyPassword123!", false);

        // when & then
        assertThatThrownBy(() -> userAuthService.login(request))
                .isInstanceOf(OAuthOnlyAccountException.class);
        verify(passwordEncoder, never()).matches(any(), any()); // BCrypt 도달 전 차단
    }

    @Test
    @DisplayName("OAuth 전용 계정으로 비밀번호 재설정 코드 발송을 시도하면 OAuthOnlyAccountException을 던진다")
    void sendPasswordResetCode_withOAuthOnlyAccount_throwsOAuthOnlyException() {
        // given
        given(userRepository.findByEmail(GMAIL)).willReturn(Optional.of(oauthOnlyUser()));

        // when & then
        assertThatThrownBy(() -> userAuthService.sendPasswordResetCode(GMAIL))
                .isInstanceOf(OAuthOnlyAccountException.class);
        verify(emailService, never()).sendPasswordResetCode(any());
    }

    @Test
    @DisplayName("OAuth 전용 계정으로 비밀번호 재설정을 시도하면 OAuthOnlyAccountException을 던진다")
    void resetPassword_withOAuthOnlyAccount_throwsOAuthOnlyException() {
        // given
        given(emailVerificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(GMAIL, "123456"))
                .willReturn(true);
        given(userRepository.findByEmail(GMAIL)).willReturn(Optional.of(oauthOnlyUser()));
        ResetPasswordServiceRequest request = new ResetPasswordServiceRequest(
                GMAIL, "123456", "NewPassword123!", "NewPassword123!");

        // when & then
        assertThatThrownBy(() -> userAuthService.resetPassword(request))
                .isInstanceOf(OAuthOnlyAccountException.class);
        verify(userRepository, never()).save(any());
    }
}
