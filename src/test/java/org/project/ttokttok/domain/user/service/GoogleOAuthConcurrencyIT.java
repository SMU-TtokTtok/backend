package org.project.ttokttok.domain.user.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.domain.enums.AuthProvider;
import org.project.ttokttok.domain.user.exception.GoogleAccountConflictException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.domain.user.service.dto.request.GoogleOnboardingCompleteServiceRequest;
import org.project.ttokttok.domain.user.service.dto.response.GoogleLoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.service.OnboardingTokenProvider;
import org.project.ttokttok.global.auth.jwt.service.OnboardingTokenProvider.OnboardingClaims;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.global.auth.oauth.GoogleIdTokenVerifier;
import org.project.ttokttok.global.auth.oauth.dto.GoogleUserInfo;
import org.project.ttokttok.infrastructure.redis.service.OnboardingTokenRedisService;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * 구글 OAuth 동시성 통합 테스트 (실제 H2 커밋 발생 — {@code @Transactional} 미사용)
 *
 * P1-1 회귀 방지: 단위 테스트의 mock 경계는 "flush 시점 유니크 제약 위반 → 롤백 → 멱등 복구"를
 * 재현하지 못한다. 이 테스트는 실제 트랜잭션 커밋/롤백을 발생시켜 그 경로를 검증한다.
 * 구글/Redis 실호출을 피하기 위해 검증기·토큰 provider·Redis 서비스만 목으로 대체하고,
 * DB(UserRepository/GoogleAccountWriter/GoogleOAuthService)는 실제 빈을 사용한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class GoogleOAuthConcurrencyIT {

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @MockitoBean
    private OnboardingTokenProvider onboardingTokenProvider;

    @MockitoBean
    private OnboardingTokenRedisService onboardingTokenRedisService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private RefreshTokenRedisService refreshTokenRedisService;

    private static final String SUB = "google-sub-123";
    private static final String GMAIL = "user@gmail.com";
    private static final String SMU_EMAIL = "202021000@sangmyung.kr";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // 토큰 발급은 검증 대상이 아니므로 고정값 목 (Redis 미사용)
        given(tokenProvider.generateToken(any())).willReturn(TokenResponse.of("access-token", "refresh-token"));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private OnboardingClaims claims(String email) {
        return new OnboardingClaims(SUB, email, "홍길동", "jti-1");
    }

    @Test
    @DisplayName("동일 sub로 동시에 온보딩 완료해도 계정은 1개만 생성되고 두 요청 모두 토큰을 받는다")
    void completeOnboarding_concurrentSameSub_createsSingleRowAndBothSucceed() throws Exception {
        // given - 두 스레드가 같은 구글 계정으로 동시에 가입 완료 시도 (jti SETNX 는 통과했다고 가정)
        given(onboardingTokenProvider.parse(any())).willReturn(claims(GMAIL));
        given(onboardingTokenRedisService.markUsed(any())).willReturn(true);
        GoogleOnboardingCompleteServiceRequest request =
                GoogleOnboardingCompleteServiceRequest.of("onboarding-token", true, "홍길동");

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);

        Callable<LoginServiceResponse> task = () -> {
            startGate.await(); // 두 스레드를 최대한 동시에 출발시켜 flush 경쟁 유도
            return googleOAuthService.completeOnboarding(request);
        };

        // when
        Future<LoginServiceResponse> f1 = pool.submit(task);
        Future<LoginServiceResponse> f2 = pool.submit(task);
        startGate.countDown();

        LoginServiceResponse r1 = f1.get();
        LoginServiceResponse r2 = f2.get();
        pool.shutdown();

        // then - 500 없이 둘 다 토큰 발급, 그리고 계정은 정확히 1개 (유니크 제약 + 멱등 복구)
        assertThat(r1.accessToken()).isNotBlank();
        assertThat(r2.accessToken()).isNotBlank();
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getProviderId()).isEqualTo(SUB);
        assertThat(users.get(0).getProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    @DisplayName("이미 같은 sub 계정이 커밋돼 있으면 온보딩 완료는 멱등 로그인으로 수렴한다")
    void completeOnboarding_winnerAlreadyCommitted_isIdempotent() {
        // given - 승자 행을 먼저 커밋
        userRepository.saveAndFlush(User.signUpWithGoogle(GMAIL, "홍길동", SUB));
        given(onboardingTokenProvider.parse(any())).willReturn(claims(GMAIL));
        given(onboardingTokenRedisService.markUsed(any())).willReturn(true);
        GoogleOnboardingCompleteServiceRequest request =
                GoogleOnboardingCompleteServiceRequest.of("onboarding-token", true, "홍길동");

        // when
        LoginServiceResponse response = googleOAuthService.completeOnboarding(request);

        // then
        assertThat(response.accessToken()).isNotBlank();
        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("온보딩 완료 사이 같은 이메일 로컬 계정이 커밋돼 있으면 자동 연동되어 토큰을 발급한다")
    void completeOnboarding_localAccountCommittedMeanwhile_autoLinks() {
        // given - 같은 이메일의 로컬(비밀번호) 계정이 이미 커밋됨
        userRepository.saveAndFlush(User.signUp(SMU_EMAIL, "encoded-password", "김철수", true));
        given(onboardingTokenProvider.parse(any())).willReturn(claims(SMU_EMAIL));
        given(onboardingTokenRedisService.markUsed(any())).willReturn(true);
        GoogleOnboardingCompleteServiceRequest request =
                GoogleOnboardingCompleteServiceRequest.of("onboarding-token", true, "김철수");

        // when
        LoginServiceResponse response = googleOAuthService.completeOnboarding(request);

        // then - 새 행이 생기지 않고 기존 로컬 계정에 연동
        assertThat(response.accessToken()).isNotBlank();
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getProviderId()).isEqualTo(SUB);
        assertThat(users.get(0).getPassword()).isNotNull(); // 비밀번호 로그인도 유지
    }

    @Test
    @DisplayName("로그인 시 이메일 일치 로컬 계정은 실제로 연동되어 커밋된다")
    void login_matchingLocalAccount_reallyLinksAndCommits() {
        // given
        userRepository.saveAndFlush(User.signUp(SMU_EMAIL, "encoded-password", "김철수", true));
        given(googleIdTokenVerifier.verify(any())).willReturn(GoogleUserInfo.builder()
                .sub(SUB).email(SMU_EMAIL).emailVerified(true).name("김철수").build());

        // when
        GoogleLoginServiceResponse response = googleOAuthService.login("id-token");

        // then
        assertThat(response.needsOnboarding()).isFalse();
        User linked = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB).orElseThrow();
        assertThat(linked.getEmail()).isEqualTo(SMU_EMAIL);
    }

    @Test
    @DisplayName("이미 다른 구글 계정과 연동된 이메일이면 GoogleAccountConflictException을 던진다")
    void login_emailOwnedByDifferentGoogleAccount_throwsConflict() {
        // given - 같은 이메일이 다른 sub 의 구글 계정으로 이미 연동돼 커밋됨
        userRepository.saveAndFlush(User.signUpWithGoogle(GMAIL, "홍길동", "other-sub"));
        given(googleIdTokenVerifier.verify(any())).willReturn(GoogleUserInfo.builder()
                .sub(SUB).email(GMAIL).emailVerified(true).name("홍길동").build());

        // when & then
        assertThatThrownBy(() -> googleOAuthService.login("id-token"))
                .isInstanceOf(GoogleAccountConflictException.class);
    }
}
