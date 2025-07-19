package org.project.ttokttok.domain.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.project.ttokttok.domain.admin.controller.dto.request.AdminLoginRequest;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthApiControllerTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String LOGIN_ENDPOINT = "/api/admin/auth/login";
    private static final String LOGOUT_ENDPOINT = "/api/admin/auth/logout";
    private static final String REISSUE_ENDPOINT = "/api/admin/auth/re-issue";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "ttac";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "ttref";

    @BeforeEach
    void clearRedisBeforeEach() {
        var keys = redisTemplate.keys("refresh:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("login(): 올바른 아이디와 비밀번호로 로그인하면 성공한다.")
    void loginSuccess() throws Exception {
        String username = "admin1234";
        String password = "testpasswordover12";
        createAdmin(username, password);

        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest(username, password));

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(ACCESS_TOKEN_COOKIE_NAME))
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(jsonPath("$.clubId").exists())
                .andExpect(jsonPath("$.clubName").exists());
    }

    @Test
    @DisplayName("login(): 잘못된 비밀번호로 로그인하면 401 Unauthorized가 반환된다.")
    void loginFail_InvalidPassword() throws Exception {
        String username = "admin1234";
        String correctPassword = "testpasswordover12";
        String wrongPassword = "wrongpassword12";
        createAdmin(username, correctPassword);

        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest(username, wrongPassword));

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @DisplayName("login(): 존재하지 않는 사용자로 로그인하면 404 Not Found가 반환된다.")
    void loginFail_UserNotFound() throws Exception {
        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest("nonexistent", "somepassword1234"));

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.details").exists());
    }

    @ParameterizedTest
    @CsvSource(
            {"admin, validPassword1234", "validadmin, wrongpw"}
    )
    @DisplayName("login(): id, 비밀번호가 너무 짧으면 400 Bad Request가 반환된다.")
    void loginFail_shortValue(String username, String rawPassword) throws Exception {
        createAdmin(username, rawPassword);

        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest(username, rawPassword));

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login(): username과 password가 누락되면 400 Bad Request가 반환된다.")
    void loginFail_MissingFields() throws Exception {
        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest(null, null));

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("logout(): Redis에 있는 리프레시 토큰을 삭제하고 로그아웃에 성공한다.")
    void logoutSuccess() throws Exception {
        String username = "adminlogout";
        String password = "logoutpassword123";
        createAdmin(username, password);

        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest(username, password));

        var loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie(ACCESS_TOKEN_COOKIE_NAME);
        Cookie refreshCookie = loginResult.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                        .cookie(accessCookie)
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists(ACCESS_TOKEN_COOKIE_NAME))
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().maxAge(ACCESS_TOKEN_COOKIE_NAME, 0))  // 쿠키가 만료되었는지 확인
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0));

        String redisKey = "refresh:" + username;
        assertNull(redisTemplate.opsForValue().get(redisKey));
    }

    @Test
    @DisplayName("logout(): 이미 로그아웃했거나 토큰이 존재하지 않을 경우 409 Conflict가 반환된다.")
    void logoutFail_TokenNotFound() throws Exception {
        String username = "adminlogoutfail";
        String password = "logoutfailpassword123";
        createAdmin(username, password);

        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest(username, password));

        var loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie(ACCESS_TOKEN_COOKIE_NAME);
        Cookie refreshCookie = loginResult.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);

        String redisKey = "refresh:" + username;
        redisTemplate.delete(redisKey);

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                        .cookie(accessCookie)
                        .cookie(refreshCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @DisplayName("reissue(): 유효한 리프레시 토큰으로 액세스 토큰과 리프레시 토큰을 재발급받는다.")
    void reissueSuccess() throws Exception {
        String username = "adminreissue";
        String password = "reissuepassword123";
        createAdmin(username, password);

        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest(username, password));

        var loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie(ACCESS_TOKEN_COOKIE_NAME);
        Cookie refreshCookie = loginResult.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);

        mockMvc.perform(post(REISSUE_ENDPOINT)
                        .cookie(accessCookie)
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(ACCESS_TOKEN_COOKIE_NAME))
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(jsonPath("$").value("re-issue Success"));
    }

    @Test
    @DisplayName("reissue(): 리프레시 토큰이 존재하지 않거나 만료된 경우 404 Not Found가 반환된다.")
    void reissueFail_TokenNotFoundOrExpired() throws Exception {
        String username = "adminreissuefail";
        String password = "reissuefailpassword123";
        createAdmin(username, password);

        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest(username, password));

        var loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie(ACCESS_TOKEN_COOKIE_NAME);
        Cookie refreshCookie = loginResult.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);

        String redisKey = "refresh:" + username;
        redisTemplate.delete(redisKey);

        mockMvc.perform(post(REISSUE_ENDPOINT)
                        .cookie(accessCookie)
                        .cookie(refreshCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @DisplayName("reissue(): 쿠키에 리프레시 토큰이 없을 경우 reissueValidate에서 예외가 발생하면 400 Bad Request가 반환된다.")
    void reissueFail_InvalidTokenFromCookie() throws Exception {
        String username = "admin1234";
        String password = "validpassword123";
        createAdmin(username, password);

        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest(username, password));

        var loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie(ACCESS_TOKEN_COOKIE_NAME);
        // 리프레시 토큰 쿠키는 제공하지 않음

        mockMvc.perform(post(REISSUE_ENDPOINT)
                        .cookie(accessCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @DisplayName("reissue(): Redis에 저장된 리프레시 토큰과 요청된 리프레시 토큰이 다를 경우 reissueValidate에서 예외가 발생하면 401 Unauthorized가 반환된다.")
    void reissueFail_InvalidRefreshToken() throws Exception {
        String username = "admin1234";
        String password = "validpassword123";
        createAdmin(username, password);

        var requestBody = objectMapper.writeValueAsString(new AdminLoginRequest(username, password));

        var loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie(ACCESS_TOKEN_COOKIE_NAME);
        Cookie refreshCookie = loginResult.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME);

        redisTemplate.opsForValue().set("refresh:" + username, "differentRefreshToken");

        mockMvc.perform(post(REISSUE_ENDPOINT)
                        .cookie(accessCookie)
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.details").exists());
    }

    private void createAdmin(String username, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        Admin admin = Admin.adminJoin(username, encodedPassword);
        admin = adminRepository.save(admin);

        // Club 생성 및 Admin과 연결
        Club club = Club.builder()
                .admin(admin)
                .build();
        clubRepository.save(club);
    }
}
