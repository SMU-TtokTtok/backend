package org.project.ttokttok.domain.user.service;

import org.project.ttokttok.global.exception.ErrorMessage;

import org.project.ttokttok.domain.user.exception.UserValidationException;

import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.global.util.LogMasker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final long CODE_VALIDITY_MINUTES = 5;

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;

    public void sendVerificationCode(String email) {
        if (!emailService.isValidSangmyungEmail(email)) {
            throw new UserValidationException(ErrorMessage.VERIFICATION_EMAIL_INVALID);
        }
        sendCode(email, emailService::sendVerificationCode);
        log.info("인증코드 발송 및 저장 완료 : {}", LogMasker.email(email));
    }

    @Transactional
    public boolean verifyEmail(String email, String code) {
        EmailVerification verification = emailVerificationRepository
                .findByEmailAndCodeAndIsVerifiedFalse(email, code)
                .orElseThrow(() -> new UserValidationException(ErrorMessage.VERIFICATION_CODE_INVALID));

        if (verification.isExpired()) {
            throw new UserValidationException(ErrorMessage.VERIFICATION_CODE_EXPIRED);
        }

        verification.markAsVerified();
        log.info("이메일 인증 완료 : {}", LogMasker.email(email));
        return true;
    }

    @Transactional(readOnly = true)
    public void requireVerifiedEmail(String email) {
        if (!emailVerificationRepository.existsByEmailAndIsVerifiedTrue(email)) {
            throw new UserValidationException(ErrorMessage.VERIFICATION_EMAIL_NOT_VERIFIED);
        }
    }

    @Transactional(readOnly = true)
    public void requireVerifiedCode(String email, String code) {
        if (!emailVerificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(email, code)) {
            throw new UserValidationException(ErrorMessage.VERIFICATION_RECORD_NOT_FOUND);
        }
    }

    public void sendPasswordResetCode(String email) {
        sendCode(email, emailService::sendPasswordResetCode);
        log.info("비밀번호 재설정 코드 발송 완료: {}", LogMasker.email(email));
    }

    // 새 코드를 짧은 트랜잭션으로 먼저 커밋하고, 메일(SMTP)은 커넥션을 반납한 뒤 보낸다.
    // 트랜잭션 안에서 보내면 발송 동안 커넥션을 쥐어 느린 SMTP가 풀을 고갈시킨다.
    // 저장을 발송 뒤로 미루면 메일이 늦게 끝난 이전 요청이 나중 요청의 코드를 만료시킨다.
    private void sendCode(String email, BiConsumer<String, String> mailer) {
        String code = emailService.generateVerificationCode();
        transactionTemplate.executeWithoutResult(status -> {
            emailVerificationRepository.expireAllPendingVerifications(email);
            emailVerificationRepository.save(EmailVerification.builder()
                    .email(email)
                    .code(code)
                    .expiresAt(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES))
                    .build());
        });
        mailer.accept(email, code);
    }
}
