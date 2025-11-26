package org.project.ttokttok.domain.user.service;

import static org.project.ttokttok.global.entity.Role.ROLE_USER;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.domain.user.service.dto.request.LoginServiceRequest;
import org.project.ttokttok.domain.user.service.dto.request.ResetPasswordServiceRequest;
import org.project.ttokttok.domain.user.service.dto.request.SignupServiceRequest;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserReissueServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserServiceResponse;
import org.project.ttokttok.global.auth.jwt.dto.request.TokenRequest;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.exception.InvalidTokenFromCookieException;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
     *
     * 상명대학교 이메일 형식을 검증하고, 기존 미인증 코드를 만료 처리한 후 새로운 인증코드를 생성하여 발송합니다.
     *
     * @param email 인증코드를 발송할 이메일 주소 (상명대학교 이메일만 허용)
     * @throws IllegalArgumentException 상명대학교 이메일이 아닌 경우
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

        emailVerificationRepository.save(Objects.requireNonNull(verification, "verification에 설정된 값이 null 일 수 없습니다."));
        log.info("인증코드 발송 및 저장 완료 : {}", email);
    }

    /**
     * 2. 이메일 인증코드 검증 - 이메일 인증코드를 검증합니다.
     *
     * @param email 검증할 이메일 주소
     * @param code 검증할 인증코드
     * @return 인증 성공 시 true
     * @throws IllegalArgumentException 올바르지 않은 인증코드이거나 만료된 경우
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
     * 3. 회원가입 처리 메서드
     *
     * 비밀번호 확인, 이메일 중복 검증, 이메일 인증 확인을 거쳐 새로운 사용자를 생성합니다.
     *
     * @param request 회원가입 요청 정보
     * @return 생성된 사용자 정보
     * @throws IllegalArgumentException 비밀번호 불일치, 이메일 중복, 이메일 미인증 등의 경우
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
     * 4. 로그인 - 사용자 로그인을 처리합니다.
     *
     * 이메일과 비밀번호를 검증하고, 이메일 인증 상태를 확인한 후 JWT 토큰을 발급합니다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호, 로그인 유지 정보)
     * @return 로그인 결과 (토큰 정보와 사용자 정보)
     * @throws IllegalArgumentException 존재하지 않는 사용자, 비밀번호 불일치, 이메일 미인증 등의 경우
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
        TokenRequest tokenRequest = TokenRequest.of(user.getEmail(), ROLE_USER);
        TokenResponse tokens = tokenProvider.generateToken(tokenRequest);

        // 4-5. 리프레시 토큰 Redis 저장 (항상 저장)
        refreshTokenRedisService.save(user.getEmail(), tokens.refreshToken());

        log.info("로그인 성공: {}", user.getEmail());

        return LoginServiceResponse.from(tokens, UserServiceResponse.from(user));
    }

    /**
     * 5. 비밀번호 재설정
     *
     * 새 비밀번호 확인 일치를 검증하고, 인증코드를 검증한 후 비밀번호를 업데이트 합니다.
     *
     * @param request 비밀번호 재설정 요청 정보
     * @throws IllegalArgumentException 새 비밀번호 불일치, 인증코드 오류, 존재하지 않는 사용자 등의 경우
     * */
    public void resetPassword(ResetPasswordServiceRequest request) {
        // 5-1. 새 비밀번호 확인 일치 검증
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        // 5-2. 인증코드 검증
        checkVerificationCode(request.email(), request.verificationCode());

        // 5-3. 사용자 조회 및 비밀번호 업데이트
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("비밀번호 재설정 완료 : {}", user.getEmail());
    }

    /**
     * 6. 비밀번호 재설정용 인증코드 발송합니다.
     *
     * 사용자 존재 여부를 확인하고, 기존 미인증 코드를 만료 처리한 후 새로운 인증코드를 생성하여 발송합니다.
     *
     * @param email 비밀번호 재설정 코드를 발송할 이메일 주소
     * @throws IllegalArgumentException 존재하지 않는 사용자인 경우
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

        emailVerificationRepository.save(Objects.requireNonNull(verification, "verification에 설정된 값이 null 일 수 없습니다."));
        log.info("비밀번호 재설정 코드 발송 완료: {}", email);
    }

    /**
     * 7. 로그아웃 - 사용자 로그아웃을 처리합니다.
     *
     * Redis에 저장된 리프레시 토큰을 삭제하고, 액세스 토큰을 블랙리스트에 추가하여 로그아웃을 처리합니다.
     *
     * @param refreshToken 로그아웃할 리프레시 토큰
     * @param accessToken 로그아웃할 액세스 토큰
     * @throws IllegalArgumentException 이미 로그아웃된 상태인 경우
     * */
    public void logout(String refreshToken, String accessToken) {
        // 액세스 토큰의 만료 시간 계산
        long accessTokenExpiryTime = 0;
        if (accessToken != null) {
            try {
                Date expiration = getClaims(accessToken).getExpiration();
                accessTokenExpiryTime = expiration.getTime() - System.currentTimeMillis();
                if (accessTokenExpiryTime < 0) {
                    accessTokenExpiryTime = 0; // 이미 만료된 토큰
                }
            } catch (Exception e) {
                log.warn("액세스 토큰 만료 시간 추출 실패: {}", e.getMessage());
                accessTokenExpiryTime = 0;
            }
        }

        // Redis에서 리프레시 토큰 삭제 및 액세스 토큰 블랙리스트 추가
        refreshTokenRedisService.logout(refreshToken, accessToken, accessTokenExpiryTime);
    }

    /**
     * 7. 로그아웃 - 사용자 로그아웃을 처리합니다. (기존 메서드 호환성)
     *
     * @param email 로그아웃할 사용자의 이메일
     * */
    public void logout(String email) {
        logout(email, null);
    }

    // JWT 토큰에서 Claims 추출
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(tokenProvider.getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Transactional
    public UserReissueServiceResponse reissue(String refreshToken) {
        validateTokenFromCookie(refreshToken);

        TokenResponse tokens = tokenProvider.reissueToken(refreshToken, ROLE_USER);
        Long ttl = refreshTokenRedisService.getRefreshTTL(tokens.refreshToken());

        return UserReissueServiceResponse.of(tokens, ttl);
    }

    private void validateTokenFromCookie(String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidTokenFromCookieException();
        }
    }

    private void checkVerificationCode(String email, String code) {
        if (!emailVerificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(email, code)) {
            throw new IllegalArgumentException("인증 코드 성공 여부가 존재하지 않습니다.");
        }
    }
}
