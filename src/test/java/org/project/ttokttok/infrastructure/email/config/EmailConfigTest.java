package org.project.ttokttok.infrastructure.email.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.mail.MailException;
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
        @DisplayName("SMTP 연결·읽기·쓰기 타임아웃을 설정한다 (#435)")
        void setsSmtpTimeouts() {
            // given
            ReflectionTestUtils.setField(emailConfig, "username", "");
            ReflectionTestUtils.setField(emailConfig, "password", "");

            // when
            Properties props = ((JavaMailSenderImpl) emailConfig.javaMailSender()).getJavaMailProperties();

            // then
            assertThat(props.getProperty("mail.smtp.connectiontimeout")).isEqualTo("5000");
            assertThat(props.getProperty("mail.smtp.timeout")).isEqualTo("10000");
            assertThat(props.getProperty("mail.smtp.writetimeout")).isEqualTo("10000");
        }

        @Test
        @DisplayName("연결만 받고 응답하지 않는 SMTP 서버에 보내면 읽기 타임아웃 안에 실패하고 스레드가 풀린다 (#435)")
        @Timeout(30)
        void failsWithinReadTimeoutWhenServerNeverResponds() throws Exception {
            // given: 연결을 받아 두기만 하고 인사말(220)을 보내지 않는 서버
            try (ServerSocket hung = new ServerSocket(0)) {
                List<Socket> held = new CopyOnWriteArrayList<>();
                Thread acceptor = new Thread(() -> {
                    try {
                        held.add(hung.accept());
                    } catch (IOException ignored) {
                        // 서버를 닫으면 끝난다
                    }
                });
                acceptor.setDaemon(true);
                acceptor.start();
                ReflectionTestUtils.setField(emailConfig, "host", "127.0.0.1");
                ReflectionTestUtils.setField(emailConfig, "port", hung.getLocalPort());
                ReflectionTestUtils.setField(emailConfig, "username", "");
                ReflectionTestUtils.setField(emailConfig, "password", "");
                JavaMailSenderImpl sender = (JavaMailSenderImpl) emailConfig.javaMailSender();
                MimeMessage message = sender.createMimeMessage();
                message.setFrom("noreply@example.com");
                message.setRecipients(Message.RecipientType.TO, "a@example.com");
                message.setText("hang");

                // when
                long start = System.nanoTime();
                assertThatThrownBy(() -> sender.send(message)).isInstanceOf(MailException.class);
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

                // then: 끝없이 기다리지 않고 읽기 타임아웃(10초) 근처에서 돌아온다
                assertThat(elapsedMs).isBetween(EmailConfig.READ_TIMEOUT_MS - 500L, EmailConfig.READ_TIMEOUT_MS + 5_000L);
                for (Socket s : held) {
                    s.close();
                }
            }
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
