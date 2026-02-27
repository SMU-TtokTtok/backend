package org.project.ttokttok.domain.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.domain.user.service.dto.request.LoginServiceRequest;
import org.project.ttokttok.domain.user.service.dto.request.ResetPasswordServiceRequest;
import org.project.ttokttok.domain.user.service.dto.request.SignupServiceRequest;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserReissueServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserServiceResponse;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.exception.InvalidRefreshTokenException;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRedisService refreshTokenRedisService;

    @InjectMocks
    private UserAuthService userAuthService;

    @Nested
    @DisplayName("이메일 인증코드 전송 테스트")
    class SendVerificationCode {

        @Test
        @DisplayName("상명대학교 이메일 형식인 경우 인증코드를 전송하고 저장한다")
        void sendVerificationCode_success() {
            // given
            String email = "test@sangmyung.kr";
            given(emailService.isValidSangmyungEmail(email)).willReturn(true);
            given(emailService.sendVerificationCode(email)).willReturn("123456");

            // when
            userAuthService.sendVerificationCode(email);

            // then
            verify(emailService, times(1)).isValidSangmyungEmail(email);
            verify(emailVerificationRepository, times(1)).expireAllPendingVerifications(email);
            verify(emailService, times(1)).sendVerificationCode(email);
            verify(emailVerificationRepository, times(1)).save(any(EmailVerification.class));
        }

        @Test
        @DisplayName("상명대학교 이메일 형식이 아닌 경우 예외가 발생한다")
        void sendVerificationCode_fail_invalidEmail() {
            // given
            String email = "test@gmail.com";
            given(emailService.isValidSangmyungEmail(email)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userAuthService.sendVerificationCode(email))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상명대학교 이메일만 사용 가능합니다.");

            verify(emailService, times(1)).isValidSangmyungEmail(email);
            verify(emailVerificationRepository, never()).expireAllPendingVerifications(anyString());
        }
    }

    @Nested
    @DisplayName("이메일 인증코드 검증 테스트")
    class VerifyEmail {

        @Test
        @DisplayName("올바른 인증코드이고 만료되지 않았으면 인증에 성공한다")
        void verifyEmail_success() {
            // given
            String email = "test@sangmyung.kr";
            String code = "123456";
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .code(code)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            given(emailVerificationRepository.findByEmailAndCodeAndIsVerifiedFalse(email, code))
                    .willReturn(java.util.Optional.of(verification));

            // when
            boolean result = userAuthService.verifyEmail(email, code);

            // then
            assertThat(result).isTrue();
            assertThat(verification.isVerified()).isTrue();
            verify(emailVerificationRepository, times(1)).findByEmailAndCodeAndIsVerifiedFalse(email, code);
        }

        @Test
        @DisplayName("잘못된 인증코드인 경우 예외가 발생한다")
        void verifyEmail_fail_invalidCode() {
            // given
            String email = "test@sangmyung.kr";
            String code = "wrong";
            given(emailVerificationRepository.findByEmailAndCodeAndIsVerifiedFalse(email, code))
                    .willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> userAuthService.verifyEmail(email, code))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("올바르지 않은 인증코드입니다.");

            verify(emailVerificationRepository, times(1)).findByEmailAndCodeAndIsVerifiedFalse(email, code);
        }

        @Test
        @DisplayName("만료된 인증코드인 경우 예외가 발생한다")
        void verifyEmail_fail_expiredCode() {
            // given
            String email = "test@sangmyung.kr";
            String code = "123456";
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .code(code)
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();

            given(emailVerificationRepository.findByEmailAndCodeAndIsVerifiedFalse(email, code))
                    .willReturn(java.util.Optional.of(verification));

            // when & then
            assertThatThrownBy(() -> userAuthService.verifyEmail(email, code))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증코드가 만료되었습니다.");

            verify(emailVerificationRepository, times(1)).findByEmailAndCodeAndIsVerifiedFalse(email, code);
        }
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class Signup {

        @Test
        @DisplayName("모든 조건이 충족되면 회원가입에 성공한다")
        void signup_success() {
            // given
            SignupServiceRequest request = SignupServiceRequest.builder()
                    .email("test@sangmyung.kr")
                    .password("password123")
                    .passwordConfirm("password123")
                    .name("테스터")
                    .termsAgreed(true)
                    .build();

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(emailVerificationRepository.existsByEmailAndIsVerifiedTrue(request.email())).willReturn(true);
            given(passwordEncoder.encode(request.password())).willReturn("encoded-password");
            
            User savedUser = new User();
            savedUser.setId("uuid-123");
            savedUser.setEmail(request.email());
            savedUser.setName(request.name());
            
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            UserServiceResponse response = userAuthService.signup(request);

            // then
            assertThat(response.email()).isEqualTo(request.email());
            assertThat(response.name()).isEqualTo(request.name());
            
            verify(userRepository, times(1)).existsByEmail(request.email());
            verify(emailVerificationRepository, times(1)).existsByEmailAndIsVerifiedTrue(request.email());
            verify(passwordEncoder, times(1)).encode(request.password());
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("비밀번호와 비밀번호 확인이 일치하지 않으면 예외가 발생한다")
        void signup_fail_passwordMismatch() {
            // given
            SignupServiceRequest request = SignupServiceRequest.builder()
                    .email("test@sangmyung.kr")
                    .password("password123")
                    .passwordConfirm("different-password")
                    .build();

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> userAuthService.signup(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호가 일치하지 않습니다.");

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("이미 가입된 이메일인 경우 예외가 발생한다")
        void signup_fail_duplicateEmail() {
            // given
            SignupServiceRequest request = SignupServiceRequest.builder()
                    .email("test@sangmyung.kr")
                    .password("password123")
                    .passwordConfirm("password123")
                    .build();

            given(userRepository.existsByEmail(request.email())).willReturn(true);

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> userAuthService.signup(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 가입된 이메일입니다.");

            verify(userRepository, times(1)).existsByEmail(request.email());
        }

        @Test
        @DisplayName("이메일 인증이 완료되지 않은 경우 예외가 발생한다")
        void signup_fail_emailNotVerified() {
            // given
            SignupServiceRequest request = SignupServiceRequest.builder()
                    .email("test@sangmyung.kr")
                    .password("password123")
                    .passwordConfirm("password123")
                    .build();

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(emailVerificationRepository.existsByEmailAndIsVerifiedTrue(request.email())).willReturn(false);

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> userAuthService.signup(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이메일 인증이 완료되지 않았습니다.");

            verify(emailVerificationRepository, times(1)).existsByEmailAndIsVerifiedTrue(request.email());
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class Login {

        @Test
        @DisplayName("이메일과 비밀번호가 일치하고 인증된 사용자인 경우 토큰을 발급한다")
        void login_success() {
            // given
            String email = "test@sangmyung.kr";
            String password = "password123";
            LoginServiceRequest request = LoginServiceRequest.builder()
                    .email(email)
                    .password(password)
                    .build();

            User user = new User();
            user.setId("uuid-123");
            user.setEmail(email);
            user.setPassword("encoded-password");
            user.setEmailVerified(true);

            TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token");

            given(userRepository.findByEmail(email)).willReturn(java.util.Optional.of(user));
            given(passwordEncoder.matches(password, user.getPassword())).willReturn(true);
            given(tokenProvider.generateToken(any())).willReturn(tokenResponse);

            // when
            LoginServiceResponse response = userAuthService.login(request);

            // then
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.user().email()).isEqualTo(email);

            verify(userRepository, times(1)).findByEmail(email);
            verify(passwordEncoder, times(1)).matches(password, user.getPassword());
            verify(refreshTokenRedisService, times(1)).save(email, "refresh-token");
        }

        @Test
        @DisplayName("존재하지 않는 사용자인 경우 예외가 발생한다")
        void login_fail_userNotFound() {
            // given
            String email = "nonexistent@sangmyung.kr";
            LoginServiceRequest request = LoginServiceRequest.builder()
                    .email(email)
                    .password("password123")
                    .build();

            given(userRepository.findByEmail(email)).willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> userAuthService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 사용자입니다.");

            verify(userRepository, times(1)).findByEmail(email);
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않는 경우 예외가 발생한다")
        void login_fail_passwordMismatch() {
            // given
            String email = "test@sangmyung.kr";
            String password = "wrong-password";
            LoginServiceRequest request = LoginServiceRequest.builder()
                    .email(email)
                    .password(password)
                    .build();

            User user = new User();
            user.setEmail(email);
            user.setPassword("encoded-password");

            given(userRepository.findByEmail(email)).willReturn(java.util.Optional.of(user));
            given(passwordEncoder.matches(password, user.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userAuthService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호가 올바르지 않습니다.");

            verify(userRepository, times(1)).findByEmail(email);
            verify(passwordEncoder, times(1)).matches(password, user.getPassword());
        }

        @Test
        @DisplayName("이메일 인증이 완료되지 않은 계정인 경우 예외가 발생한다")
        void login_fail_emailNotVerified() {
            // given
            String email = "test@sangmyung.kr";
            String password = "password123";
            LoginServiceRequest request = LoginServiceRequest.builder()
                    .email(email)
                    .password(password)
                    .build();

            User user = new User();
            user.setEmail(email);
            user.setPassword("encoded-password");
            user.setEmailVerified(false);

            given(userRepository.findByEmail(email)).willReturn(java.util.Optional.of(user));
            given(passwordEncoder.matches(password, user.getPassword())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userAuthService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이메일 인증이 완료되지 않은 계정입니다.");

            verify(userRepository, times(1)).findByEmail(email);
            verify(passwordEncoder, times(1)).matches(password, user.getPassword());
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 테스트")
    class ResetPassword {

        @Test
        @DisplayName("새 비밀번호가 일치하고 인증코드가 유효하면 비밀번호를 재설정한다")
        void resetPassword_success() {
            // given
            String email = "test@sangmyung.kr";
            String code = "123456";
            String newPassword = "newPassword123";
            ResetPasswordServiceRequest request = ResetPasswordServiceRequest.builder()
                    .email(email)
                    .verificationCode(code)
                    .newPassword(newPassword)
                    .newPasswordConfirm(newPassword)
                    .build();

            User user = new User();
            user.setEmail(email);

            given(emailVerificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(email, code)).willReturn(true);
            given(userRepository.findByEmail(email)).willReturn(java.util.Optional.of(user));
            given(passwordEncoder.encode(newPassword)).willReturn("encoded-new-password");

            // when
            userAuthService.resetPassword(request);

            // then
            assertThat(user.getPassword()).isEqualTo("encoded-new-password");
            verify(emailVerificationRepository, times(1)).existsByEmailAndCodeAndIsVerifiedTrue(email, code);
            verify(userRepository, times(1)).findByEmail(email);
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("새 비밀번호와 확인이 일치하지 않으면 예외가 발생한다")
        void resetPassword_fail_passwordMismatch() {
            // given
            ResetPasswordServiceRequest request = ResetPasswordServiceRequest.builder()
                    .newPassword("password123")
                    .newPasswordConfirm("different")
                    .build();

            // when & then
            assertThatThrownBy(() -> userAuthService.resetPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("새 비밀번호가 일치하지 않습니다.");

            verify(emailVerificationRepository, never()).existsByEmailAndCodeAndIsVerifiedTrue(anyString(), anyString());
        }

        @Test
        @DisplayName("인증코드가 유효하지 않으면 예외가 발생한다")
        void resetPassword_fail_invalidCode() {
            // given
            String email = "test@sangmyung.kr";
            String code = "invalid";
            ResetPasswordServiceRequest request = ResetPasswordServiceRequest.builder()
                    .email(email)
                    .verificationCode(code)
                    .newPassword("password123")
                    .newPasswordConfirm("password123")
                    .build();

            given(emailVerificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(email, code)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userAuthService.resetPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증 코드 성공 여부가 존재하지 않습니다.");

            verify(emailVerificationRepository, times(1)).existsByEmailAndCodeAndIsVerifiedTrue(email, code);
            verify(userRepository, never()).findByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 코드 전송 테스트")
    class SendPasswordResetCode {

        @Test
        @DisplayName("존재하는 사용자인 경우 재설정 코드를 전송한다")
        void sendPasswordResetCode_success() {
            // given
            String email = "test@sangmyung.kr";
            given(userRepository.existsByEmail(email)).willReturn(true);
            given(emailService.sendPasswordResetCode(email)).willReturn("123456");

            // when
            userAuthService.sendPasswordResetCode(email);

            // then
            verify(userRepository, times(1)).existsByEmail(email);
            verify(emailVerificationRepository, times(1)).expireAllPendingVerifications(email);
            verify(emailService, times(1)).sendPasswordResetCode(email);
            verify(emailVerificationRepository, times(1)).save(any(EmailVerification.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자인 경우 예외가 발생한다")
        void sendPasswordResetCode_fail_userNotFound() {
            // given
            String email = "nonexistent@sangmyung.kr";
            given(userRepository.existsByEmail(email)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userAuthService.sendPasswordResetCode(email))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 사용자입니다.");

            verify(userRepository, times(1)).existsByEmail(email);
            verify(emailService, never()).sendPasswordResetCode(anyString());
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class Logout {

        @Test
        @DisplayName("로그아웃 시 Redis에서 리프레시 토큰을 삭제하고 액세스 토큰을 블랙리스트에 추가한다")
        void logout_with_tokens_success() {
            // given
            String refreshToken = "refresh-token";
            String accessToken = null; // JWT 파싱 복잡성을 피하기 위해 null로 테스트 (또는 MockedStatic 필요)

            // when
            userAuthService.logout(refreshToken, accessToken);

            // then
            verify(refreshTokenRedisService, times(1)).logout(eq(refreshToken), eq(accessToken), anyLong());
        }

        @Test
        @DisplayName("이메일만 전달하여 로그아웃할 경우 해당 값을 리프레시 토큰으로 간주하여 처리한다")
        void logout_with_email_success() {
            // given
            String email = "test@sangmyung.kr";

            // when
            userAuthService.logout(email);

            // then
            verify(refreshTokenRedisService, times(1)).logout(eq(email), eq(null), anyLong());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class Reissue {

        @Test
        @DisplayName("유효한 리프레시 토큰인 경우 토큰을 재발급한다")
        void reissue_success() {
            // given
            String refreshToken = "valid-refresh-token";
            TokenResponse tokenResponse = TokenResponse.of("new-access-token", "new-refresh-token");
            given(tokenProvider.reissueToken(eq(refreshToken), any())).willReturn(tokenResponse);
            given(refreshTokenRedisService.getRefreshTTL(tokenResponse.refreshToken())).willReturn(3600L);

            // when
            UserReissueServiceResponse response = userAuthService.reissue(refreshToken);

            // then
            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.ttl()).isEqualTo(3600L);

            verify(tokenProvider, times(1)).reissueToken(eq(refreshToken), any());
            verify(refreshTokenRedisService, times(1)).getRefreshTTL(tokenResponse.refreshToken());
        }

        @Test
        @DisplayName("리프레시 토큰이 null인 경우 예외가 발생한다")
        void reissue_fail_nullToken() {
            // when & then
            assertThatThrownBy(() -> userAuthService.reissue(null))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            verify(tokenProvider, never()).reissueToken(anyString(), any());
        }
    }
}
