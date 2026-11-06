package org.project.ttokttok.domain.user.service;

import org.project.ttokttok.global.exception.ErrorMessage;

import org.project.ttokttok.domain.user.exception.UserValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.exception.OAuthOnlyAccountException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.domain.user.service.dto.request.ResetPasswordServiceRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void resetPassword(ResetPasswordServiceRequest request) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new UserValidationException(ErrorMessage.USER_NEW_PASSWORD_MISMATCH);
        }

        emailVerificationService.requireVerifiedCode(request.email(), request.verificationCode());

        User user = findPasswordAccount(request.email());

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("비밀번호 재설정 완료 : {}", user.getEmail());
    }

    // 트랜잭션 없이 실행한다. 트랜잭션(NOT_SUPPORTED 포함) 범위 안에서 사용자를 조회하면
    // 그 커넥션을 메서드가 끝날 때까지 쥐고 있어 메일 발송 동안 반납되지 않는다.
    public void sendPasswordResetCode(String email) {
        findPasswordAccount(email);
        emailVerificationService.sendPasswordResetCode(email);
    }

    private User findPasswordAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserValidationException(ErrorMessage.USER_AUTH_NOT_FOUND));
        if (user.isOAuthOnly()) {
            throw new OAuthOnlyAccountException();
        }
        return user;
    }
}
