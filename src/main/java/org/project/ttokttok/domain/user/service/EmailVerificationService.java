package org.project.ttokttok.domain.user.service;

import java.time.LocalDateTime;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {
    private static final long CODE_VALIDITY_MINUTES = 5;

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;

    public void sendVerificationCode(String email) {
        if (!emailService.isValidSangmyungEmail(email)) {
            throw new IllegalArgumentException("상명대학교 이메일만 사용 가능합니다.");
        }
        sendCode(email, () -> emailService.sendVerificationCode(email));
        log.info("인증코드 발송 및 저장 완료 : {}", email);
    }

    public boolean verifyEmail(String email, String code) {
        EmailVerification verification = emailVerificationRepository
                .findByEmailAndCodeAndIsVerifiedFalse(email, code)
                .orElseThrow(() -> new IllegalArgumentException("올바르지 않은 인증코드입니다."));

        if (verification.isExpired()) {
            throw new IllegalArgumentException("인증코드가 만료되었습니다.");
        }

        verification.markAsVerified();
        log.info("이메일 인증 완료 : {}", email);
        return true;
    }

    @Transactional(readOnly = true)
    public void requireVerifiedEmail(String email) {
        if (!emailVerificationRepository.existsByEmailAndIsVerifiedTrue(email)) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }
    }

    @Transactional(readOnly = true)
    public void requireVerifiedCode(String email, String code) {
        if (!emailVerificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(email, code)) {
            throw new IllegalArgumentException("인증 코드 성공 여부가 존재하지 않습니다.");
        }
    }

    public void sendPasswordResetCode(String email) {
        sendCode(email, () -> emailService.sendPasswordResetCode(email));
        log.info("비밀번호 재설정 코드 발송 완료: {}", email);
    }

    private void sendCode(String email, Supplier<String> codeSender) {
        emailVerificationRepository.expireAllPendingVerifications(email);
        String code = codeSender.get();
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES))
                .build();
        emailVerificationRepository.save(verification);
    }
}
