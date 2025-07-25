package org.project.ttokttok.infrastructure.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.infrastructure.email.dto.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${email.from.address}")
    private String fromAddress;
    
    @Value("${email.from.name}")
    private String fromName;
    
    @Value("${email.reply-to}")
    private String replyTo;
    private static final String CHARACTERS = "0123456789";
    private static final SecureRandom random = new SecureRandom();
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$";

    // 이메일 발송
    public void sendEmail(EmailRequest emailRequest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName); // 발신자 주소와 이름 설정
            helper.setReplyTo(replyTo);
            helper.setTo(emailRequest.getTo());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getContent(), emailRequest.isHtml());

            mailSender.send(message);
            log.info("이메일 발송 성공: {}", emailRequest.getTo());

        } catch (MessagingException e) {
            log.error("이메일 메시지 생성 실패: {} - {}", emailRequest.getTo(), e.getMessage());
            throw new RuntimeException("이메일 메시지 생성에 실패했습니다.", e);
        } catch (UnsupportedEncodingException e) {
            log.error("이메일 인코딩 실패: {} - {}", emailRequest.getTo(), e.getMessage());
            throw new RuntimeException("이메일 인코딩에 실패했습니다.", e);
        } catch (MailException e) {
            log.error("이메일 발송 실패: {} - {}", emailRequest.getTo(), e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    // 6자리 랜덤 인증코드 생성
    public String generateVerificationCode() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    // 상명대 이메일 형식 검증 (테스트용 Gmail 추가)
    public boolean isValidSangmyungEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.toLowerCase().endsWith("@sangmyung.kr");
    }

    // 이메일 인증코드 발성
    public String sendVerificationCode(String email) {
        if (!isValidSangmyungEmail(email)) {
            throw new IllegalArgumentException("상명대학교 이메일만 가입 가능합니다.");
        }

        String code = generateVerificationCode();
        EmailRequest emailRequest = EmailRequest.createVerificationEmail(email, code);
        sendEmail(emailRequest);

        log.info("인증코드 발송 완료: {}", email);
        return code;
    }

    // 비밀번호 재설정 인증코드 발송
    public String sendPasswordResetCode(String email) {
        String code = generateVerificationCode();
        EmailRequest emailRequest = EmailRequest.createPasswordResetEmail(email, code);
        sendEmail(emailRequest);

        log.info("비밀번호 재설정 코드 발송 완료: {}", email);
        return code;
    }

    // TODO: 성능이 너무 안 나올 경우, 배치 처리로 변경 필요
    // 동아리 지원 결과 안내 이메일 발송
    public void sendResultMail(List<String> emails, String resultBody) {
        for (String email : emails) {
            if (!isValidEmail(email)) {
                log.warn("유효하지 않은 이메일 주소: {}", email);
                continue;
            }

            EmailRequest emailRequest = EmailRequest.createResultEmail(email, resultBody);
            sendEmailAsync(emailRequest);
            log.info("지원 결과 안내 이메일 발송 완료: {}", email);
        }
    }

    // 비동기 이메일 발송 - TODO: 추후 리팩토링
    @Async
    protected void sendEmailAsync(EmailRequest emailRequest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName); // 발신자 주소와 이름 설정
            helper.setReplyTo(replyTo);
            helper.setTo(emailRequest.getTo());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getContent(), emailRequest.isHtml());

            mailSender.send(message);
            log.info("이메일 발송 성공: {}", emailRequest.getTo());

        } catch (MessagingException e) {
            log.error("이메일 메시지 생성 실패: {} - {}", emailRequest.getTo(), e.getMessage());
        } catch (UnsupportedEncodingException e) {
            log.error("이메일 인코딩 실패: {} - {}", emailRequest.getTo(), e.getMessage());
        } catch (MailException e) {
            log.error("이메일 발송 실패: {} - {}", emailRequest.getTo(), e.getMessage());
        }
    }

    // 순수 이메일 주소 검증 메서드
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // 간단한 이메일 형식 검증
        return email.matches(EMAIL_REGEX);
    }
}
