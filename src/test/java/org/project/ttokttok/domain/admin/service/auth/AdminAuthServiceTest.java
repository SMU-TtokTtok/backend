package org.project.ttokttok.domain.admin.service.auth;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.project.ttokttok.global.jwt.service.TokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@ActiveProfiles("test")
class AdminAuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RefreshTokenRedisService refreshTokenRedisService;

    @InjectMocks
    private AdminAuthService adminAuthService;

    // 로그인 성공

    // 존재하지 않는 admin

    // 틀린 비밀번호
}