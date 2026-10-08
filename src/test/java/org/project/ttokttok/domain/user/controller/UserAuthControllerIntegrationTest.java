package org.project.ttokttok.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.project.ttokttok.domain.user.domain.EmailVerification;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.repository.EmailVerificationRepository;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:controllerAuthentication;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAuthControllerIntegrationTest {
    private static final String CODE = "123456";
    private static final String PASSWORD = "NewPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmailVerificationRepository verificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;
    @MockitoBean private S3Service s3Service;

    private String email;

    @BeforeEach
    void setUp() {
        email = UUID.randomUUID() + "@sangmyung.kr";
    }

    @AfterEach
    void cleanUpOwnFixtures() {
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            entityManager.createQuery("delete from EmailVerification e where e.email = :email")
                    .setParameter("email", email).executeUpdate();
            entityManager.createQuery("delete from User u where u.email = :email")
                    .setParameter("email", email).executeUpdate();
        });
    }

    @Test
    void sendVerificationCode_commitsNewPendingCode() throws Exception {
        given(emailService.isValidSangmyungEmail(email)).willReturn(true);
        given(emailService.sendVerificationCode(email)).willReturn(CODE);
        LocalDateTime before = LocalDateTime.now();

        perform("send-verification", Map.of("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("인증코드가 발송되었습니다."));

        EmailVerification saved = verificationRepository.findByEmailAndCodeAndIsVerifiedFalse(email, CODE).orElseThrow();
        assertThat(saved.getExpiresAt()).isBetween(before.plusMinutes(5), LocalDateTime.now().plusMinutes(5));
    }

    @Test
    void verifyEmail_commitsDirtyCheckingWithoutTestTransaction() throws Exception {
        EmailVerification pending = saveVerification(false, LocalDateTime.now().plusMinutes(5));

        perform("verify-email", Map.of("email", email, "code", CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."));

        assertThat(verificationRepository.findById(pending.getId()).orElseThrow().isVerified()).isTrue();
    }

    @Test
    void expiredVerification_returnsOriginalErrorAndRemainsPending() throws Exception {
        EmailVerification expired = saveVerification(false, LocalDateTime.now().minusMinutes(1));

        perform("verify-email", Map.of("email", email, "code", CODE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("인증코드가 만료되었습니다."));

        assertThat(verificationRepository.findById(expired.getId()).orElseThrow().isVerified()).isFalse();
    }

    @Test
    void unknownVerification_returnsOriginalError() throws Exception {
        perform("verify-email", Map.of("email", email, "code", CODE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("올바르지 않은 인증코드입니다."));
    }

    @Test
    void signupWithoutVerification_doesNotCreateUser() throws Exception {
        perform("signup", signupBody())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("이메일 인증이 완료되지 않았습니다."));

        assertThat(userRepository.existsByEmail(email)).isFalse();
        verifyNoInteractions(emailService);
    }

    @Test
    void signupWithVerification_commitsEncodedPassword() throws Exception {
        saveVerification(true, LocalDateTime.now().plusMinutes(5));

        perform("signup", signupBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.email").value(email));

        assertThat(passwordEncoder.matches(PASSWORD, userRepository.findByEmail(email).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void resetPassword_commitsEncodedPasswordWithoutTestTransaction() throws Exception {
        saveLocalUser();
        saveVerification(true, LocalDateTime.now().plusMinutes(5));

        perform("reset-password", resetBody(PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 재설정되었습니다."));

        assertThat(passwordEncoder.matches(PASSWORD, userRepository.findByEmail(email).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void mismatchedPassword_keepsPasswordAndOriginalError() throws Exception {
        User user = saveLocalUser();

        perform("reset-password", resetBody("different"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("새 비밀번호가 일치하지 않습니다."));

        assertThat(userRepository.findByEmail(email).orElseThrow().getPassword()).isEqualTo(user.getPassword());
        verifyNoInteractions(emailService);
    }

    @Test
    void missingVerifiedCode_keepsPasswordAndOriginalError() throws Exception {
        User user = saveLocalUser();

        perform("reset-password", resetBody(PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("인증 코드 성공 여부가 존재하지 않습니다."));

        assertThat(userRepository.findByEmail(email).orElseThrow().getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    void sendResetCode_commitsCodeForExistingPasswordAccount() throws Exception {
        saveLocalUser();
        given(emailService.sendPasswordResetCode(email)).willReturn(CODE);

        perform("send-reset-code", Map.of("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호 재설정 코드가 발송되었습니다."));

        assertThat(verificationRepository.findByEmailAndCodeAndIsVerifiedFalse(email, CODE)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mailFailure_rollsBackPendingCodeUpdate(boolean passwordReset) throws Exception {
        EmailVerification pending = saveVerification(false, LocalDateTime.now().plusMinutes(5));
        RuntimeException failure = new RuntimeException("SMTP unavailable");
        if (passwordReset) {
            saveLocalUser();
            given(emailService.sendPasswordResetCode(email)).willThrow(failure);
        } else {
            given(emailService.isValidSangmyungEmail(email)).willReturn(true);
            given(emailService.sendVerificationCode(email)).willThrow(failure);
        }

        perform(passwordReset ? "send-reset-code" : "send-verification", Map.of("email", email))
                .andExpect(status().isInternalServerError());

        assertThat(verificationRepository.findById(pending.getId()).orElseThrow().isVerified()).isFalse();
        assertThat(verificationRepository.findByEmailAndCodeAndIsVerifiedFalse(email, CODE).orElseThrow().getId())
                .isEqualTo(pending.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"send-reset-code", "reset-password"})
    void oauthAccount_isRejectedBeforeMailOrPasswordMutation(String endpoint) throws Exception {
        userRepository.save(User.signUpWithGoogle(email, "OAuth 사용자", UUID.randomUUID().toString()));
        saveVerification(true, LocalDateTime.now().plusMinutes(5));

        perform(endpoint, endpoint.equals("reset-password") ? resetBody(PASSWORD) : Map.of("email", email))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").value(ErrorMessage.OAUTH_ONLY_ACCOUNT.getMessage()));

        assertThat(userRepository.findByEmail(email).orElseThrow().getPassword()).isNull();
        verifyNoInteractions(emailService);
    }

    private EmailVerification saveVerification(boolean verified, LocalDateTime expiresAt) {
        EmailVerification verification = EmailVerification.builder().email(email).code(CODE).expiresAt(expiresAt).build();
        if (verified) {
            verification.markAsVerified();
        }
        return verificationRepository.save(verification);
    }

    private User saveLocalUser() {
        return userRepository.save(User.signUp(email, passwordEncoder.encode("OldPassword123!"), "테스터", true));
    }

    private Map<String, Object> signupBody() {
        return Map.of("email", email, "verificationCode", CODE, "password", PASSWORD,
                "passwordConfirm", PASSWORD, "name", "테스터", "termsAgreed", true);
    }

    private Map<String, Object> resetBody(String confirmation) {
        return Map.of("email", email, "verificationCode", CODE, "newPassword", PASSWORD, "newPasswordConfirm", confirmation);
    }

    private ResultActions perform(String endpoint, Map<String, ?> body) throws Exception {
        return mockMvc.perform(post("/api/user/auth/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }
}
