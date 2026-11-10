package org.project.ttokttok.infrastructure.email.service;

import org.project.ttokttok.global.exception.ErrorMessage;

import org.project.ttokttok.infrastructure.email.exception.EmailValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.infrastructure.email.dto.EmailRequest;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.project.ttokttok.global.util.LogMasker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

@Slf4j
@Service
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
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

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
            log.debug("이메일 발송 성공");

        } catch (MessagingException e) {
            log.warn("이메일 메시지 생성 실패: {} - {}", LogMasker.email(emailRequest.getTo()), e.getClass().getSimpleName());
            throw new RuntimeException("이메일 메시지 생성에 실패했습니다.", e);
        } catch (UnsupportedEncodingException e) {
            log.warn("이메일 인코딩 실패: {} - {}", LogMasker.email(emailRequest.getTo()), e.getClass().getSimpleName());
            throw new RuntimeException("이메일 인코딩에 실패했습니다.", e);
        } catch (MailException e) {
            log.warn("이메일 발송 실패: {} - {}", LogMasker.email(emailRequest.getTo()), e.getClass().getSimpleName());
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

    // 이메일 인증코드 발송. 코드는 호출부가 먼저 만들어 저장한 것을 받는다
    public void sendVerificationCode(String email, String code) {
        if (!isValidSangmyungEmail(email)) {
            throw new EmailValidationException(ErrorMessage.EMAIL_SIGNUP_DOMAIN_INVALID);
        }

        sendEmail(EmailRequest.createVerificationEmail(email, code));
        log.debug("인증코드 발송 완료");
    }

    // 비밀번호 재설정 인증코드 발송
    public void sendPasswordResetCode(String email, String code) {
        sendEmail(EmailRequest.createPasswordResetEmail(email, code));
        log.debug("비밀번호 재설정 코드 발송 완료");
    }

    // 결과 안내 이메일 한 통을 동기 발송한다. 결과 메일은 #460부터 DB 발송 작업과 워커로 보내며, 호출부는 워커이고 트랜잭션 밖에서 부른다.
    public void sendResultEmail(String email, ResultMailContent content) {
        if (!isValidEmail(email)) {
            throw new EmailValidationException(ErrorMessage.EMAIL_RESULT_RECIPIENT_INVALID);
        }
        sendEmail(EmailRequest.createResultEmail(email, content.title(), content.body()));
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
