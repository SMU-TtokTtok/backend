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
import java.util.Optional;

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
            
            User user = User.signUp(request.email(), "encoded-password", request.name(), true);
            given(userRepository.save(any(User.class))).willReturn(user);

            // when
            UserServiceResponse response = userAuthService.signup(request);

            // then
            assertThat(response.email()).isEqualTo(request.email());
            assertThat(response.name()).isEqualTo(request.name());
            
            verify(userRepository, times(1)).existsByEmail(request.email());
            verify(userRepository, times(1)).save(any(User.class));
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

            User user = User.signUp(email, "encoded-password", "테스터", true);
            TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token");

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(password, user.getPassword())).willReturn(true);
            given(tokenProvider.generateToken(any())).willReturn(tokenResponse);

            // when
            LoginServiceResponse response = userAuthService.login(request);

            // then
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");

            verify(userRepository, times(1)).findByEmail(email);
            verify(refreshTokenRedisService, times(1)).save(email, "refresh-token");
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

            User user = User.signUp(email, "old-password", "홍길동", true);

            given(emailVerificationRepository.existsByEmailAndCodeAndIsVerifiedTrue(email, code)).willReturn(true);
            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(passwordEncoder.encode(newPassword)).willReturn("encoded-new-password");

            // when
            userAuthService.resetPassword(request);

            // then
            assertThat(user.getPassword()).isEqualTo("encoded-new-password");
            verify(userRepository, times(1)).save(user);
        }
    }
}
