package org.project.ttokttok.domain.admin.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.exception.AdminNotFoundException;
import org.project.ttokttok.domain.admin.exception.AdminPasswordNotMatchException;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.admin.service.AdminAuthService;
import org.project.ttokttok.domain.admin.service.dto.request.AdminLoginServiceRequest;
import org.project.ttokttok.domain.admin.service.dto.response.AdminLoginServiceResponse;
import org.project.ttokttok.domain.admin.service.dto.response.ReissueServiceResponse;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.global.auth.jwt.dto.request.TokenRequest;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.exception.InvalidRefreshTokenException;
import org.project.ttokttok.global.auth.jwt.exception.InvalidTokenFromCookieException;
import org.project.ttokttok.global.auth.jwt.exception.RefreshTokenNotFoundException;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Transactional
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RefreshTokenRedisService refreshTokenRedisService;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    @DisplayName("login(): 로그인에 성공하면 토큰 페어를 반환한다.")
    void loginSuccess() {
        // Given
        String username = "adminUser";
        String password = "securePassword";
        Admin mockAdmin = mock(Admin.class);
        String clubId = "club-123";
        String clubName = "Test Club";
        Club mockClub = mock(Club.class);

        TokenResponse mockTokenResponse = new TokenResponse("any-access-token", "any-refresh-token");
        AdminLoginServiceRequest request = new AdminLoginServiceRequest(username, password);

        // When
        when(adminRepository.findByUsername(username)).thenReturn(Optional.of(mockAdmin));
        when(mockAdmin.getUsername()).thenReturn(username);
        doNothing().when(mockAdmin).validatePassword(eq(password), any(PasswordEncoder.class));
        when(tokenProvider.generateToken(any(TokenRequest.class))).thenReturn(mockTokenResponse);
        when(clubRepository.findByAdminUsername(eq(username))).thenReturn(Optional.of(mockClub));
        when(mockClub.getId()).thenReturn(clubId);
        when(mockClub.getName()).thenReturn(clubName);

        AdminLoginServiceResponse result = adminAuthService.login(request);

        // Then
        assertNotNull(result);
        assertEquals("any-access-token", result.accessToken());
        assertEquals("any-refresh-token", result.refreshToken());
        assertEquals(clubId, result.clubId());
        assertEquals(clubName, result.clubName());

        verify(adminRepository).findByUsername(username);
        verify(mockAdmin).validatePassword(password, passwordEncoder);
        verify(tokenProvider).generateToken(any(TokenRequest.class));
        verify(clubRepository).findByAdminUsername(username);
    }

    @Test
    @DisplayName("login(): 존재하지 않는 관리자 로그인 시 AdminNotFoundException을 발생시킨다.")
    void loginAdminNotFound() {
        // Given
        String username = "unknown";
        AdminLoginServiceRequest request = new AdminLoginServiceRequest(username, "anyPassword");

        // When
        when(adminRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(AdminNotFoundException.class);
    }

    @Test
    @DisplayName("login(): 비밀번호가 틀릴 경우 예외가 발생한다.")
    void loginInvalidPassword() {
        // Given
        String username = "adminUser";
        String password = "wrongPassword";
        Admin mockAdmin = mock(Admin.class);
        AdminLoginServiceRequest request = new AdminLoginServiceRequest(username, password);

        // When
        when(adminRepository.findByUsername(username)).thenReturn(Optional.of(mockAdmin));
        doThrow(new AdminPasswordNotMatchException()).when(mockAdmin).validatePassword(eq(password), any());

        // Then
        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(AdminPasswordNotMatchException.class);
    }

    @Test
    @DisplayName("reissue(): 쿠키에 토큰이 없으면 InvalidTokenFromCookieException 발생")
    void reissue_fail_token_null() {
        // Given
        String username = "adminUser";
        String nullRefreshToken = null;

        // When & Then
        assertThatThrownBy(() -> adminAuthService.reissue(nullRefreshToken))
                .isInstanceOf(InvalidTokenFromCookieException.class);
    }

    @Test
    @DisplayName("reissue(): Redis에 저장된 토큰이 없으면 RefreshTokenNotFoundException 발생")
    void reissue_fail_token_not_found() {
        // Given
        String username = "adminUser";
        String refreshToken = "some-token";

        // When
        when(refreshTokenRedisService.getUsernameFromRefreshToken(username))
                .thenThrow(new RefreshTokenNotFoundException());

        // Then
        assertThatThrownBy(() -> adminAuthService.reissue(refreshToken))
                .isInstanceOf(RefreshTokenNotFoundException.class);
    }

    @Test
    @DisplayName("reissue(): Redis에 저장된 토큰과 다르면 InvalidRefreshTokenException 발생")
    void reissue_fail_token_mismatch() {
        // Given
        String username = "adminUser";
        String requestToken = "client-token";
        String storedToken = "different-token";

        // When
        when(refreshTokenRedisService.getUsernameFromRefreshToken(username))
                .thenReturn(storedToken);

        // Then
        assertThatThrownBy(() -> adminAuthService.reissue(requestToken))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("reissue(): 유효한 리프레시 토큰이 주어지면 새 토큰과 TTL을 반환한다.")
    void reissueSuccess() {
        // given
        String username = "adminUser";
        String refreshToken = "valid-refresh-token";

        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        long mockTtl = 1800L;

        // TokenResponse stub
        TokenResponse mockTokenResponse = new TokenResponse(newAccessToken, newRefreshToken);

        // Redis에서 동일한 refreshToken 존재함
        when(refreshTokenRedisService.getUsernameFromRefreshToken(username)).thenReturn(refreshToken);

        // 토큰 재발급
        when(tokenProvider.reissueToken(username, Role.ROLE_ADMIN)).thenReturn(mockTokenResponse);

        // Redis TTL 갱신
        when(refreshTokenRedisService.getRefreshTTL(newRefreshToken)).thenReturn(mockTtl);

        // when
        ReissueServiceResponse result = adminAuthService.reissue(refreshToken);

        // then
        assertNotNull(result);
        assertEquals(newAccessToken, result.accessToken());
        assertEquals(newRefreshToken, result.refreshToken());
        assertEquals(mockTtl, result.refreshTTL());

        // verify 호출 순서까지 확인
        verify(refreshTokenRedisService).getUsernameFromRefreshToken(username);
        verify(tokenProvider).reissueToken(username, Role.ROLE_ADMIN);
        verify(refreshTokenRedisService).getRefreshTTL(newRefreshToken);
    }
}