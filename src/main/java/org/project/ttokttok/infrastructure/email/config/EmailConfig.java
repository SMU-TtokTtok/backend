package org.project.ttokttok.infrastructure.email.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Configuration
public class EmailConfig {

    static final int CONNECT_TIMEOUT_MS = 5_000;
    static final int READ_TIMEOUT_MS = 10_000;
    static final int WRITE_TIMEOUT_MS = 10_000;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");

        // 운영에서는 내부망 Postfix 릴레이(smtp:25)로 인증 없이 넘긴다 — 릴레이 계정은
        // Postfix 컨테이너에만 있고 앱에는 주입되지 않는다(spring.mail.username/password가 빈 문자열).
        // username이 비어 있는데 auth=true였다면 JavaMail이 JVM user.name으로 폴백해
        // AuthenticationFailedException을 던진다. 자격증명이 있을 때만(직접 인증하는
        // 환경에서만) auth/starttls를 켠다.
        boolean useAuth = StringUtils.hasText(username);
        props.put("mail.smtp.auth", String.valueOf(useAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(useAuth));
        // JavaMail은 타임아웃이 없으면 응답을 끝없이 기다린다. Postfix가 연결만 받고 멈추면 발송 스레드가 영영 풀리지 않는다(#435).
        // 운영 Postfix는 한 통을 20~30ms에 받으므로 수백 배 여유를 둔다.
        props.put("mail.smtp.connectiontimeout", String.valueOf(CONNECT_TIMEOUT_MS));
        props.put("mail.smtp.timeout", String.valueOf(READ_TIMEOUT_MS));
        props.put("mail.smtp.writetimeout", String.valueOf(WRITE_TIMEOUT_MS));

        return mailSender;
    }
}
