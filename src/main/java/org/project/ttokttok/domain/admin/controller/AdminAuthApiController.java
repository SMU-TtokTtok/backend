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

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AdminAuthApiController implements AdminAuthDocs {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody @Valid AdminLoginRequest request) {
        AdminLoginServiceResponse response = adminAuthService.login(request.toServiceRequest());

        return ResponseEntity.ok()
                .body(
                        AdminLoginResponse.of(
                                response.clubId(),
                                response.clubName(),
                                response.accessToken(),
                                response.refreshToken())
                );
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthUserInfo String adminName) {
        adminAuthService.logout(adminName);

        return ResponseEntity.ok()
                .body(Map.of("message", "로그아웃이 완료되었습니다."));
    }

    @PostMapping("/re-issue")
    public ResponseEntity<Map<String, Object>> reissue(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Bearer 토큰에서 refresh token 추출
        String refreshToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            refreshToken = authHeader.substring(7);
        }

        ReissueServiceResponse response = adminAuthService.reissue(refreshToken);

        return ResponseEntity.ok()
                .body(Map.of(
                        "message", "토큰이 재발급되었습니다.",
                        "accessToken", response.accessToken(),
                        "refreshToken", response.refreshToken()
                ));
    }

    // todo: 추후 삭제 - 관리자 가입 메서드
    @PostMapping("/join")
    public ResponseEntity<AdminJoinResponse> join(@RequestBody @Valid AdminJoinRequest request) {
        String adminId = adminAuthService.join(
                request.toServiceRequest()
        );

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