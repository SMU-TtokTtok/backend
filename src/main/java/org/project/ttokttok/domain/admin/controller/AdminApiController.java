package org.project.ttokttok.domain.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.admin.controller.dto.request.AdminLoginRequest;
import org.project.ttokttok.domain.admin.controller.dto.response.AdminLoginResponse;
import org.project.ttokttok.domain.admin.service.AdminService;
import org.project.ttokttok.global.jwt.TokenProperties;
import org.project.ttokttok.global.jwt.dto.TokenResponse;
import org.project.ttokttok.global.util.CookieUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.project.ttokttok.global.jwt.TokenProperties.AUTH_HEADER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminApiController {

    private final AdminService adminService;
    private final CookieUtil cookieUtil;

    @PostMapping("/auth/login")
    public ResponseEntity<Void> login(@RequestBody @Valid AdminLoginRequest request) {
        AdminLoginResponse response = AdminLoginResponse.from(adminService.login(request.toServiceRequest()));
        ResponseCookie refreshCookie = CookieUtil.createRefreshTokenCookie(response.refreshToken());

        return ResponseEntity.ok()
                .header(AUTH_HEADER.getValue(), response.accessToken())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }
}