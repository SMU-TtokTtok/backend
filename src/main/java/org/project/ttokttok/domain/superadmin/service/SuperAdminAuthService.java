package org.project.ttokttok.domain.superadmin.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.superadmin.domain.SuperAdmin;
import org.project.ttokttok.domain.superadmin.exception.SuperAdminNotFoundException;
import org.project.ttokttok.domain.superadmin.repository.SuperAdminRepository;
import org.project.ttokttok.domain.superadmin.service.dto.request.SuperAdminLoginServiceRequest;
import org.project.ttokttok.domain.superadmin.service.dto.response.SuperAdminLoginServiceResponse;
import org.project.ttokttok.global.auth.jwt.dto.request.TokenRequest;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static org.project.ttokttok.global.entity.Role.ROLE_SUPER_ADMIN;

@Service
@RequiredArgsConstructor
public class SuperAdminAuthService {

    private final PasswordEncoder passwordEncoder;
    private final SuperAdminRepository superAdminRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRedisService refreshTokenRedisService;

    // 운영자 로그인 -> ROLE_SUPER_ADMIN 토큰 발급
    public SuperAdminLoginServiceResponse login(SuperAdminLoginServiceRequest request) {
        SuperAdmin superAdmin = superAdminRepository.findByUsername(request.username())
                .orElseThrow(SuperAdminNotFoundException::new);

        superAdmin.validatePassword(request.password(), passwordEncoder);

        TokenResponse tokenResponse = tokenProvider.generateToken(
                TokenRequest.of(superAdmin.getUsername(), ROLE_SUPER_ADMIN)
        );

        refreshTokenRedisService.save(superAdmin.getUsername(), tokenResponse.refreshToken());

        return SuperAdminLoginServiceResponse.of(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken()
        );
    }
}
