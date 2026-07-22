package org.project.ttokttok.domain.user.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.user.service.GoogleOAuthService;
import org.project.ttokttok.domain.user.service.dto.request.GoogleOnboardingCompleteServiceRequest;
import org.project.ttokttok.domain.user.service.dto.response.GoogleLoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserServiceResponse;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.exception.OnboardingTokenExpiredException;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserOAuthController.class)
class UserOAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleOAuthService googleOAuthService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    private static final String GMAIL = "user@gmail.com";

    private UserServiceResponse userServiceResponse() {
        return UserServiceResponse.builder()
                .id("user-1").email(GMAIL).name("홍길동")
                .isEmailVerified(true).termsAgreed(true).build();
    }

    @Test
    @WithMockUser
    @DisplayName("구글 로그인 API는 기존 사용자면 토큰과 함께 200을 반환한다")
    void googleLogin_existingUser_returnsTokens() throws Exception {
        GoogleLoginServiceResponse serviceResponse = GoogleLoginServiceResponse.ofLogin(
                TokenResponse.of("access-token", "refresh-token"), userServiceResponse());
        given(googleOAuthService.login(any())).willReturn(serviceResponse);

        mockMvc.perform(post("/api/user/auth/oauth/google").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.needsOnboarding").value(false))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.email").value(GMAIL));
    }

    @Test
    @WithMockUser
    @DisplayName("구글 로그인 API는 신규 사용자면 온보딩 토큰과 함께 200을 반환한다")
    void googleLogin_newUser_returnsOnboardingToken() throws Exception {
        GoogleLoginServiceResponse serviceResponse = GoogleLoginServiceResponse.ofOnboarding(
                "onboarding-token", GMAIL, "홍길동");
        given(googleOAuthService.login(any())).willReturn(serviceResponse);

        mockMvc.perform(post("/api/user/auth/oauth/google").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("약관 동의가 필요합니다."))
                .andExpect(jsonPath("$.data.needsOnboarding").value(true))
                .andExpect(jsonPath("$.data.onboardingToken").value("onboarding-token"))
                .andExpect(jsonPath("$.data.suggestedName").value("홍길동"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @Test
    @WithMockUser
    @DisplayName("idToken이 비어있으면 구글 로그인 API는 400을 반환한다")
    void googleLogin_withBlankIdToken_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/user/auth/oauth/google").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("온보딩 완료 API는 200과 로그인 응답을 반환한다")
    void completeOnboarding_returnsTokens() throws Exception {
        LoginServiceResponse serviceResponse = LoginServiceResponse.from(
                TokenResponse.of("access-token", "refresh-token"), userServiceResponse());
        given(googleOAuthService.completeOnboarding(any(GoogleOnboardingCompleteServiceRequest.class)))
                .willReturn(serviceResponse);

        String body = "{\"onboardingToken\":\"onboarding-token\",\"termsAgreed\":true,\"name\":\"홍길동\"}";

        mockMvc.perform(post("/api/user/auth/oauth/google/complete").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입 및 로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.email").value(GMAIL));
    }

    @Test
    @WithMockUser
    @DisplayName("이름이 1자면 온보딩 완료 API는 400을 반환한다")
    void completeOnboarding_withShortName_returnsBadRequest() throws Exception {
        String body = "{\"onboardingToken\":\"onboarding-token\",\"termsAgreed\":true,\"name\":\"김\"}";

        mockMvc.perform(post("/api/user/auth/oauth/google/complete").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("이름이 11자면 온보딩 완료 API는 400을 반환한다")
    void completeOnboarding_withLongName_returnsBadRequest() throws Exception {
        String body = "{\"onboardingToken\":\"onboarding-token\",\"termsAgreed\":true,"
                + "\"name\":\"열한글자가넘는이름입니다\"}";

        mockMvc.perform(post("/api/user/auth/oauth/google/complete").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("온보딩 토큰이 만료됐으면 온보딩 완료 API는 401을 반환한다")
    void completeOnboarding_withExpiredToken_returnsUnauthorized() throws Exception {
        given(googleOAuthService.completeOnboarding(any(GoogleOnboardingCompleteServiceRequest.class)))
                .willThrow(new OnboardingTokenExpiredException());

        String body = "{\"onboardingToken\":\"expired-token\",\"termsAgreed\":true,\"name\":\"홍길동\"}";

        mockMvc.perform(post("/api/user/auth/oauth/google/complete").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
