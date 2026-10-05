package org.project.ttokttok.domain.user.service;

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
@Transactional
public class PasswordResetService {
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    public void resetPassword(ResetPasswordServiceRequest request) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        emailVerificationService.requireVerifiedCode(request.email(), request.verificationCode());

        User user = findPasswordAccount(request.email());

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("비밀번호 재설정 완료 : {}", user.getEmail());
    }

    public void sendPasswordResetCode(String email) {
        findPasswordAccount(email);
        emailVerificationService.sendPasswordResetCode(email);
    }

    private User findPasswordAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        if (user.isOAuthOnly()) {
            throw new OAuthOnlyAccountException();
        }
        return user;
    }
}
