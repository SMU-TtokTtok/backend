package org.project.ttokttok.domain.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.domain.enums.AuthProvider;
import org.project.ttokttok.domain.user.exception.GoogleAccountConflictException;
import org.project.ttokttok.domain.user.exception.OnboardingAlreadyCompletedException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.domain.user.service.dto.request.GoogleOnboardingCompleteServiceRequest;
import org.project.ttokttok.domain.user.service.dto.response.GoogleLoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private OnboardingTokenProvider onboardingTokenProvider;

    @Mock
    private OnboardingTokenRedisService onboardingTokenRedisService;

    @Mock
    private RefreshTokenRedisService refreshTokenRedisService;

    @InjectMocks
    private GoogleOAuthService googleOAuthService;

    private static final String ID_TOKEN = "google-id-token";
    private static final String SUB = "google-sub-123";
    private static final String GMAIL = "user@gmail.com";
    private static final String SMU_EMAIL = "202021000@sangmyung.kr";

    private GoogleUserInfo verifiedUserInfo;
    private TokenResponse tokens;

    @BeforeEach
    void setUp() {
        verifiedUserInfo = GoogleUserInfo.builder()
                .sub(SUB).email(GMAIL).emailVerified(true).name("홍길동")
                .build();
        tokens = TokenResponse.of("access-token", "refresh-token");
    }

    private User googleUser() {
        return User.signUpWithGoogle(GMAIL, "홍길동", SUB);
    }

    private User localUser() {
        return User.signUp(SMU_EMAIL, "encoded-password", "김철수", true);
    }

    @Nested
    @DisplayName("login 메서드")
    class LoginTest {

        @Test
        @DisplayName("sub로 조회되는 기존 사용자는 바로 로그인 처리된다")
        void login_existingUserBySub_returnsTokens() {
            // given
            given(googleIdTokenVerifier.verify(ID_TOKEN)).willReturn(verifiedUserInfo);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.of(googleUser()));
            given(tokenProvider.generateToken(any(TokenRequest.class))).willReturn(tokens);

            // when
            GoogleLoginServiceResponse response = googleOAuthService.login(ID_TOKEN);

            // then
            assertThat(response.needsOnboarding()).isFalse();
            assertThat(response.accessToken()).isEqualTo("access-token");
            verify(refreshTokenRedisService).save(GMAIL, "refresh-token");
        }

        @Test
        @DisplayName("구글 이메일이 바뀌어도 sub가 같으면 기존 계정으로 로그인된다")
        void login_changedGoogleEmail_stillLogsInBySub() {
            // given - 구글 측 이메일이 변경된 상황 (sub 는 불변)
            GoogleUserInfo changedEmailInfo = GoogleUserInfo.builder()
                    .sub(SUB).email("changed@gmail.com").emailVerified(true).name("홍길동")
                    .build();
            User existing = googleUser(); // 우리 DB 에는 기존 이메일로 저장됨
            given(googleIdTokenVerifier.verify(ID_TOKEN)).willReturn(changedEmailInfo);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.of(existing));
            given(tokenProvider.generateToken(any(TokenRequest.class))).willReturn(tokens);

            // when
            GoogleLoginServiceResponse response = googleOAuthService.login(ID_TOKEN);

            // then - 우리 email 컬럼 기준으로 토큰 발급 (email 조회로 빠지지 않음)
            assertThat(response.needsOnboarding()).isFalse();
            assertThat(response.user().email()).isEqualTo(GMAIL);
            verify(userRepository, never()).findByEmail(any());
        }

        @Test
        @DisplayName("이메일이 일치하는 기존 로컬 계정은 자동 연동 후 로그인된다")
        void login_matchingLocalAccount_autoLinksAndLogsIn() {
            // given
            GoogleUserInfo smuInfo = GoogleUserInfo.builder()
                    .sub(SUB).email(SMU_EMAIL).emailVerified(true).name("김철수")
                    .build();
            User local = localUser();
            given(googleIdTokenVerifier.verify(ID_TOKEN)).willReturn(smuInfo);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail(SMU_EMAIL)).willReturn(Optional.of(local));
            given(userRepository.save(local)).willReturn(local);
            given(tokenProvider.generateToken(any(TokenRequest.class))).willReturn(tokens);

            // when
            GoogleLoginServiceResponse response = googleOAuthService.login(ID_TOKEN);

            // then
            assertThat(response.needsOnboarding()).isFalse();
            assertThat(local.getProviderId()).isEqualTo(SUB);
            assertThat(local.getProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(local.getPassword()).isNotNull(); // 비밀번호 로그인도 계속 가능
            verify(refreshTokenRedisService).save(SMU_EMAIL, "refresh-token");
        }

        @Test
        @DisplayName("이미 다른 구글 계정과 연동된 이메일이면 GoogleAccountConflictException을 던진다")
        void login_emailLinkedToDifferentSub_throwsConflict() {
            // given
            User linkedToOther = googleUser(); // providerId = SUB
            GoogleUserInfo otherSubInfo = GoogleUserInfo.builder()
                    .sub("different-sub").email(GMAIL).emailVerified(true).name("홍길동")
                    .build();
            given(googleIdTokenVerifier.verify(ID_TOKEN)).willReturn(otherSubInfo);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "different-sub"))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail(GMAIL)).willReturn(Optional.of(linkedToOther));

            // when & then
            assertThatThrownBy(() -> googleOAuthService.login(ID_TOKEN))
                    .isInstanceOf(GoogleAccountConflictException.class);
        }

        @Test
        @DisplayName("자동 연동 중 유니크 제약 충돌이 나면 sub로 재조회하여 멱등 로그인한다")
        void login_autoLinkRace_recoversIdempotently() {
            // given - 병렬 연동 경쟁에서 패배한 상황
            GoogleUserInfo smuInfo = GoogleUserInfo.builder()
                    .sub(SUB).email(SMU_EMAIL).emailVerified(true).name("김철수")
                    .build();
            User local = localUser();
            User alreadyLinked = googleUser();
            given(googleIdTokenVerifier.verify(ID_TOKEN)).willReturn(smuInfo);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.empty())            // 최초 조회 - 없음
                    .willReturn(Optional.of(alreadyLinked)); // 충돌 후 재조회 - 있음
            given(userRepository.findByEmail(SMU_EMAIL)).willReturn(Optional.of(local));
            given(userRepository.save(local)).willThrow(new DataIntegrityViolationException("duplicate"));
            given(tokenProvider.generateToken(any(TokenRequest.class))).willReturn(tokens);

            // when
            GoogleLoginServiceResponse response = googleOAuthService.login(ID_TOKEN);

            // then
            assertThat(response.needsOnboarding()).isFalse();
        }

        @Test
        @DisplayName("신규 사용자는 계정을 만들지 않고 온보딩 토큰을 발급한다")
        void login_newUser_returnsOnboardingTokenWithoutCreatingAccount() {
            // given
            given(googleIdTokenVerifier.verify(ID_TOKEN)).willReturn(verifiedUserInfo);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail(GMAIL)).willReturn(Optional.empty());
            given(onboardingTokenProvider.generate(verifiedUserInfo)).willReturn("onboarding-token");

            // when
            GoogleLoginServiceResponse response = googleOAuthService.login(ID_TOKEN);

            // then
            assertThat(response.needsOnboarding()).isTrue();
            assertThat(response.onboardingToken()).isEqualTo("onboarding-token");
            assertThat(response.email()).isEqualTo(GMAIL);
            assertThat(response.suggestedName()).isEqualTo("홍길동");
            verify(userRepository, never()).save(any()); // 계정 미생성 검증
            verify(refreshTokenRedisService, never()).save(any(), any());
        }

        @Test
        @DisplayName("구글 측 이메일이 미검증이면 GoogleEmailNotVerifiedException을 던진다")
        void login_unverifiedEmail_throwsNotVerified() {
            // given - 미검증 이메일로 자동 연동하면 계정 탈취가 가능하므로 거부
            GoogleUserInfo unverified = GoogleUserInfo.builder()
                    .sub(SUB).email(GMAIL).emailVerified(false).name("홍길동")
                    .build();
            given(googleIdTokenVerifier.verify(ID_TOKEN)).willReturn(unverified);

            // when & then
            assertThatThrownBy(() -> googleOAuthService.login(ID_TOKEN))
                    .isInstanceOf(GoogleEmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("이메일 클레임이 없으면 GoogleEmailNotVerifiedException을 던진다")
        void login_missingEmail_throwsNotVerified() {
            // given
            GoogleUserInfo noEmail = GoogleUserInfo.builder()
                    .sub(SUB).email(null).emailVerified(true).name("홍길동")
                    .build();
            given(googleIdTokenVerifier.verify(ID_TOKEN)).willReturn(noEmail);

            // when & then
            assertThatThrownBy(() -> googleOAuthService.login(ID_TOKEN))
                    .isInstanceOf(GoogleEmailNotVerifiedException.class);
        }
    }

    @Nested
    @DisplayName("completeOnboarding 메서드")
    class CompleteOnboardingTest {

        private static final String ONBOARDING_TOKEN = "onboarding-token";
        private static final String JTI = "jti-1";

        private final GoogleOnboardingCompleteServiceRequest request =
                GoogleOnboardingCompleteServiceRequest.of(ONBOARDING_TOKEN, true, "홍길동");

        private final OnboardingClaims claims = new OnboardingClaims(SUB, GMAIL, "홍길동", JTI);

        @Test
        @DisplayName("정상 요청이면 계정을 생성하고 토큰을 발급한다")
        void complete_withValidRequest_signsUpAndReturnsTokens() {
            // given
            given(onboardingTokenProvider.parse(ONBOARDING_TOKEN)).willReturn(claims);
            given(onboardingTokenRedisService.markUsed(JTI)).willReturn(true);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail(GMAIL)).willReturn(Optional.empty());
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
            given(tokenProvider.generateToken(any(TokenRequest.class))).willReturn(tokens);

            // when
            LoginServiceResponse response = googleOAuthService.completeOnboarding(request);

            // then
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.user().email()).isEqualTo(GMAIL);
            verify(refreshTokenRedisService).save(GMAIL, "refresh-token");
        }

        @Test
        @DisplayName("약관에 동의하지 않으면 IllegalArgumentException을 던진다")
        void complete_withoutTermsAgreed_throwsIllegalArgument() {
            // given
            GoogleOnboardingCompleteServiceRequest noTerms =
                    GoogleOnboardingCompleteServiceRequest.of(ONBOARDING_TOKEN, false, "홍길동");

            // when & then
            assertThatThrownBy(() -> googleOAuthService.completeOnboarding(noTerms))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("약관 동의가 필요합니다.");
        }

        @Test
        @DisplayName("이미 사용된 토큰이라도 가입이 완료된 계정이 있으면 멱등 로그인한다")
        void complete_replayAfterSignup_logsInIdempotently() {
            // given - 두 탭에서 동시 제출한 상황의 패자
            given(onboardingTokenProvider.parse(ONBOARDING_TOKEN)).willReturn(claims);
            given(onboardingTokenRedisService.markUsed(JTI)).willReturn(false);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.of(googleUser()));
            given(tokenProvider.generateToken(any(TokenRequest.class))).willReturn(tokens);

            // when
            LoginServiceResponse response = googleOAuthService.completeOnboarding(request);

            // then
            assertThat(response.accessToken()).isEqualTo("access-token");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 사용된 토큰인데 계정이 없으면 OnboardingAlreadyCompletedException을 던진다")
        void complete_replayWithoutAccount_throwsAlreadyCompleted() {
            // given
            given(onboardingTokenProvider.parse(ONBOARDING_TOKEN)).willReturn(claims);
            given(onboardingTokenRedisService.markUsed(JTI)).willReturn(false);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> googleOAuthService.completeOnboarding(request))
                    .isInstanceOf(OnboardingAlreadyCompletedException.class);
        }

        @Test
        @DisplayName("온보딩 사이에 같은 이메일의 로컬 계정이 생겼으면 자동 연동한다")
        void complete_localAccountCreatedMeanwhile_autoLinks() {
            // given - 온보딩 토큰 발급과 완료 사이에 이메일 가입이 끝난 상황
            OnboardingClaims smuClaims = new OnboardingClaims(SUB, SMU_EMAIL, "김철수", JTI);
            User local = localUser();
            given(onboardingTokenProvider.parse(ONBOARDING_TOKEN)).willReturn(smuClaims);
            given(onboardingTokenRedisService.markUsed(JTI)).willReturn(true);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail(SMU_EMAIL)).willReturn(Optional.of(local));
            given(userRepository.save(local)).willReturn(local);
            given(tokenProvider.generateToken(any(TokenRequest.class))).willReturn(tokens);

            // when
            LoginServiceResponse response = googleOAuthService.completeOnboarding(request);

            // then
            assertThat(local.getProviderId()).isEqualTo(SUB);
            assertThat(response.user().email()).isEqualTo(SMU_EMAIL);
        }

        @Test
        @DisplayName("가입 저장 시 유니크 제약 충돌이 나면 sub로 재조회하여 멱등 처리한다")
        void complete_signUpRace_recoversIdempotently() {
            // given - 동시 가입 경쟁에서 저장에 실패한 상황
            User winner = googleUser();
            given(onboardingTokenProvider.parse(ONBOARDING_TOKEN)).willReturn(claims);
            given(onboardingTokenRedisService.markUsed(JTI)).willReturn(true);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.empty())        // 저장 전 조회 - 없음
                    .willReturn(Optional.of(winner));    // 충돌 후 재조회 - 있음
            given(userRepository.findByEmail(GMAIL)).willReturn(Optional.empty());
            given(userRepository.save(any(User.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate"));
            given(tokenProvider.generateToken(any(TokenRequest.class))).willReturn(tokens);

            // when
            LoginServiceResponse response = googleOAuthService.completeOnboarding(request);

            // then
            assertThat(response.accessToken()).isEqualTo("access-token");
        }

        @Test
        @DisplayName("유니크 제약 충돌 후 재조회에도 없으면 GoogleAccountConflictException을 던진다")
        void complete_signUpRaceWithEmailConflict_throwsConflict() {
            // given - 같은 이메일의 다른 계정이 선점한 상황 (unique(email) 충돌)
            given(onboardingTokenProvider.parse(ONBOARDING_TOKEN)).willReturn(claims);
            given(onboardingTokenRedisService.markUsed(JTI)).willReturn(true);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail(GMAIL)).willReturn(Optional.empty());
            given(userRepository.save(any(User.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate"));

            // when & then
            assertThatThrownBy(() -> googleOAuthService.completeOnboarding(request))
                    .isInstanceOf(GoogleAccountConflictException.class);
        }
    }
}
