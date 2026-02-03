package org.project.ttokttok.domain.admin.controller;

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
import org.project.ttokttok.domain.admin.controller.dto.request.AdminLoginRequest;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.admin.service.AdminAuthService;
import org.project.ttokttok.domain.admin.service.dto.request.AdminJoinServiceRequest;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
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
    private AdminRepository adminRepository;

    @Autowired
    private ClubRepository clubRepository;

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
                    .andExpect(jsonPath("$.message").isString());
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 로그인하면 404 Not Found가 반환된다")
        void loginFailWithUserNotFound() throws Exception {
            // given

            // when

            // then
        }

        @ParameterizedTest
        @CsvSource({
                "admin, validPassword1234",
                "validadmin, wrongpw"
        })
        @DisplayName("id 또는 비밀번호가 너무 짧으면 400 Bad Request가 반환된다")
        void loginFailWithShortValue(final String username, final String rawPassword) throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("username과 password가 누락되면 400 Bad Request가 반환된다")
        void loginFailWithMissingFields() throws Exception {
            // given

            // when

            // then
        }
    }

    // ===== 2. logout 테스트 =====
    @Nested
    @DisplayName("POST /api/admin/auth/logout")
    class LogoutTest {

        @Test
        @DisplayName("Redis에 있는 리프레시 토큰을 삭제하고 로그아웃에 성공한다")
        void logoutSuccess() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("이미 로그아웃했거나 토큰이 존재하지 않을 경우 409 Conflict가 반환된다")
        void logoutFailWithTokenNotFound() throws Exception {
            // given

            // when

            // then
        }
    }

    // ===== 3. reissue 테스트 =====
    @Nested
    @DisplayName("POST /api/admin/auth/re-issue")
    class ReissueTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 액세스 토큰과 리프레시 토큰을 재발급받는다")
        void reissueSuccess() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("리프레시 토큰이 존재하지 않거나 만료된 경우 404 Not Found가 반환된다")
        void reissueFailWithTokenNotFoundOrExpired() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("리프레시 토큰이 없을 경우 400 Bad Request가 반환된다")
        void reissueFailWithMissingRefreshToken() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("Redis에 저장된 리프레시 토큰과 요청된 리프레시 토큰이 다를 경우 401 Unauthorized가 반환된다")
        void reissueFailWithInvalidRefreshToken() throws Exception {
            // given

            // when

            // then
        }
    }

    // ===== 4. join 테스트 =====
    @Nested
    @DisplayName("POST /api/admin/auth/join")
    class JoinTest {

        @Test
        @DisplayName("유효한 정보로 회원가입하면 201 OK와 관리자 ID가 반환된다")
        void joinSuccess() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("이미 존재하는 사용자명으로 회원가입하면 409 Conflict가 반환된다")
        void joinFailWithDuplicateUsername() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("username이 누락되면 400 Bad Request가 반환된다")
        void joinFailWithMissingUsername() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("password가 누락되면 400 Bad Request가 반환된다")
        void joinFailWithMissingPassword() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("clubName이 누락되면 400 Bad Request가 반환된다")
        void joinFailWithMissingClubName() throws Exception {
            // given

            // when

            // then
        }
    }

    // ===== 5. getAdminInfo 테스트 =====
    @Nested
    @DisplayName("GET /api/admin/auth/info")
    class GetAdminInfoTest {

        @Test
        @DisplayName("인증된 관리자가 자신의 정보를 조회하면 200 OK와 클럽 정보가 반환된다")
        void getAdminInfoSuccess() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("인증되지 않은 요청이면 401 Unauthorized가 반환된다")
        void getAdminInfoFailWithUnauthenticated() throws Exception {
            // given

            // when

            // then
        }
    }

    // ===== 6. resetPassword 테스트 =====
    @Nested
    @DisplayName("POST /api/admin/auth/reset-password")
    class ResetPasswordTest {

        @Test
        @DisplayName("새 비밀번호와 확인 비밀번호가 일치하면 200 OK가 반환된다")
        void resetPasswordSuccess() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("새 비밀번호와 확인 비밀번호가 일치하지 않으면 400 Bad Request가 반환된다")
        void resetPasswordFailWithMismatchedPasswords() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("존재하지 않는 사용자명으로 비밀번호 재설정을 시도하면 404 Not Found가 반환된다")
        void resetPasswordFailWithUserNotFound() throws Exception {
            // given

            // when

            // then
        }

        @Test
        @DisplayName("필수 필드가 누락되면 400 Bad Request가 반환된다")
        void resetPasswordFailWithMissingFields() throws Exception {
            // given

            // when

            // then
        }
    }
}
