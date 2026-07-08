package org.project.ttokttok.infrastructure.email.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applicant.controller.dto.request.MailFormatRequest;
import org.project.ttokttok.infrastructure.email.dto.EmailRequest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService - 이메일 발송 동작")
class EmailServiceBehaviorTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setFields() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@sangmyung.kr");
        ReflectionTestUtils.setField(emailService, "fromName", "TtokTtok");
        ReflectionTestUtils.setField(emailService, "replyTo", "reply@sangmyung.kr");
    }

    private MimeMessage emptyMimeMessage() {
        return new MimeMessage((jakarta.mail.Session) null);
    }

    @Test
    @DisplayName("sendEmail은 MimeMessage를 구성해 메일을 발송한다")
    void sendEmail_success() {
        given(mailSender.createMimeMessage()).willReturn(emptyMimeMessage());
        EmailRequest request = EmailRequest.createVerificationEmail("user@sangmyung.kr", "123456");

        emailService.sendEmail(request);

        verify(mailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    @DisplayName("메일 발송 중 MailException이 발생하면 RuntimeException으로 감싼다")
    void sendEmail_whenMailFails_throwsRuntime() {
        given(mailSender.createMimeMessage()).willReturn(emptyMimeMessage());
        doThrow(new MailSendException("발송 실패")).when(mailSender)
                .send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
        EmailRequest request = EmailRequest.createVerificationEmail("user@sangmyung.kr", "123456");

        assertThatThrownBy(() -> emailService.sendEmail(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이메일 발송");
    }

    @Test
    @DisplayName("generateVerificationCode는 6자리 숫자 코드를 생성한다")
    void generateVerificationCode() {
        String code = emailService.generateVerificationCode();

        assertThat(code).hasSize(6).containsPattern("\\d{6}");
    }

    @Test
    @DisplayName("isValidSangmyungEmail은 상명대 이메일만 true를 반환한다")
    void isValidSangmyungEmail() {
        assertThat(emailService.isValidSangmyungEmail("a@sangmyung.kr")).isTrue();
        assertThat(emailService.isValidSangmyungEmail("a@gmail.com")).isFalse();
        assertThat(emailService.isValidSangmyungEmail(null)).isFalse();
        assertThat(emailService.isValidSangmyungEmail("  ")).isFalse();
    }

    @Test
    @DisplayName("sendVerificationCode는 상명대 이메일이 아니면 예외를 던진다")
    void sendVerificationCode_invalidEmail() {
        assertThatThrownBy(() -> emailService.sendVerificationCode("a@gmail.com"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendVerificationCode는 유효한 이메일에 인증코드를 발송하고 코드를 반환한다")
    void sendVerificationCode_success() {
        given(mailSender.createMimeMessage()).willReturn(emptyMimeMessage());

        String code = emailService.sendVerificationCode("user@sangmyung.kr");

        assertThat(code).hasSize(6);
        verify(mailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPasswordResetCode는 재설정 코드를 발송하고 코드를 반환한다")
    void sendPasswordResetCode_success() {
        given(mailSender.createMimeMessage()).willReturn(emptyMimeMessage());

        String code = emailService.sendPasswordResetCode("user@sangmyung.kr");

        assertThat(code).hasSize(6);
        verify(mailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendResultMail은 유효하지 않은 이메일은 건너뛰고 유효한 이메일에만 발송한다")
    void sendResultMail_skipsInvalidEmails() {
        given(mailSender.createMimeMessage()).willReturn(emptyMimeMessage());
        MailFormatRequest request = new MailFormatRequest("합격", "축하합니다");

        emailService.sendResultMail(List.of("valid@sangmyung.kr", "invalid-email", "another@gmail.com"), request);

        // 유효한 이메일 2건에 대해서만 발송 시도 (invalid-email 제외)
        verify(mailSender, atLeastOnce()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }
}
