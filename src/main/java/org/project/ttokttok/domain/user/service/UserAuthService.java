package org.project.ttokttok.domain.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.exception.OAuthOnlyAccountException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.domain.user.service.dto.request.LoginServiceRequest;
import org.project.ttokttok.domain.user.service.dto.request.SignupServiceRequest;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserReissueServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserServiceResponse;
import org.project.ttokttok.global.auth.jwt.dto.request.TokenRequest;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.exception.InvalidRefreshTokenException;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.project.ttokttok.global.entity.Role.ROLE_USER;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAuthService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRedisService refreshTokenRedisService;

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
        emailVerificationService.requireVerifiedEmail(request.email());

        // 3-4. 사용자 정보 저장
        User user = User.signUp(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.termsAgreed()
        );

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

        // 4-2. OAuth 전용 계정(비밀번호 없음)은 비밀번호 로그인 불가
        if (user.isOAuthOnly()) {
            throw new OAuthOnlyAccountException();
        }

        // 4-3. 비밀번호 검증
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
        //log.info("로그아웃 완료: {}", email);
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
            throw new InvalidRefreshTokenException();
        }
    }

}
