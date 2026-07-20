package org.project.ttokttok.domain.superadmin.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.superadmin.controller.dto.request.SuperAdminLoginRequest;
import org.project.ttokttok.domain.superadmin.controller.dto.response.SuperAdminLoginResponse;
import org.project.ttokttok.domain.superadmin.service.SuperAdminAuthService;
import org.project.ttokttok.domain.superadmin.service.dto.response.SuperAdminLoginServiceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminAuthControllerTest {

    private final SuperAdminAuthService superAdminAuthService = mock(SuperAdminAuthService.class);
    private final SuperAdminAuthController superAdminAuthController =
            new SuperAdminAuthController(superAdminAuthService);

    @Test
    @DisplayName("운영자 로그인 요청 시 200과 액세스/리프레시 토큰을 반환한다.")
    void login() {
        // given
        when(superAdminAuthService.login(any()))
                .thenReturn(SuperAdminLoginServiceResponse.of("access-token", "refresh-token"));
        SuperAdminLoginRequest request = new SuperAdminLoginRequest("ttok_operator", "rawPassword");

        // when
        ResponseEntity<SuperAdminLoginResponse> response = superAdminAuthController.login(request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token");
    }
}
