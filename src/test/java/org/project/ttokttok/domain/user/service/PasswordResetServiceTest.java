package org.project.ttokttok.domain.user.service;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.exception.OAuthOnlyAccountException;
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.domain.user.service.dto.request.ResetPasswordServiceRequest;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    private static final String EMAIL = "test@sangmyung.kr";
    private static final String CODE = "123456";
    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationRepository verificationRepository;
    @Mock private EmailService emailService;
    @Mock private TokenProvider tokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRedisService refreshTokenRedisService;
    private UserAuthService service;

    @BeforeEach
    void setUp() {
        service = new UserAuthService(userRepository, verificationRepository, emailService,
                tokenProvider, passwordEncoder, refreshTokenRedisService);
    }

    @Test
    void resetUpdatesEncodedPasswordAfterVerificationAndLookup() {
        User user = localUser();
        given(verificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(EMAIL, CODE)).willReturn(true);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("NewPassword123!")).willReturn("encoded-new");

        service.resetPassword(request("NewPassword123!"));

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        InOrder order = inOrder(verificationRepository, userRepository, passwordEncoder);
        order.verify(verificationRepository).existsByEmailAndCodeAndIsVerifiedTrue(EMAIL, CODE);
        order.verify(userRepository).findByEmail(EMAIL);
        order.verify(passwordEncoder).encode("NewPassword123!");
        order.verify(userRepository).save(user);
    }

    @Test
    void passwordMismatchStopsBeforeVerification() {
        assertThatThrownBy(() -> service.resetPassword(request("different")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("새 비밀번호가 일치하지 않습니다.");
        verifyNoInteractions(verificationRepository, userRepository, passwordEncoder, emailService);
    }

    @Test
    void missingVerificationStopsBeforeUserLookup() {
        assertThatThrownBy(() -> service.resetPassword(request("NewPassword123!")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("인증 코드 성공 여부가 존재하지 않습니다.");
        verifyNoInteractions(userRepository, passwordEncoder, emailService);
    }

    @Test
    void missingUserDoesNotEncodeOrSave() {
        given(verificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(EMAIL, CODE)).willReturn(true);
        assertThatThrownBy(() -> service.resetPassword(request("NewPassword123!")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("존재하지 않는 사용자입니다.");
        verifyNoInteractions(passwordEncoder, emailService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void oauthAccountCannotResetPassword() {
        User user = User.signUpWithGoogle(EMAIL, "사용자", "google-sub");
        given(verificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(EMAIL, CODE)).willReturn(true);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resetPassword(request("NewPassword123!")))
                .isInstanceOf(OAuthOnlyAccountException.class);
        assertThat(user.getPassword()).isNull();
        verifyNoInteractions(passwordEncoder, emailService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetCodeChecksUserThenExpiresSendsAndSaves() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(localUser()));
        given(emailService.sendPasswordResetCode(EMAIL)).willReturn(CODE);
        LocalDateTime before = LocalDateTime.now();

        service.sendPasswordResetCode(EMAIL);

        ArgumentCaptor<EmailVerification> saved = ArgumentCaptor.forClass(EmailVerification.class);
        InOrder order = inOrder(userRepository, verificationRepository, emailService);
        order.verify(userRepository).findByEmail(EMAIL);
        order.verify(verificationRepository).expireAllPendingVerifications(EMAIL);
        order.verify(emailService).sendPasswordResetCode(EMAIL);
        order.verify(verificationRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getValue().getCode()).isEqualTo(CODE);
        assertThat(saved.getValue().isVerified()).isFalse();
        assertThat(saved.getValue().getExpiresAt()).isBetween(before.plusMinutes(5), LocalDateTime.now().plusMinutes(5));
    }

    @Test
    void missingUserCannotRequestResetCode() {
        assertThatThrownBy(() -> service.sendPasswordResetCode(EMAIL))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("존재하지 않는 사용자입니다.");
        verifyNoInteractions(verificationRepository, emailService);
    }

    @Test
    void oauthAccountCannotRequestResetCode() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(User.signUpWithGoogle(EMAIL, "사용자", "google-sub")));
        assertThatThrownBy(() -> service.sendPasswordResetCode(EMAIL)).isInstanceOf(OAuthOnlyAccountException.class);
        verifyNoInteractions(verificationRepository, emailService);
    }

    @Test
    void failedResetCodeSendDoesNotSaveNewCode() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(localUser()));
        RuntimeException failure = new RuntimeException("SMTP failed");
        given(emailService.sendPasswordResetCode(EMAIL)).willThrow(failure);

        assertThatThrownBy(() -> service.sendPasswordResetCode(EMAIL)).isSameAs(failure);
        verify(verificationRepository, never()).save(any());
    }

    private User localUser() {
        return User.signUp(EMAIL, "old-password", "사용자", true);
    }

    private ResetPasswordServiceRequest request(String confirmation) {
        return new ResetPasswordServiceRequest(EMAIL, CODE, "NewPassword123!", confirmation);
    }
}
