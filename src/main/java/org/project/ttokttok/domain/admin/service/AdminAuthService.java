package org.project.ttokttok.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.exception.AdminNotFoundException;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.admin.service.dto.request.AdminLoginServiceRequest;
import org.project.ttokttok.domain.admin.service.dto.response.AdminLoginServiceResponse;
import org.project.ttokttok.domain.admin.service.dto.response.ReissueServiceResponse;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.global.auth.jwt.dto.request.TokenRequest;
import org.project.ttokttok.global.auth.jwt.dto.response.TokenResponse;
import org.project.ttokttok.global.auth.jwt.exception.InvalidRefreshTokenException;
import org.project.ttokttok.global.auth.jwt.exception.InvalidTokenFromCookieException;
import org.project.ttokttok.infrastructure.redis.service.RefreshTokenRedisService;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.project.ttokttok.global.entity.Role.ROLE_ADMIN;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;
    private final ClubRepository clubRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRedisService refreshTokenRedisService;

    public AdminLoginServiceResponse login(AdminLoginServiceRequest request) {
        Admin targetAdmin = adminRepository.findByUsername(request.username())
                .orElseThrow(AdminNotFoundException::new);

        targetAdmin.validatePassword(request.password(), passwordEncoder);

        TokenResponse tokenResponse = getTokenResponse(targetAdmin.getUsername());

        Club findClub = clubRepository.findByAdminUsername(targetAdmin.getUsername())
                .orElseThrow(AdminNotFoundException::new);

        return AdminLoginServiceResponse.of(
                tokenResponse, findClub.getId(), findClub.getName()
        );
    }

    public void logout(String username) {
        refreshTokenRedisService.deleteRefreshToken(username);
    }

    @Transactional
    public ReissueServiceResponse reissue(String username, String refreshToken) {
        reissueValidate(username, refreshToken);

        TokenResponse tokens = tokenProvider.reissueToken(username, ROLE_ADMIN);
        Long ttl = refreshTokenRedisService.updateRefreshToken(username, tokens.refreshToken());

        return ReissueServiceResponse.of(tokens, ttl);
    }

    // FIXME: 관리자 가입 메서드, 추후 삭제 예정
    public String join(String username, String password) {
        Admin admin = Admin.adminJoin(username, passwordEncoder.encode(password));

        return adminRepository.save(admin)
                .getId();
    }

    private void reissueValidate(String username, String refreshToken) {
        validateTokenFromCookie(refreshToken);
        isRefreshSame(username, refreshToken);
    }

    private void isRefreshSame(String username, String refreshToken) {
        String targetRefresh = refreshTokenRedisService.getRefreshToken(username);

        if (!refreshToken.equals(targetRefresh)) {
            throw new InvalidRefreshTokenException();
        }
    }

    private void validateTokenFromCookie(String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidTokenFromCookieException();
        }
    }

    private TokenResponse getTokenResponse(String username) {
        TokenResponse tokenResponse = tokenProvider.generateToken(TokenRequest.of(username, ROLE_ADMIN));
        refreshTokenRedisService.save(username, tokenResponse.refreshToken());

        return tokenResponse;
    }
}
