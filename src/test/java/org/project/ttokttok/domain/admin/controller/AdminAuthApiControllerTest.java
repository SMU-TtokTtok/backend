package org.project.ttokttok.domain.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.project.ttokttok.domain.admin.controller.dto.request.AdminJoinRequest;
import org.project.ttokttok.domain.admin.controller.dto.request.AdminLoginRequest;
import org.project.ttokttok.domain.admin.controller.dto.request.AdminResetPasswordRequest;
import org.project.ttokttok.domain.admin.service.AdminAuthService;
import org.project.ttokttok.domain.admin.service.dto.request.AdminJoinServiceRequest;
import org.project.ttokttok.domain.admin.service.dto.request.AdminLoginServiceRequest;
import org.project.ttokttok.domain.admin.service.dto.response.AdminLoginServiceResponse;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthApiControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // ===== 엔드포인트 상수 =====
    private static final String LOGIN_ENDPOINT = "/api/admin/auth/login";
    private static final String LOGOUT_ENDPOINT = "/api/admin/auth/logout";
    private static final String REISSUE_ENDPOINT = "/api/admin/auth/re-issue";
    private static final String JOIN_ENDPOINT = "/api/admin/auth/join";
    private static final String INFO_ENDPOINT = "/api/admin/auth/info";
    private static final String RESET_PASSWORD_ENDPOINT = "/api/admin/auth/reset-password";

    // ===== 테스트 데이터 상수 =====
    private static final String VALID_USERNAME = "admin1234";
    private static final String VALID_PASSWORD = "testpasswordover12";

    @BeforeEach
    void clearRedisBeforeEach() {
        final var keys = redisTemplate.keys("refresh:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ===== 1. login 테스트 =====
    @Nested
    @DisplayName("POST /api/admin/auth/login")
    class LoginTest {

        @BeforeEach
        void setupAdmin() {
            // 테스트용 임의 객체 생성
            AdminJoinServiceRequest request = new AdminJoinServiceRequest(
                    VALID_USERNAME,
                    VALID_PASSWORD,
                    "Test Club",
                    ClubUniv.ENGINEERING
            );

            adminAuthService.join(request);
        }

        @Test
        @DisplayName("올바른 아이디와 비밀번호로 로그인하면 성공한다")
        void loginSuccess() throws Exception {
            // given
            AdminLoginRequest loginRequest = new AdminLoginRequest(
                    VALID_USERNAME,
                    VALID_PASSWORD
            );
            final String requestJson = objectMapper.writeValueAsString(loginRequest);

            // when
            final ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clubId").isString())
                    .andExpect(jsonPath("$.clubName").value("Test Club"))
                    .andExpect(jsonPath("$.accessToken").isString())
                    .andExpect(jsonPath("$.refreshToken").isString());
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인하면 401 Unauthorized가 반환된다")
        void loginFailWithInvalidPassword() throws Exception {
            // given
            final String wrongPassword = "wrongPassword123";
            AdminLoginRequest loginRequest = new AdminLoginRequest(
                    VALID_USERNAME,
                    wrongPassword
            );
            final String requestJson = objectMapper.writeValueAsString(loginRequest);

            // when
            final ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.details").isString());
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 로그인하면 404 Not Found가 반환된다")
        void loginFailWithUserNotFound() throws Exception {
            // given
            final String nonExistentUsername = "nonexistentadmin";
            final AdminLoginRequest loginRequest = new AdminLoginRequest(
                    nonExistentUsername,
                    VALID_PASSWORD
            );
            final String requestJson = objectMapper.writeValueAsString(loginRequest);

            // when
            final ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.details").isString());
        }

        @ParameterizedTest
        @CsvSource({
                "admin, validPassword1234",
                "validadmin, wrongpw"
        })
        @DisplayName("id 또는 비밀번호가 너무 짧으면 400 Bad Request가 반환된다")
        void loginFailWithShortValue(final String username, final String rawPassword) throws Exception {
            // given
            final AdminLoginRequest loginRequest = new AdminLoginRequest(
                    username,
                    rawPassword
            );
            final String requestJson = objectMapper.writeValueAsString(loginRequest);

            // when
            final ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400));
        }

        @Test
        @DisplayName("username과 password가 누락되면 400 Bad Request가 반환된다")
        void loginFailWithMissingFields() throws Exception {
            // given
            final String emptyRequestJson = "{}";

            // when
            final ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .content(emptyRequestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400));
        }
    }

    // ===== 2. logout 테스트 =====
    @Nested
    @DisplayName("POST /api/admin/auth/logout")
    class LogoutTest {

        private String accessToken;

        @BeforeEach
        void setupAdminAndLogin() {
            final AdminJoinServiceRequest joinRequest = new AdminJoinServiceRequest(
                    VALID_USERNAME,
                    VALID_PASSWORD,
                    "Test Club",
                    ClubUniv.ENGINEERING
            );
            adminAuthService.join(joinRequest);

            final AdminLoginServiceResponse loginResponse = adminAuthService.login(
                    new AdminLoginServiceRequest(
                            VALID_USERNAME,
                            VALID_PASSWORD
                    )
            );
            accessToken = loginResponse.accessToken();
        }

        @Test
        @DisplayName("Redis에 있는 리프레시 토큰을 삭제하고 로그아웃에 성공한다")
        void logoutSuccess() throws Exception {
            // given - BeforeEach에서 설정됨

            // when
            final ResultActions result = mockMvc.perform(post(LOGOUT_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."));
        }

        @Test
        @DisplayName("이미 로그아웃했거나 토큰이 존재하지 않아도 200 OK가 반환된다")
        void logoutSuccessEvenWhenAlreadyLoggedOut() throws Exception {
            // given
            adminAuthService.logout(VALID_USERNAME);

            // when - 이미 로그아웃된 상태에서 다시 로그아웃 시도
            final ResultActions result = mockMvc.perform(post(LOGOUT_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON));

            // then - 현재 구현은 이미 로그아웃 상태에서도 OK 반환
            result
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."));
        }
    }

    // ===== 3. reissue 테스트 =====
    @Nested
    @DisplayName("POST /api/admin/auth/re-issue")
    class ReissueTest {

        private String refreshToken;

        @BeforeEach
        void setupAdminAndLogin() {
            final AdminJoinServiceRequest joinRequest = new AdminJoinServiceRequest(
                    VALID_USERNAME,
                    VALID_PASSWORD,
                    "Test Club",
                    ClubUniv.ENGINEERING
            );
            adminAuthService.join(joinRequest);

            final AdminLoginServiceResponse loginResponse = adminAuthService.login(
                    new AdminLoginServiceRequest(
                            VALID_USERNAME,
                            VALID_PASSWORD
                    )
            );
            refreshToken = loginResponse.refreshToken();
        }

        @Test
        @DisplayName("유효한 리프레시 토큰으로 액세스 토큰과 리프레시 토큰을 재발급받는다")
        void reissueSuccess() throws Exception {
            // given - BeforeEach에서 설정됨

            // when
            final ResultActions result = mockMvc.perform(post(REISSUE_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."))
                    .andExpect(jsonPath("$.accessToken").isString())
                    .andExpect(jsonPath("$.refreshToken").isString());
        }

        @Test
        @DisplayName("리프레시 토큰이 존재하지 않거나 만료된 경우 404 Not Found가 반환된다")
        void reissueFailWithTokenNotFoundOrExpired() throws Exception {
            // given
            final String invalidRefreshToken = "invalid.refresh.token";

            // when
            final ResultActions result = mockMvc.perform(post(REISSUE_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidRefreshToken)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404));
        }

        @Test
        @DisplayName("리프레시 토큰이 없을 경우 400 Bad Request가 반환된다")
        void reissueFailWithMissingRefreshToken() throws Exception {
            // given - 헤더 없이 요청

            // when
            final ResultActions result = mockMvc.perform(post(REISSUE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400));
        }

        @Test
        @DisplayName("Redis에 저장된 리프레시 토큰과 요청된 리프레시 토큰이 다를 경우 404 Not Found가 반환된다")
        void reissueFailWithInvalidRefreshToken() throws Exception {
            // given
            final String tamperedRefreshToken = refreshToken + "tampered";

            // when
            final ResultActions result = mockMvc.perform(post(REISSUE_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedRefreshToken)
                    .contentType(MediaType.APPLICATION_JSON));

            // then - 위조된 토큰은 Redis에서 찾을 수 없으므로 404 반환
            result
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404));
        }
    }

    // ===== 4. join 테스트 =====
    @Nested
    @DisplayName("POST /api/admin/auth/join")
    class JoinTest {

        @Test
        @DisplayName("유효한 정보로 회원가입하면 200 OK와 관리자 ID가 반환된다")
        void joinSuccess() throws Exception {
            // given
            final AdminJoinRequest joinRequest = new AdminJoinRequest(
                    VALID_USERNAME,
                    VALID_PASSWORD,
                    "Test Club",
                    ClubUniv.ENGINEERING
            );
            final String requestJson = objectMapper.writeValueAsString(joinRequest);

            // when
            final ResultActions result = mockMvc.perform(post(JOIN_ENDPOINT)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.adminId").isString());
        }

        @Test
        @DisplayName("이미 존재하는 사용자명으로 회원가입하면 409 Conflict가 반환된다")
        void joinFailWithDuplicateUsername() throws Exception {
            // given
            final AdminJoinServiceRequest preJoinRequest = new AdminJoinServiceRequest(
                    VALID_USERNAME,
                    VALID_PASSWORD,
                    "Test Club",
                    ClubUniv.ENGINEERING
            );
            adminAuthService.join(preJoinRequest);

            final AdminJoinRequest joinRequest = new AdminJoinRequest(
                    VALID_USERNAME,
                    VALID_PASSWORD,
                    "Another Club",
                    ClubUniv.ARTS
            );
            final String requestJson = objectMapper.writeValueAsString(joinRequest);

            // when
            final ResultActions result = mockMvc.perform(post(JOIN_ENDPOINT)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409));
        }

        @ParameterizedTest(name = "{0}이(가) 누락되면 400 Bad Request가 반환된다")
        @CsvSource(delimiter = '|', textBlock = """
                username  | {"password": "testpasswordover12", "clubName": "Test Club", "clubUniv": "ENGINEERING"}
                password  | {"username": "admin1234", "clubName": "Test Club", "clubUniv": "ENGINEERING"}
                clubName  | {"username": "admin1234", "password": "testpasswordover12", "clubUniv": "ENGINEERING"}
                clubUniv  | {"username": "admin1234", "password": "testpasswordover12", "clubName": "Test Club"}
            """)
        @DisplayName("필수 필드가 누락되면 400 Bad Request가 반환된다")
        void joinFailWithMissingRequiredField(final String missingField, final String requestJson) throws Exception {
            // when
            final ResultActions result = mockMvc.perform(post(JOIN_ENDPOINT)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400));
        }
    }

    // ===== 5. getAdminInfo 테스트 =====
    @Nested
    @DisplayName("GET /api/admin/auth/info")
    class GetAdminInfoTest {

        private String accessToken;

        @BeforeEach
        void setupAdminAndLogin() {
            final AdminJoinServiceRequest joinRequest = new AdminJoinServiceRequest(
                    VALID_USERNAME,
                    VALID_PASSWORD,
                    "Test Club",
                    ClubUniv.ENGINEERING
            );
            adminAuthService.join(joinRequest);

            final AdminLoginServiceResponse loginResponse = adminAuthService.login(
                    new AdminLoginServiceRequest(
                            VALID_USERNAME,
                            VALID_PASSWORD
                    )
            );
            accessToken = loginResponse.accessToken();
        }

        @Test
        @DisplayName("인증된 관리자가 자신의 정보를 조회하면 200 OK와 클럽 정보가 반환된다")
        void getAdminInfoSuccess() throws Exception {
            // given - BeforeEach에서 설정됨

            // when
            final ResultActions result = mockMvc.perform(get(INFO_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clubId").isString())
                    .andExpect(jsonPath("$.clubName").value("Test Club"));
        }

        @Test
        @DisplayName("인증되지 않은 요청이면 401 Unauthorized가 반환된다")
        void getAdminInfoFailWithUnauthenticated() throws Exception {
            // given - 토큰 없이 요청

            // when
            final ResultActions result = mockMvc.perform(get(INFO_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Expired Token Or Need Authentication."));
        }
    }

    // ===== 6. resetPassword 테스트 =====
    @Nested
    @DisplayName("POST /api/admin/auth/reset-password")
    class ResetPasswordTest {

        @BeforeEach
        void setupAdmin() {
            final AdminJoinServiceRequest joinRequest = new AdminJoinServiceRequest(
                    VALID_USERNAME,
                    VALID_PASSWORD,
                    "Test Club",
                    ClubUniv.ENGINEERING
            );
            adminAuthService.join(joinRequest);
        }

        @Test
        @DisplayName("새 비밀번호와 확인 비밀번호가 일치하면 200 OK가 반환된다")
        void resetPasswordSuccess() throws Exception {
            // given
            final String newPassword = "newpassword1234";
            final AdminResetPasswordRequest resetRequest = new AdminResetPasswordRequest(
                    VALID_USERNAME,
                    newPassword,
                    newPassword
            );
            final String requestJson = objectMapper.writeValueAsString(resetRequest);

            // when
            final ResultActions result = mockMvc.perform(post(RESET_PASSWORD_ENDPOINT)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다."));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', textBlock = """
                비밀번호 불일치     | {"username": "admin1234", "newPassword": "newpassword1234", "newPasswordConfirm": "differentpassword"}
                username 누락     | {"newPassword": "newpassword1234", "newPasswordConfirm": "newpassword1234"}
                newPassword 누락  | {"username": "admin1234", "newPasswordConfirm": "newpassword1234"}
                newPasswordConfirm 누락 | {"username": "admin1234", "newPassword": "newpassword1234"}
            """)
        @DisplayName("유효하지 않은 요청이면 400 Bad Request가 반환된다")
        void resetPasswordFailWithInvalidRequest(final String testCase, final String requestJson) throws Exception {
            // when
            final ResultActions result = mockMvc.perform(post(RESET_PASSWORD_ENDPOINT)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400));
        }

        @Test
        @DisplayName("존재하지 않는 사용자명으로 비밀번호 재설정을 시도하면 404 Not Found가 반환된다")
        void resetPasswordFailWithUserNotFound() throws Exception {
            // given
            final String newPassword = "newpassword1234";
            final AdminResetPasswordRequest resetRequest = new AdminResetPasswordRequest(
                    "nonexistentadmin",
                    newPassword,
                    newPassword
            );
            final String requestJson = objectMapper.writeValueAsString(resetRequest);

            // when
            final ResultActions result = mockMvc.perform(post(RESET_PASSWORD_ENDPOINT)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404));
        }
    }
}
