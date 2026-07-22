package org.project.ttokttok.domain.superadmin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.superadmin.domain.SuperAdmin;
import org.project.ttokttok.domain.superadmin.exception.SuperAdminNotFoundException;
import org.project.ttokttok.domain.superadmin.exception.SuperAdminPasswordNotMatchException;
import org.project.ttokttok.domain.superadmin.repository.SuperAdminRepository;
import org.project.ttokttok.domain.superadmin.service.dto.request.SuperAdminLoginServiceRequest;
import org.project.ttokttok.domain.superadmin.service.dto.response.SuperAdminLoginServiceResponse;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminAuthServiceTest {

    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final SuperAdminRepository superAdminRepository = mock(SuperAdminRepository.class);
    private final TokenProvider tokenProvider = mock(TokenProvider.class);
    private final RefreshTokenRedisService refreshTokenRedisService = mock(RefreshTokenRedisService.class);
    private final SuperAdminAuthService superAdminAuthService =
            new SuperAdminAuthService(passwordEncoder, superAdminRepository, tokenProvider, refreshTokenRedisService);

    @Test
    @DisplayName("운영자 로그인에 성공하면 토큰을 반환하고 리프레시 토큰을 저장한다.")
    void loginSuccess() {
        // given
        SuperAdmin superAdmin = mock(SuperAdmin.class);
        when(superAdmin.getUsername()).thenReturn("ttok_operator");
        when(superAdminRepository.findByUsername("ttok_operator")).thenReturn(Optional.of(superAdmin));
        when(tokenProvider.generateToken(any())).thenReturn(TokenResponse.of("access-token", "refresh-token"));

        SuperAdminLoginServiceRequest request = SuperAdminLoginServiceRequest.builder()
                .username("ttok_operator")
                .password("rawPassword")
                .build();

        // when
        SuperAdminLoginServiceResponse response = superAdminAuthService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(superAdmin).validatePassword("rawPassword", passwordEncoder);
        verify(refreshTokenRedisService).save("ttok_operator", "refresh-token");
    }

    @Test
    @DisplayName("존재하지 않는 운영자로 로그인하면 예외가 발생한다.")
    void loginNotFound() {
        // given
        when(superAdminRepository.findByUsername("missing")).thenReturn(Optional.empty());

        SuperAdminLoginServiceRequest request = SuperAdminLoginServiceRequest.builder()
                .username("missing")
                .password("rawPassword")
                .build();

        // when & then
        assertThatThrownBy(() -> superAdminAuthService.login(request))
                .isInstanceOf(SuperAdminNotFoundException.class);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다.")
    void loginPasswordMismatch() {
        // given
        SuperAdmin superAdmin = mock(SuperAdmin.class);
        doThrow(new SuperAdminPasswordNotMatchException())
                .when(superAdmin).validatePassword("wrongPassword", passwordEncoder);
        when(superAdminRepository.findByUsername("ttok_operator")).thenReturn(Optional.of(superAdmin));

        SuperAdminLoginServiceRequest request = SuperAdminLoginServiceRequest.builder()
                .username("ttok_operator")
                .password("wrongPassword")
                .build();

        // when & then
        assertThatThrownBy(() -> superAdminAuthService.login(request))
                .isInstanceOf(SuperAdminPasswordNotMatchException.class);
    }
}
