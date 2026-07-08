package org.project.ttokttok.domain.notification.fcm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.notification.fcm.controller.dto.request.FCMTokenDeleteRequest;
import org.project.ttokttok.domain.notification.fcm.controller.dto.request.FCMTokenSaveRequest;
import org.project.ttokttok.domain.notification.fcm.service.FCMTokenService;
import org.project.ttokttok.domain.notification.fcm.service.dto.FCMTokenDeleteServiceRequest;
import org.project.ttokttok.domain.notification.fcm.service.dto.FCMTokenSaveServiceRequest;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FCMTokenApiController.class)
class FCMTokenApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FCMTokenService fcmTokenService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    private static final String USER_EMAIL = "test@sangmyung.kr";

    private void givenAuthenticatedUser() throws Exception {
        given(authUserInfoResolver.supportsParameter(any())).willReturn(true);
        given(authUserInfoResolver.resolveArgument(any(), any(), any(), any())).willReturn(USER_EMAIL);
    }

    @Test
    @WithMockUser
    @DisplayName("FCM 토큰 저장 API를 호출하면 200과 완료 메시지를 응답한다")
    void saveToken() throws Exception {
        givenAuthenticatedUser();

        FCMTokenSaveRequest request = new FCMTokenSaveRequest("fcm-token-value", "WEB");

        mockMvc.perform(post("/api/users/fcm/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("FCM 토큰 저장 완료"));

        verify(fcmTokenService).saveOrUpdate(any(FCMTokenSaveServiceRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("토큰이 비어있는 채로 저장을 요청하면 400을 응답한다")
    void saveTokenBlankToken() throws Exception {
        givenAuthenticatedUser();

        FCMTokenSaveRequest request = new FCMTokenSaveRequest(" ", "WEB");

        mockMvc.perform(post("/api/users/fcm/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("FCM 토큰 삭제 API를 호출하면 200과 완료 메시지를 응답한다")
    void deleteToken() throws Exception {
        givenAuthenticatedUser();

        FCMTokenDeleteRequest request = new FCMTokenDeleteRequest("fcm-token-value");

        mockMvc.perform(delete("/api/users/fcm/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("FCM 토큰 삭제 완료"));

        verify(fcmTokenService).delete(any(FCMTokenDeleteServiceRequest.class));
    }
}
