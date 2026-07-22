package org.project.ttokttok.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.domain.enums.AuthProvider;
import org.project.ttokttok.domain.user.exception.GoogleAccountConflictException;
import org.project.ttokttok.domain.user.exception.OnboardingAlreadyCompletedException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.domain.user.service.dto.request.GoogleOnboardingCompleteServiceRequest;
import org.project.ttokttok.domain.user.service.dto.response.GoogleLoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserServiceResponse;
import org.project.ttokttok.global.auth.jwt.dto.request.TokenRequest;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.service.OnboardingTokenProvider;
import org.project.ttokttok.global.auth.jwt.service.OnboardingTokenProvider.OnboardingClaims;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.global.auth.oauth.GoogleIdTokenVerifier;
import org.project.ttokttok.global.auth.oauth.dto.GoogleUserInfo;
import org.project.ttokttok.global.auth.oauth.exception.GoogleEmailNotVerifiedException;
import org.project.ttokttok.infrastructure.redis.service.OnboardingTokenRedisService;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.project.ttokttok.global.entity.Role.ROLE_USER;

/**
 * 구글 OAuth 로그인 서비스
 *
 * 1) 구글 ID 토큰 검증 후 sub(불변 식별자) 우선, email 차선으로 사용자를 조회하여 로그인/자동연동 처리
 * 2) 신규 사용자는 계정을 만들지 않고 10분 TTL 온보딩 토큰 발급 (약관 동의 화면으로 유도)
 * 3) 온보딩 완료 시 SETNX(jti) + DB 유니크 제약 + 멱등 재조회의 3중 방어로
 *    재생(replay)/동시 요청(두 탭) 경쟁을 "로그인 성공"으로 수렴시킨다
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final TokenProvider tokenProvider;
    private final OnboardingTokenProvider onboardingTokenProvider;
    private final OnboardingTokenRedisService onboardingTokenRedisService;
    private final RefreshTokenRedisService refreshTokenRedisService;

    /**
     * 1. 구글 로그인 - 구글 ID 토큰을 검증하고 로그인 또는 온보딩을 안내합니다.
     *
     * @param idToken 프론트엔드가 전달한 구글 ID 토큰
     * @return 로그인 결과 (토큰) 또는 온보딩 필요 응답 (온보딩 토큰)
     * @throws GoogleEmailNotVerifiedException 구글 측 이메일 미검증 시 (자동 연동 = 계정 탈취 벡터 차단)
     */
    public GoogleLoginServiceResponse login(String idToken) {
        // 1-1. 구글 ID 토큰 검증 (서명/iss/aud/exp)
        GoogleUserInfo userInfo = googleIdTokenVerifier.verify(idToken);

        // 1-2. 미검증 이메일 거부 - 검증되지 않은 이메일로 자동 연동하면 계정 탈취 가능
        validateEmailVerified(userInfo);

        // 1-3. sub 우선 조회 (구글 이메일이 변경되어도 로그인 유지)
        Optional<User> bySub = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, userInfo.sub());
        if (bySub.isPresent()) {
            return loginExistingUser(bySub.get());
        }

        // 1-4. email 차선 조회 - 기존 계정이면 자동 연동
        Optional<User> byEmail = userRepository.findByEmail(userInfo.email());
        if (byEmail.isPresent()) {
            return autoLink(byEmail.get(), userInfo.sub());
        }

        // 1-5. 신규 사용자 - 계정을 만들지 않고 온보딩 토큰 발급
        return startOnboarding(userInfo);
    }

    /**
     * 2. 온보딩 완료 - 약관 동의 후 구글 계정으로 회원가입을 완료합니다.
     *
     * @param request 온보딩 완료 요청 (온보딩 토큰, 약관 동의, 이름)
     * @return 로그인 결과 (기존 login 과 동일한 토큰 + 사용자 정보)
     * @throws IllegalArgumentException 약관 미동의 시
     * @throws OnboardingAlreadyCompletedException 이미 사용된 토큰인데 계정이 없는 이상 상태
     */
    public LoginServiceResponse completeOnboarding(GoogleOnboardingCompleteServiceRequest request) {
        // 2-1. 약관 동의 필수
        if (!request.termsAgreed()) {
            throw new IllegalArgumentException("약관 동의가 필요합니다.");
        }

        // 2-2. 온보딩 토큰 검증 (서명/만료/token_type)
        OnboardingClaims claims = onboardingTokenProvider.parse(request.onboardingToken());

        // 2-3. jti 일회성 선점 - 패자(재생/두 탭)는 멱등 처리
        if (!onboardingTokenRedisService.markUsed(claims.jti())) {
            return loginIfAlreadyCompleted(claims.sub());
        }

        // 2-4. 토큰 발급 사이 상태 변화 재확인 후 가입 (동시성 방어)
        User user = findOrSignUp(claims, request.name());

        log.info("구글 온보딩 가입 완료: {}", user.getEmail());
        return issueTokens(user);
    }

    // 기존 사용자 로그인 처리
    private GoogleLoginServiceResponse loginExistingUser(User user) {
        LoginServiceResponse login = issueTokens(user);
        log.info("구글 로그인 성공: {}", user.getEmail());
        return GoogleLoginServiceResponse.ofLogin(
                TokenResponse.of(login.accessToken(), login.refreshToken()), login.user());
    }

    // 기존 이메일 계정에 구글 계정 자동 연동
    private GoogleLoginServiceResponse autoLink(User user, String sub) {
        try {
            user.linkGoogle(sub); // 다른 sub 와 이미 연동 시 GoogleAccountConflictException
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // 병렬 연동 경쟁 - 유니크 제약 충돌 시 sub 로 재조회하여 멱등 처리
            return userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, sub)
                    .map(this::loginExistingUser)
                    .orElseThrow(GoogleAccountConflictException::new);
        }

        log.info("구글 계정 자동 연동 완료: {}", user.getEmail());
        return loginExistingUser(user);
    }

    // 신규 사용자 온보딩 시작 (계정 미생성)
    private GoogleLoginServiceResponse startOnboarding(GoogleUserInfo userInfo) {
        String onboardingToken = onboardingTokenProvider.generate(userInfo);
        log.info("구글 신규 사용자 온보딩 시작: {}", userInfo.email());
        return GoogleLoginServiceResponse.ofOnboarding(onboardingToken, userInfo.email(), userInfo.name());
    }

    // 이미 사용된 온보딩 토큰 - 가입이 완료됐다면 멱등 로그인, 아니면 이상 상태
    private LoginServiceResponse loginIfAlreadyCompleted(String sub) {
        return userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, sub)
                .map(this::issueTokens)
                .orElseThrow(OnboardingAlreadyCompletedException::new);
    }

    // 온보딩 완료 시점의 상태를 재확인하고 필요 시 가입한다
    private User findOrSignUp(OnboardingClaims claims, String name) {
        // 이미 다른 요청이 가입을 끝냈다면 그 계정으로 (멱등)
        Optional<User> bySub = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, claims.sub());
        if (bySub.isPresent()) {
            return bySub.get();
        }

        // 온보딩 토큰 발급 사이에 같은 이메일의 로컬 계정이 생겼다면 자동 연동
        Optional<User> byEmail = userRepository.findByEmail(claims.email());
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.linkGoogle(claims.sub());
            return userRepository.save(existing);
        }

        return signUpNewUser(claims, name);
    }

    // 신규 가입 - DB 유니크 제약 경쟁 시 멱등 재조회
    private User signUpNewUser(OnboardingClaims claims, String name) {
        try {
            return userRepository.save(User.signUpWithGoogle(claims.email(), name, claims.sub()));
        } catch (DataIntegrityViolationException e) {
            // 동시 가입 경쟁 패배 - 같은 sub 로 가입됐다면 멱등 로그인, 아니면 이메일 충돌
            return userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, claims.sub())
                    .orElseThrow(GoogleAccountConflictException::new);
        }
    }

    // 자체 JWT 발급 + 리프레시 토큰 Redis 저장 (기존 login 과 동일)
    private LoginServiceResponse issueTokens(User user) {
        TokenResponse tokens = tokenProvider.generateToken(TokenRequest.of(user.getEmail(), ROLE_USER));
        refreshTokenRedisService.save(user.getEmail(), tokens.refreshToken());
        return LoginServiceResponse.from(tokens, UserServiceResponse.from(user));
    }

    private void validateEmailVerified(GoogleUserInfo userInfo) {
        if (userInfo.email() == null || userInfo.email().isBlank() || !userInfo.emailVerified()) {
            throw new GoogleEmailNotVerifiedException();
        }
    }
}
