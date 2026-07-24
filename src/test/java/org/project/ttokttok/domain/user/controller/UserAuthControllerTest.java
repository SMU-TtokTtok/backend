package org.project.ttokttok.domain.user.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.user.service.UserAuthService;
import org.project.ttokttok.domain.user.service.dto.request.LoginServiceRequest;
import org.project.ttokttok.domain.user.service.dto.request.ResetPasswordServiceRequest;
import org.project.ttokttok.domain.user.service.dto.request.SignupServiceRequest;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserReissueServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserServiceResponse;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAuthController.class)
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAuthService userAuthService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    private static final String EMAIL = "202021000@sangmyung.kr";

    @Test
    @WithMockUser
    @DisplayName("이메일 인증코드 발송 API를 호출하면 200을 반환한다")
    void sendVerificationCode() throws Exception {
        doNothing().when(userAuthService).sendVerificationCode(any());

        mockMvc.perform(post("/api/user/auth/send-verification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("인증코드가 발송되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("이메일 인증코드 검증 API를 호출하면 200을 반환한다")
    void verifyEmail() throws Exception {
        given(userAuthService.verifyEmail(any(), any())).willReturn(true);

        mockMvc.perform(post("/api/user/auth/verify-email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("회원가입 API를 호출하면 200과 사용자 정보를 반환한다")
    void signup() throws Exception {
        UserServiceResponse serviceResponse = UserServiceResponse.builder()
                .id("user-1").email(EMAIL).name("홍길동")
                .isEmailVerified(true).termsAgreed(true).build();
        given(userAuthService.signup(any(SignupServiceRequest.class))).willReturn(serviceResponse);

        String body = "{\"email\":\"" + EMAIL + "\",\"verificationCode\":\"123456\","
                + "\"password\":\"Password123!\",\"passwordConfirm\":\"Password123!\","
                + "\"name\":\"홍길동\",\"termsAgreed\":true}";

        mockMvc.perform(post("/api/user/auth/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.name").value("홍길동"));
    }

    @Test
    @WithMockUser
    @DisplayName("비밀번호가 약하면 회원가입은 400을 반환한다")
    void signup_withWeakPassword_returnsBadRequest() throws Exception {
        String body = "{\"email\":\"" + EMAIL + "\",\"verificationCode\":\"123456\","
                + "\"password\":\"weak\",\"passwordConfirm\":\"weak\","
                + "\"name\":\"홍길동\",\"termsAgreed\":true}";

        mockMvc.perform(post("/api/user/auth/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("로그인 API를 호출하면 200과 토큰/사용자 정보를 반환한다")
    void login() throws Exception {
        UserServiceResponse user = UserServiceResponse.builder()
                .id("user-1").email(EMAIL).name("홍길동")
                .isEmailVerified(true).termsAgreed(true).build();
        LoginServiceResponse serviceResponse = LoginServiceResponse.builder()
                .accessToken("access-token").refreshToken("refresh-token").user(user).build();
        given(userAuthService.login(any(LoginServiceRequest.class))).willReturn(serviceResponse);

        String body = "{\"email\":\"" + EMAIL + "\",\"password\":\"Password123!\",\"rememberMe\":true}";

        mockMvc.perform(post("/api/user/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.user.email").value(EMAIL));
    }

    @Test
    @WithMockUser
    @DisplayName("토큰 재발급 API를 호출하면 200과 새 토큰을 반환한다")
    void reIssue() throws Exception {
        given(userAuthService.reissue(any()))
                .willReturn(new UserReissueServiceResponse("new-access", "new-refresh", 3600L));

        mockMvc.perform(post("/api/user/auth/re-issue").with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer some-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("토큰 재발급 성공"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    @WithMockUser
    @DisplayName("비밀번호 재설정 코드 발송 API를 호출하면 200을 반환한다")
    void sendPasswordResetCode() throws Exception {
        doNothing().when(userAuthService).sendPasswordResetCode(any());

        mockMvc.perform(post("/api/user/auth/send-reset-code").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호 재설정 코드가 발송되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("비밀번호 재설정 API를 호출하면 200을 반환한다")
    void resetPassword() throws Exception {
        doNothing().when(userAuthService).resetPassword(any(ResetPasswordServiceRequest.class));

        String body = "{\"email\":\"" + EMAIL + "\",\"verificationCode\":\"123456\","
                + "\"newPassword\":\"Password123!\",\"newPasswordConfirm\":\"Password123!\"}";

        mockMvc.perform(post("/api/user/auth/reset-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 재설정되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("로그아웃 API를 호출하면 200을 반환한다")
    void logout() throws Exception {
        doNothing().when(userAuthService).logout(any(), any());

        mockMvc.perform(post("/api/user/auth/logout").with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."));
    }
}
