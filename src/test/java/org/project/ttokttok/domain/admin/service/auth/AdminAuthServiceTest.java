package org.project.ttokttok.domain.admin.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.exception.AdminNotFoundException;
import org.project.ttokttok.domain.admin.exception.AdminPasswordNotMatchException;
import org.project.ttokttok.domain.admin.exception.AdminUsernameConflictException;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.admin.service.AdminAuthService;
import org.project.ttokttok.domain.admin.service.dto.request.AdminJoinServiceRequest;
import org.project.ttokttok.domain.admin.service.dto.request.AdminLoginServiceRequest;
import org.project.ttokttok.domain.admin.service.dto.request.AdminResetPasswordServiceRequest;
import org.project.ttokttok.domain.admin.service.dto.response.AdminLoginServiceResponse;
import org.project.ttokttok.domain.admin.service.dto.response.ReissueServiceResponse;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
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

import static org.assertj.core.api.Assertions.*;
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

    // ===== 1. login 테스트 =====
    @Nested
    @DisplayName("login 메서드")
    class LoginTest {

        @Test
        @DisplayName("유효한 자격 증명으로 로그인하면 토큰과 클럽 정보가 반환된다")
        void loginWithValidCredentials() {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("존재하지 않는 사용자명으로 로그인하면 AdminNotFoundException이 발생한다")
        void loginWithNonExistentUsername() {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인하면 AdminPasswordNotMatchException이 발생한다")
        void loginWithWrongPassword() {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("관리자에게 연결된 클럽이 없으면 AdminNotFoundException이 발생한다")
        void loginWithNoAssociatedClub() {
            // given

            // when

            // then
        }
    }

    // ===== 2. logout 테스트 =====
    @Nested
    @DisplayName("logout 메서드")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃하면 RefreshToken이 삭제된다")
        void logoutDeletesRefreshToken() {
            // given

            // when

            // then
        }
    }

    // ===== 3. reissue 테스트 =====
    @Nested
    @DisplayName("reissue 메서드")
    class ReissueTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 토큰을 재발급받을 수 있다")
        void reissueWithValidRefreshToken() {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("리프레시 토큰이 null이면 InvalidTokenFromCookieException이 발생한다")
        void reissueWithNullRefreshToken() {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("Redis에 저장된 토큰이 없으면 RefreshTokenNotFoundException이 발생한다")
        void reissueWithTokenNotFoundInRedis() {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("Redis에 저장된 토큰과 다르면 InvalidRefreshTokenException이 발생한다")
        void reissueWithTokenMismatch() {
            // given

            // when

            // then
        }
    }

    // ===== 4. join 테스트 =====
    @Nested
    @DisplayName("join 메서드")
    class JoinTest {

        private static final String DEFAULT_USERNAME = "newadmin123";
        private static final String DEFAULT_PASSWORD = "password123";
        private static final String DEFAULT_CLUB_NAME = "테스트 동아리";
        // 임의의 대학 값 이용
        private static final ClubUniv DEFAULT_CLUB_UNIV = ClubUniv.ENGINEERING;

        @Test
        @DisplayName("유효한 정보로 회원가입하면 관리자 ID가 반환된다")
        void joinWithValidRequest() {
            // given
            final String expectedAdminId = "admin-uuid-123";
            AdminJoinServiceRequest request = createJoinRequest(
                    DEFAULT_USERNAME, DEFAULT_PASSWORD, DEFAULT_CLUB_NAME, DEFAULT_CLUB_UNIV);

            setupSuccessfulJoinMocks(DEFAULT_USERNAME, DEFAULT_PASSWORD, expectedAdminId);

            // when
            final String result = adminAuthService.join(request);

            // then
            assertThat(result).isEqualTo(expectedAdminId);
            verify(adminRepository).existsByUsername(DEFAULT_USERNAME);
            verify(adminRepository).save(any(Admin.class));
            verify(clubRepository).save(any(Club.class));
        }

        @Test
        @DisplayName("이미 존재하는 사용자명으로 회원가입하면 AdminUsernameConflictException이 발생한다")
        void joinWithDuplicateUsername() {
            // given
            final String duplicateUsername = "existingadmin";
            AdminJoinServiceRequest request = createJoinRequest(
                    duplicateUsername, DEFAULT_PASSWORD, DEFAULT_CLUB_NAME, DEFAULT_CLUB_UNIV);

            when(adminRepository.existsByUsername(duplicateUsername)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> adminAuthService.join(request))
                    .isInstanceOf(AdminUsernameConflictException.class);

            verify(adminRepository).existsByUsername(duplicateUsername);
            verify(adminRepository, never()).save(any(Admin.class));
            verify(clubRepository, never()).save(any(Club.class));
        }

        @Test
        @DisplayName("회원가입 시 비밀번호가 인코딩되어 저장된다")
        void joinEncodesPassword() {
            // given
            final String rawPassword = "rawPassword123";
            AdminJoinServiceRequest request = createJoinRequest(
                    DEFAULT_USERNAME, rawPassword, DEFAULT_CLUB_NAME, ClubUniv.DESIGN);

            setupSuccessfulJoinMocks(DEFAULT_USERNAME, rawPassword, "admin-id");

            // when
            adminAuthService.join(request);

            // then
            verify(passwordEncoder).encode(rawPassword);
        }

        @Test
        @DisplayName("회원가입 시 클럽도 함께 생성된다")
        void joinCreatesClub() {
            // given
            AdminJoinServiceRequest request = createJoinRequest(
                    DEFAULT_USERNAME, DEFAULT_PASSWORD, "새로운 동아리", ClubUniv.ARTS);

            setupSuccessfulJoinMocks(DEFAULT_USERNAME, DEFAULT_PASSWORD, "admin-id");

            // when
            adminAuthService.join(request);

            // then
            verify(clubRepository).save(any(Club.class));
        }

        // ===== Helper Methods =====
        private AdminJoinServiceRequest createJoinRequest(final String username,
                                                          final String password,
                                                          final String clubName,
                                                          final ClubUniv clubUniv) {
            return AdminJoinServiceRequest.builder()
                    .username(username)
                    .password(password)
                    .clubName(clubName)
                    .clubUniv(clubUniv)
                    .build();
        }

        private void setupSuccessfulJoinMocks(final String username,
                                              final String password,
                                              final String adminId) {
            when(adminRepository.existsByUsername(username)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
            when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> {
                final Admin savedAdmin = mock(Admin.class);
                when(savedAdmin.getId()).thenReturn(adminId);
                return savedAdmin;
            });
        }
    }

    // ===== 5. getAdminInfo 테스트 =====
    @Nested
    @DisplayName("getAdminInfo 메서드")
    class GetAdminInfoTest {

        @Test
        @DisplayName("관리자명으로 관리자 정보를 조회할 수 있다")
        void getAdminInfoWithValidUsername() {
            // given
            final String adminUsername = "testadmin123";
            final String clubId = "club-uuid-123";
            final String clubName = "테스트 동아리";

            final Club mockClub = mock(Club.class);
            when(mockClub.getId()).thenReturn(clubId);
            when(mockClub.getName()).thenReturn(clubName);
            when(clubRepository.findByAdminUsername(adminUsername))
                    .thenReturn(Optional.of(mockClub));

            // when
            var result = adminAuthService.getAdminInfo(adminUsername);

            // then
            assertThat(result).isNotNull();
            assertThat(result.clubId()).isEqualTo(clubId);
            assertThat(result.clubName()).isEqualTo(clubName);
            verify(clubRepository).findByAdminUsername(adminUsername);
        }

        @Test
        @DisplayName("존재하지 않는 관리자명으로 조회하면 AdminNotFoundException이 발생한다")
        void getAdminInfoWithNonExistentUsername() {
            // given
            final String nonExistentUsername = "nonexistent123";
            when(clubRepository.findByAdminUsername(nonExistentUsername))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminAuthService.getAdminInfo(nonExistentUsername))
                    .isInstanceOf(AdminNotFoundException.class);
            verify(clubRepository).findByAdminUsername(nonExistentUsername);
        }
    }

    // ===== 6. resetPassword 테스트 =====
    @Nested
    @DisplayName("resetPassword 메서드")
    class ResetPasswordTest {

        @Test
        @DisplayName("새 비밀번호와 확인 비밀번호가 일치하면 비밀번호가 재설정된다")
        void resetPasswordWithMatchingPasswords() {
            // given
            final String username = "testadmin123";
            final String newPassword = "newPassword123";
            final String newPasswordConfirm = "newPassword123";

            AdminResetPasswordServiceRequest request = AdminResetPasswordServiceRequest.builder()
                    .username(username)
                    .newPassword(newPassword)
                    .newPasswordConfirm(newPasswordConfirm)
                    .build();

            final Admin mockAdmin = mock(Admin.class);
            when(adminRepository.findByUsername(username)).thenReturn(Optional.of(mockAdmin));

            // when
            adminAuthService.resetPassword(request);

            // then
            verify(adminRepository).findByUsername(username);
            verify(mockAdmin).resetPassword(newPassword, passwordEncoder);
        }

        @Test
        @DisplayName("새 비밀번호와 확인 비밀번호가 일치하지 않으면 IllegalArgumentException이 발생한다")
        void resetPasswordWithMismatchedPasswords() {
            // given
            final String username = "testadmin123";
            final String newPassword = "newPassword123";
            final String newPasswordConfirm = "differentPassword456";

            AdminResetPasswordServiceRequest request = AdminResetPasswordServiceRequest.builder()
                    .username(username)
                    .newPassword(newPassword)
                    .newPasswordConfirm(newPasswordConfirm)
                    .build();

            // when & then
            assertThatThrownBy(() -> adminAuthService.resetPassword(request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(adminRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("존재하지 않는 사용자명으로 비밀번호 재설정을 시도하면 AdminNotFoundException이 발생한다")
        void resetPasswordWithNonExistentUsername() {
            // given
            final String nonExistentUsername = "nonexistent123";
            final String newPassword = "newPassword123";

            AdminResetPasswordServiceRequest request = AdminResetPasswordServiceRequest.builder()
                    .username(nonExistentUsername)
                    .newPassword(newPassword)
                    .newPasswordConfirm(newPassword)
                    .build();

            when(adminRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminAuthService.resetPassword(request))
                    .isInstanceOf(AdminNotFoundException.class);

            verify(adminRepository).findByUsername(nonExistentUsername);
        }

        @Test
        @DisplayName("비밀번호 재설정 시 Admin.resetPassword 메서드가 호출된다")
        void resetPasswordCallsAdminResetPassword() {
            // given
            final String username = "testadmin123";
            final String newPassword = "newSecurePassword123";

            final AdminResetPasswordServiceRequest request = AdminResetPasswordServiceRequest.builder()
                    .username(username)
                    .newPassword(newPassword)
                    .newPasswordConfirm(newPassword)
                    .build();

            final Admin mockAdmin = mock(Admin.class);
            when(adminRepository.findByUsername(username)).thenReturn(Optional.of(mockAdmin));

            // when
            adminAuthService.resetPassword(request);

            // then
            verify(mockAdmin).resetPassword(eq(newPassword), eq(passwordEncoder));
        }
    }
}