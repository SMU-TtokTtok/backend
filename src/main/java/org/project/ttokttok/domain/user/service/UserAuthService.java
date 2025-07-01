package org.project.ttokttok.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.project.ttokttok.domain.user.service.dto.request.SignupServiceRequest;

import org.project.ttokttok.domain.user.service.dto.request.LoginServiceRequest;
import org.project.ttokttok.domain.user.service.dto.request.ResetPasswordServiceRequest;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserServiceResponse;

import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.global.auth.jwt.dto.request.TokenRequest;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRedisService refreshTokenRedisService;

    /**
     * 1. 이메일 인증코드 전송
     * */
    public void sendVerificationCode(String email) {
        // 1-1. 상명대 이메일 형식 검증
        if (!emailService.isValidSangmyungEmail(email)) {
            throw new IllegalArgumentException("상명대학교 이메일만 사용 가능합니다.");
        }

        // 1-2. 기존 미인증 코드를 만료 처리
        emailVerificationRepository.expireAllPendingVerifications(email);

        // 1-3. 새 인증코드 생성 및 발송
        String code = emailService.sendVerificationCode(email);

        // 1-4. DB 에 인증 정보 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        emailVerificationRepository.save(verification);
        log.info("인증코드 발송 및 저장 완료 : {}", email);
    }

    /**
     * 2. 이메일 인증코드 검증
     * */
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

    /**
     * 3. 회원가입
     * */
    public UserServiceResponse signup(SignupServiceRequest request) {
        // 3-1. 비밀번호 확인 일치 검증
        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3-2. 이메일 중복 검증
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 3-3. 이메일 인증 확인
        if (!emailVerificationRepository.existsByEmailAndIsVerifiedTrue(request.email())) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        // 3-4. 사용자 정보 저장
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setEmailVerified(true);
        user.setTermsAgreed(request.termsAgreed());

        User savedUser = userRepository.save(user);
        log.info("회원가입 완료: {}", savedUser.getEmail());

        return UserServiceResponse.from(savedUser);
    }

    /**
     * 4. 로그인
     * */
    @Transactional(readOnly = true)
    public LoginServiceResponse login(LoginServiceRequest request) {
        // 4-1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 4-2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }

        // 4-3. 이메일 인증 확인
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않은 계정입니다.");
        }

        // 4-4. JWT 토큰 발급
        TokenRequest tokenRequest = TokenRequest.of(user.getId(), Role.ROLE_USER);
        TokenResponse tokens = tokenProvider.generateToken(tokenRequest);

        // 4-5. 리프레시 토큰 Redis 저장 (로그인 유지 옵션 고려)
        if (request.rememberMe()) {
            refreshTokenRedisService.save(user.getId(), tokens.refreshToken());
        }

        log.info("로그인 성공: {}", user.getEmail());

        return LoginServiceResponse.from(tokens, UserServiceResponse.from(user));
    }

    /**
     * 5. 비밀번호 재설정
     * */
    public void resetPassword(ResetPasswordServiceRequest request) {
        // 5-1. 새 비밀번호 확인 일치 검증
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        // 5-2. 인증코드 검증
        verifyEmail(request.email(), request.verificationCode());

        // 5-3. 사용자 조회 및 비밀번호 업데이트
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("비밀번호 재설정 완료 : {}", user.getEmail());
    }

    /**
     * 6. 비밀번호 재설정용 인증코드 발송
     * */
    public void sendPasswordResetCode(String email) {
        // 사용자 존재 확인
        if (!userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 기존 미인증 코드들 만료 처리
        emailVerificationRepository.expireAllPendingVerifications(email);

        // 새 인증코드 생성 및 발송
        String code = emailService.sendPasswordResetCode(email);

        // DB에 인증 정보 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        emailVerificationRepository.save(verification);
        log.info("비밀번호 재설정 코드 발송 완료: {}", email);
    }
}
