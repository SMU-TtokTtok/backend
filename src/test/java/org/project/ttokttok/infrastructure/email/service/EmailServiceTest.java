package org.project.ttokttok.infrastructure.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    // ===== generateVerificationCode 테스트 =====

    @Nested
    @DisplayName("generateVerificationCode 메서드")
    class GenerateVerificationCodeTest {

        @Test
        @DisplayName("6자리 인증코드를 생성한다")
        void generatesSixDigitCode() {
            // when
            String code = emailService.generateVerificationCode();

            // then
            assertThat(code).hasSize(6);
        }

        @Test
        @DisplayName("인증코드는 숫자로만 구성된다")
        void codeContainsOnlyDigits() {
            // when
            String code = emailService.generateVerificationCode();

            // then
            assertThat(code).matches("\\d{6}");
        }

        @Test
        @DisplayName("매번 다른 인증코드를 생성한다")
        void generatesDifferentCodes() {
            // when
            String code1 = emailService.generateVerificationCode();
            String code2 = emailService.generateVerificationCode();
            String code3 = emailService.generateVerificationCode();

            // then (최소 2개는 다를 것으로 기대 - 확률적으로 매우 높음)
            boolean allSame = code1.equals(code2) && code2.equals(code3);
            assertThat(allSame).isFalse();
        }
    }

    // ===== isValidSangmyungEmail 테스트 =====

    @Nested
    @DisplayName("isValidSangmyungEmail 메서드")
    class IsValidSangmyungEmailTest {

        @Test
        @DisplayName("상명대학교 이메일(@sangmyung.kr)은 true를 반환한다")
        void returnsTrueForSangmyungEmail() {
            // given
            String email = "student@sangmyung.kr";

            // when
            boolean result = emailService.isValidSangmyungEmail(email);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("대소문자 구분 없이 상명대 이메일을 인식한다")
        void returnsTrueForUppercaseSangmyungEmail() {
            // given
            String email = "student@SANGMYUNG.KR";

            // when
            boolean result = emailService.isValidSangmyungEmail(email);

            // then
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "user@gmail.com",
                "user@naver.com",
                "user@sangmyung.ac.kr",
                "user@sangmyung",
                "sangmyung.kr"
        })
        @DisplayName("상명대학교 이메일이 아니면 false를 반환한다")
        void returnsFalseForNonSangmyungEmail(String email) {
            // when
            boolean result = emailService.isValidSangmyungEmail(email);

            // then
            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("null이거나 빈 문자열이면 false를 반환한다")
        void returnsFalseForNullOrEmpty(String email) {
            // when
            boolean result = emailService.isValidSangmyungEmail(email);

            // then
            assertThat(result).isFalse();
        }
    }

    // ===== sendVerificationCode 테스트 =====

    @Nested
    @DisplayName("sendVerificationCode 메서드")
    class SendVerificationCodeTest {

        @BeforeEach
        void setUp() {
            // 테스트용 @Value 필드 설정
            ReflectionTestUtils.setField(emailService, "fromAddress", "test@example.com");
            ReflectionTestUtils.setField(emailService, "fromName", "TestSender");
            ReflectionTestUtils.setField(emailService, "replyTo", "reply@example.com");
        }

        @Test
        @DisplayName("상명대 이메일이 아니면 IllegalArgumentException이 발생한다")
        void throwsExceptionForNonSangmyungEmail() {
            // given
            String invalidEmail = "user@gmail.com";

            // when & then
            assertThatThrownBy(() -> emailService.sendVerificationCode(invalidEmail))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상명대학교 이메일만 가입 가능합니다.");
        }

        @Test
        @DisplayName("상명대 이메일로 인증코드를 발송하면 6자리 코드를 반환한다")
        void returnsSixDigitCodeForValidEmail() {
            // given
            String validEmail = "student@sangmyung.kr";
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // when
            String code = emailService.sendVerificationCode(validEmail);

            // then
            assertThat(code).hasSize(6);
            assertThat(code).matches("\\d{6}");
            verify(mailSender).send(any(MimeMessage.class));
        }
    }

    // ===== sendPasswordResetCode 테스트 =====

    @Nested
    @DisplayName("sendPasswordResetCode 메서드")
    class SendPasswordResetCodeTest {

        @BeforeEach
        void setUp() {
            // 테스트용 @Value 필드 설정
            ReflectionTestUtils.setField(emailService, "fromAddress", "test@example.com");
            ReflectionTestUtils.setField(emailService, "fromName", "TestSender");
            ReflectionTestUtils.setField(emailService, "replyTo", "reply@example.com");
        }

        @Test
        @DisplayName("비밀번호 재설정 코드를 발송하면 6자리 코드를 반환한다")
        void returnsSixDigitCode() {
            // given
            String email = "user@sangmyung.kr";
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // when
            String code = emailService.sendPasswordResetCode(email);

            // then
            assertThat(code).hasSize(6);
            assertThat(code).matches("\\d{6}");
            verify(mailSender).send(any(MimeMessage.class));
        }
    }
}

