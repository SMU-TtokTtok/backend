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
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {
    private static final String EMAIL = "test@sangmyung.kr";
    private static final String CODE = "123456";
    @Mock private EmailVerificationRepository verificationRepository;
    @Mock private EmailService emailService;
    @Mock private UserRepository userRepository;
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
    void sendCodeExpiresPendingThenSendsAndSavesFiveMinuteCode() {
        given(emailService.isValidSangmyungEmail(EMAIL)).willReturn(true);
        given(emailService.sendVerificationCode(EMAIL)).willReturn(CODE);
        LocalDateTime before = LocalDateTime.now();

        service.sendVerificationCode(EMAIL);

        ArgumentCaptor<EmailVerification> saved = ArgumentCaptor.forClass(EmailVerification.class);
        InOrder order = inOrder(emailService, verificationRepository);
        order.verify(emailService).isValidSangmyungEmail(EMAIL);
        order.verify(verificationRepository).expireAllPendingVerifications(EMAIL);
        order.verify(emailService).sendVerificationCode(EMAIL);
        order.verify(verificationRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getValue().getCode()).isEqualTo(CODE);
        assertThat(saved.getValue().isVerified()).isFalse();
        assertThat(saved.getValue().getExpiresAt()).isBetween(before.plusMinutes(5), LocalDateTime.now().plusMinutes(5));
    }

    @Test
    void invalidEmailIsRejectedBeforeExpiryOrSend() {
        assertThatThrownBy(() -> service.sendVerificationCode(EMAIL))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("상명대학교 이메일만 사용 가능합니다.");
        verifyNoInteractions(verificationRepository);
        verify(emailService, never()).sendVerificationCode(any());
    }

    @Test
    void sendFailureDoesNotSaveNewCode() {
        given(emailService.isValidSangmyungEmail(EMAIL)).willReturn(true);
        RuntimeException failure = new RuntimeException("SMTP failed");
        given(emailService.sendVerificationCode(EMAIL)).willThrow(failure);

        assertThatThrownBy(() -> service.sendVerificationCode(EMAIL)).isSameAs(failure);

        verify(verificationRepository).expireAllPendingVerifications(EMAIL);
        verify(verificationRepository, never()).save(any());
    }

    @Test
    void validCodeMarksVerificationWithoutExplicitSave() {
        EmailVerification verification = verification(LocalDateTime.now().plusMinutes(5));
        given(verificationRepository.findByEmailAndCodeAndIsVerifiedFalse(EMAIL, CODE))
                .willReturn(Optional.of(verification));

        assertThat(service.verifyEmail(EMAIL, CODE)).isTrue();

        assertThat(verification.isVerified()).isTrue();
        verify(verificationRepository, never()).save(any());
    }

    @Test
    void unknownCodePreservesMessage() {
        assertThatThrownBy(() -> service.verifyEmail(EMAIL, CODE))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("올바르지 않은 인증코드입니다.");
    }

    @Test
    void expiredCodeDoesNotMarkVerification() {
        EmailVerification verification = verification(LocalDateTime.now().minusMinutes(1));
        given(verificationRepository.findByEmailAndCodeAndIsVerifiedFalse(EMAIL, CODE))
                .willReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.verifyEmail(EMAIL, CODE))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("인증코드가 만료되었습니다.");
        assertThat(verification.isVerified()).isFalse();
    }

    private EmailVerification verification(LocalDateTime expiresAt) {
        return EmailVerification.builder().email(EMAIL).code(CODE).expiresAt(expiresAt).build();
    }
}
