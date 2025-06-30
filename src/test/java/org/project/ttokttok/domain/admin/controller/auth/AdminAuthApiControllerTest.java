package org.project.ttokttok.domain.admin.controller.auth;

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
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.jwt.JwtFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

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
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JwtFactory jwtFactory;

    private static final String LOGIN_ENDPOINT = "/api/admin/auth/login";
    private static final String LOGOUT_ENDPOINT = "/api/admin/auth/logout";

    @BeforeEach
    void clearRedisBeforeEach() {

        Set<String> keys = redisTemplate.keys("refresh:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @DisplayName("login(): 올바른 아이디와 비밀번호로 로그인하면 성공한다.")
    @Test
    void loginSuccess() throws Exception {
        // given
        final String username = "admin1234";
        final String password = "testpasswordover12";
        createAdmin(username, password);

        AdminLoginRequest request = new AdminLoginRequest(username, password);
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(cookie().exists("ttref"))
                .andExpect(jsonPath("$").value("Admin Login Success"));
    }

    @DisplayName("login(): 잘못된 비밀번호로 로그인하면 401 Unauthorized가 반환된다.")
    @Test
    void loginFail_InvalidPassword() throws Exception {
        // given
        final String username = "admin1234";
        final String correctPassword = "testpasswordover12";
        final String wrongPassword = "wrongpassword12";

        createAdmin(username, correctPassword);

        AdminLoginRequest request = new AdminLoginRequest(username, wrongPassword);
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.details").exists());
    }

    @DisplayName("login(): 존재하지 않는 사용자로 로그인하면 404 Not Found가 반환된다.")
    @Test
    void loginFail_UserNotFound() throws Exception {
        // given
        AdminLoginRequest request = new AdminLoginRequest("nonexistent", "somepassword1234");
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.details").exists());
    }

    @DisplayName("login(): id, 비밀번호가 너무 짧으면 400 Bad Request가 반환된다.")
    @ParameterizedTest
    @CsvSource({"admin, validPassword1234", "validadmin, wrongpw"})
    void loginFail_shortValue(String username, String rawPassword) throws Exception {
        // given
        createAdmin(username, rawPassword);

        AdminLoginRequest request = new AdminLoginRequest(username, rawPassword);
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("login(): username과 password가 누락되면 400 Bad Request가 반환된다.")
    @Test
    void loginFail_MissingFields() throws Exception {
        // given
        AdminLoginRequest request = new AdminLoginRequest(null, null);
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("logout(): Redis에 있는 리프레시 토큰을 삭제하고 로그아웃에 성공한다.")
    @Test
    void logoutSuccess() throws Exception {
        // given
        final String username = "adminlogout";
        final String password = "logoutpassword123";
        createAdmin(username, password);

        // 로그인하여 토큰 및 쿠키 발급
        AdminLoginRequest request = new AdminLoginRequest(username, password);
        String requestBody = objectMapper.writeValueAsString(request);

        var loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = "Bearer " + loginResult.getResponse().getHeader("Authorization");
        String refreshCookie = loginResult.getResponse().getCookie("ttref").getValue();

        // when & then
        mockMvc.perform(post(LOGOUT_ENDPOINT)
                        .header("Authorization", accessToken)
                        .cookie(new Cookie("ttref", refreshCookie)))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));

        // Redis에서 리프레시 토큰이 삭제되었는지 확인
        String redisKey = "refresh:" + username;
        assertNull(redisTemplate.opsForValue().get(redisKey));
    }

    @DisplayName("logout(): 이미 로그아웃했거나 토큰이 존재하지 않을 경우 409 Conflict가 반환된다.")
    @Test
    void logoutFail_TokenNotFound() throws Exception {
        // given
        final String username = "adminlogoutfail";
        final String password = "logoutfailpassword123";
        createAdmin(username, password);

        // 로그인하여 토큰 및 쿠키 발급
        AdminLoginRequest request = new AdminLoginRequest(username, password);
        String requestBody = objectMapper.writeValueAsString(request);

        var loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = "Bearer " + loginResult.getResponse().getHeader("Authorization");
        String refreshCookie = loginResult.getResponse().getCookie("ttref").getValue();

        // Redis에서 리프레시 토큰 삭제
        String redisKey = "refresh:" + username;
        redisTemplate.delete(redisKey);

        // when & then
        mockMvc.perform(post(LOGOUT_ENDPOINT)
                        .header("Authorization", accessToken)
                        .cookie(new Cookie("ttref", refreshCookie)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.details").exists());
    }

    // ======= 유틸 메서드 =======
    private void createAdmin(String username, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        Admin admin = Admin.adminJoin(username, encodedPassword);

        adminRepository.save(admin);
    }
}