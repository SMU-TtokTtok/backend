package org.project.ttokttok.domain.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.admin.controller.docs.AdminAuthDocs;
import org.project.ttokttok.domain.admin.controller.dto.request.AdminJoinRequest;
import org.project.ttokttok.domain.admin.controller.dto.request.AdminLoginRequest;
import org.project.ttokttok.domain.admin.controller.dto.response.AdminJoinResponse;
import org.project.ttokttok.domain.admin.controller.dto.response.AdminLoginResponse;
import org.project.ttokttok.domain.admin.service.AdminAuthService;
import org.project.ttokttok.domain.admin.service.dto.response.AdminLoginServiceResponse;
import org.project.ttokttok.domain.admin.service.dto.response.ReissueServiceResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.project.ttokttok.global.util.CookieUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

import static org.project.ttokttok.global.auth.jwt.TokenExpiry.ACCESS_TOKEN_EXPIRY_TIME;
import static org.project.ttokttok.global.auth.jwt.TokenExpiry.REFRESH_TOKEN_EXPIRY_TIME;
import static org.project.ttokttok.global.auth.jwt.TokenProperties.ACCESS_TOKEN_COOKIE;
import static org.project.ttokttok.global.auth.jwt.TokenProperties.REFRESH_KEY;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AdminAuthApiController implements AdminAuthDocs {

    private final AdminAuthService adminAuthService;
    private final CookieUtil cookieUtil;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody @Valid AdminLoginRequest request) {
        AdminLoginServiceResponse response = adminAuthService.login(request.toServiceRequest());

        // 액세스 토큰 쿠키 생성
        ResponseCookie accessCookie = cookieUtil.createResponseCookie(
                ACCESS_TOKEN_COOKIE.getValue(),
                response.accessToken(),
                Duration.ofMillis(ACCESS_TOKEN_EXPIRY_TIME.getExpiry())
        );

        // 리프레시 토큰 쿠키 생성
        ResponseCookie refreshCookie = cookieUtil.createResponseCookie(
                REFRESH_KEY.getValue(),
                response.refreshToken(),
                Duration.ofMillis(REFRESH_TOKEN_EXPIRY_TIME.getExpiry())
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(
                        AdminLoginResponse.of(
                                response.clubId(),
                                response.clubName())
                );
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthUserInfo String adminName) {
        adminAuthService.logout(adminName);

        // 두 쿠키 모두 만료시키기
        ResponseCookie[] expiredCookies = cookieUtil.expireBothTokenCookies();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookies[0].toString())
                .header(HttpHeaders.SET_COOKIE, expiredCookies[1].toString())
                .body(Map.of("message", "로그아웃이 완료되었습니다."));
    }

    @PostMapping("/re-issue")
    public ResponseEntity<Map<String, String>> reissue(
            @AuthUserInfo String adminName,
            @CookieValue(value = "ttref", required = false) String refreshToken) {

        ReissueServiceResponse response = adminAuthService.reissue(adminName, refreshToken);

        // 새 액세스 토큰 쿠키 생성
        ResponseCookie accessCookie = cookieUtil.createResponseCookie(
                ACCESS_TOKEN_COOKIE.getValue(),
                response.accessToken(),
                Duration.ofMillis(ACCESS_TOKEN_EXPIRY_TIME.getExpiry())
        );

        // 새 리프레시 토큰 쿠키 생성 (남은 TTL 사용)
        ResponseCookie refreshCookie = cookieUtil.createResponseCookie(
                REFRESH_KEY.getValue(),
                response.refreshToken(),
                Duration.ofMillis(response.refreshTTL())
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "토큰이 재발급되었습니다."));
    }

    // todo: 추후 삭제 - 관리자 가입 메서드
    @PostMapping("/join")
    public ResponseEntity<AdminJoinResponse> join(@RequestBody @Valid AdminJoinRequest request) {
        String adminId = adminAuthService.join(request.username(), request.password());

        AdminJoinResponse response = AdminJoinResponse.of(adminId);

        return ResponseEntity.ok()
                .body(response);
    }

    // 프론트 테스트용
    @GetMapping("/info")
    public ResponseEntity<AdminLoginResponse> getAdminInfo(@AuthUserInfo String adminName) {
        return ResponseEntity.ok()
                .body(adminAuthService.getAdminInfo(adminName));
    }
}