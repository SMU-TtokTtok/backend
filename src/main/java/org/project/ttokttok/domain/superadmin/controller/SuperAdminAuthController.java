package org.project.ttokttok.domain.superadmin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.superadmin.controller.docs.SuperAdminAuthDocs;
import org.project.ttokttok.domain.superadmin.controller.dto.request.SuperAdminLoginRequest;
import org.project.ttokttok.domain.superadmin.controller.dto.response.SuperAdminLoginResponse;
import org.project.ttokttok.domain.superadmin.service.SuperAdminAuthService;
import org.project.ttokttok.domain.superadmin.service.dto.response.SuperAdminLoginServiceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자 인증 API 컨트롤러
 * 서비스 운영/유지보수 팀 계정의 로그인 및 ROLE_SUPER_ADMIN 토큰 발급을 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/super-admin")
public class SuperAdminAuthController implements SuperAdminAuthDocs {

    private final SuperAdminAuthService superAdminAuthService;

    @PostMapping("/login")
    public ResponseEntity<SuperAdminLoginResponse> login(@RequestBody @Valid SuperAdminLoginRequest request) {
        SuperAdminLoginServiceResponse response = superAdminAuthService.login(request.toServiceRequest());

        return ResponseEntity.ok()
                .body(SuperAdminLoginResponse.of(response.accessToken(), response.refreshToken()));
    }
}
