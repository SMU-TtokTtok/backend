package org.project.ttokttok.domain.user.service;

import org.project.ttokttok.global.exception.ErrorMessage;

import org.project.ttokttok.domain.user.exception.UserValidationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.redis.service.VerificationAttemptRedisService;
import org.project.ttokttok.global.util.LogMasker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final long CODE_VALIDITY_MINUTES = 5;
    private static final int MAX_ATTEMPTS_PER_IP = 5;
    private static final int MAX_ATTEMPTS_PER_EMAIL = 20;

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;
    private final VerificationAttemptRedisService verificationAttemptRedisService;

    public void sendVerificationCode(String email) {
        if (!emailService.isValidSangmyungEmail(email)) {
            throw new UserValidationException(ErrorMessage.VERIFICATION_EMAIL_INVALID);
        }
        sendCode(email, emailService::sendVerificationCode);
        log.info("인증코드 발송 및 저장 완료 : {}", LogMasker.email(email));
    }

    // 전체 시도 초과로 코드를 만료시킨 뒤 예외를 던지므로 그 만료는 롤백하지 않는다.
    // 실패 경로에는 이 만료 말고 다른 쓰기가 없다.
    @Transactional(noRollbackFor = UserValidationException.class)
    public boolean verifyEmail(String email, String code, String clientIp) {
        checkAttempts(email, clientIp);

        EmailVerification verification = emailVerificationRepository
                .findByEmailAndCodeAndIsVerifiedFalse(email, code)
                .orElseThrow(() -> new UserValidationException(ErrorMessage.VERIFICATION_CODE_INVALID));

        if (verification.isExpired()) {
            throw new UserValidationException(ErrorMessage.VERIFICATION_CODE_EXPIRED);
        }

        verification.markAsVerified();
        verificationAttemptRedisService.reset(email);
        log.info("이메일 인증 완료 : {}", LogMasker.email(email));
        return true;
    }

    // 횟수는 코드를 대조하기 전에 올려 동시 요청도 한도까지만 대조되게 한다.
    // IP별 한도를 넘으면 그 IP만 막고, 다른 사람이 틀린 횟수로 내 코드가 무효가 되지는 않는다.
    // 여러 IP로 나눠 대입하는 경우는 이메일 전체 한도에서 코드를 만료시켜 막는다.
    private void checkAttempts(String email, String clientIp) {
        Duration ttl = Duration.ofMinutes(CODE_VALIDITY_MINUTES);
        if (verificationAttemptRedisService.incrementForIp(email, clientIp, ttl) > MAX_ATTEMPTS_PER_IP) {
            throw new UserValidationException(ErrorMessage.VERIFICATION_ATTEMPTS_EXCEEDED);
        }
        if (verificationAttemptRedisService.incrementTotal(email, ttl) > MAX_ATTEMPTS_PER_EMAIL) {
            emailVerificationRepository.expireAllPendingVerifications(email, LocalDateTime.now());
            throw new UserValidationException(ErrorMessage.VERIFICATION_ATTEMPTS_EXCEEDED);
        }
    }

    // 회원가입용. 인증된 코드를 소비하므로 호출한 트랜잭션이 롤백되면 코드도 되살아난다.
    @Transactional
    public void consumeVerifiedEmail(String email, String code) {
        if (!consume(email, code)) {
            throw new UserValidationException(ErrorMessage.VERIFICATION_EMAIL_NOT_VERIFIED);
        }
    }

    // 비밀번호 재설정용. 소비 방식은 consumeVerifiedEmail 과 같다.
    @Transactional
    public void consumeVerifiedCode(String email, String code) {
        if (!consume(email, code)) {
            throw new UserValidationException(ErrorMessage.VERIFICATION_RECORD_NOT_FOUND);
        }
    }

    private boolean consume(String email, String code) {
        return emailVerificationRepository.consumeVerifiedCode(email, code, LocalDateTime.now()) == 1;
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
            emailVerificationRepository.expireAllPendingVerifications(email, LocalDateTime.now());
            emailVerificationRepository.save(EmailVerification.builder()
                    .email(email)
                    .code(code)
                    .expiresAt(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES))
                    .build());
        });
        verificationAttemptRedisService.reset(email);
        mailer.accept(email, code);
    }
}
