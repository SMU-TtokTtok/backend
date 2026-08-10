package org.project.ttokttok.infrastructure.email.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

class EmailConfigTest {

    private EmailConfig emailConfig;

    @BeforeEach
    void setUp() {
        emailConfig = new EmailConfig();
        ReflectionTestUtils.setField(emailConfig, "host", "smtp");
        ReflectionTestUtils.setField(emailConfig, "port", 25);
    }

    // ===== javaMailSender 메서드 =====

    @Nested
    @DisplayName("javaMailSender 메서드")
    class JavaMailSenderTest {

        @Test
        @DisplayName("username이 빈 문자열이면 인증 없이 평문 릴레이로 접속한다")
        void disablesAuthWhenUsernameIsBlank() {
            // given
            ReflectionTestUtils.setField(emailConfig, "username", "");
            ReflectionTestUtils.setField(emailConfig, "password", "");

            // when
            JavaMailSenderImpl mailSender = (JavaMailSenderImpl) emailConfig.javaMailSender();
            Properties props = mailSender.getJavaMailProperties();

            // then
            assertThat(props.getProperty("mail.smtp.auth")).isEqualTo("false");
            assertThat(props.getProperty("mail.smtp.starttls.enable")).isEqualTo("false");
        }

        @Test
        @DisplayName("username이 있으면 인증과 starttls를 켠다")
        void enablesAuthWhenUsernameIsPresent() {
            // given
            ReflectionTestUtils.setField(emailConfig, "username", "ttokttok@naver.com");
            ReflectionTestUtils.setField(emailConfig, "password", "secret");

            // when
            JavaMailSenderImpl mailSender = (JavaMailSenderImpl) emailConfig.javaMailSender();
            Properties props = mailSender.getJavaMailProperties();

            // then
            assertThat(props.getProperty("mail.smtp.auth")).isEqualTo("true");
            assertThat(props.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
        }

        @Test
        @DisplayName("운영 stdout에 SMTP 대화를 노출하지 않도록 mail.debug를 설정하지 않는다")
        void doesNotEnableMailDebug() {
            // given
            ReflectionTestUtils.setField(emailConfig, "username", "");
            ReflectionTestUtils.setField(emailConfig, "password", "");

            // when
            JavaMailSenderImpl mailSender = (JavaMailSenderImpl) emailConfig.javaMailSender();
            Properties props = mailSender.getJavaMailProperties();

            // then
            assertThat(props.getProperty("mail.debug")).isNull();
        }
    }
}
