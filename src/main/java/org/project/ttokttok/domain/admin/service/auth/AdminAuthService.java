package org.project.ttokttok.domain.admin.service.auth;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.exception.AdminNotFoundException;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.admin.service.dto.request.AdminLoginServiceRequest;
import org.project.ttokttok.domain.admin.service.dto.response.AdminLoginServiceResponse;
import org.project.ttokttok.global.jwt.dto.TokenResponse;
import org.project.ttokttok.global.jwt.service.RefreshTokenRedisService;
import org.project.ttokttok.global.jwt.service.TokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRedisService refreshTokenRedisService;

    public AdminLoginServiceResponse login(AdminLoginServiceRequest request) {
        Admin targetAdmin = adminRepository.findByUsername(request.username())
                .orElseThrow(AdminNotFoundException::new);

        targetAdmin.validatePassword(request.password(), passwordEncoder);

        TokenResponse tokenResponse = getTokenResponse(targetAdmin.getId(), targetAdmin.getUsername());

        return AdminLoginServiceResponse.from(tokenResponse);
    }

    private TokenResponse getTokenResponse(String id, String username) {
        TokenResponse tokenResponse = tokenProvider.generateToken(id, username);
        refreshTokenRedisService.save(id, tokenResponse.refreshToken());

        return tokenResponse;
    }
}
