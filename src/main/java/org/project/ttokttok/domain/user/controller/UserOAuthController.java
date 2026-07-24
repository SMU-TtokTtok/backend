package org.project.ttokttok.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.user.controller.docs.UserOAuthDocs;
import org.project.ttokttok.domain.user.controller.dto.request.GoogleLoginRequest;
import org.project.ttokttok.domain.user.controller.dto.request.GoogleOnboardingCompleteRequest;
import org.project.ttokttok.domain.user.controller.dto.response.ApiResponse;
import org.project.ttokttok.domain.user.controller.dto.response.GoogleLoginResponse;
import org.project.ttokttok.domain.user.controller.dto.response.LoginResponse;
import org.project.ttokttok.domain.user.service.GoogleOAuthService;
import org.project.ttokttok.domain.user.service.dto.response.GoogleLoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user/auth/oauth")
@RequiredArgsConstructor
public class UserOAuthController implements UserOAuthDocs {

    private final GoogleOAuthService googleOAuthService;

    /**
     * 구글 로그인 API
     * 구글 ID 토큰을 검증하고 로그인(기존/자동연동) 또는 온보딩 안내(신규)를 처리합니다.
     */
    @Override
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<GoogleLoginResponse>> googleLogin(
            @RequestBody @Valid GoogleLoginRequest request) {

        GoogleLoginServiceResponse serviceResponse = googleOAuthService.login(request.idToken());

        String message = serviceResponse.needsOnboarding()
                ? "약관 동의가 필요합니다."
                : "로그인에 성공했습니다.";

        return ResponseEntity.ok(
                ApiResponse.success(message, GoogleLoginResponse.from(serviceResponse))
        );
    }

    /**
     * 구글 온보딩 완료 API
     * 약관 동의 및 이름 입력을 받아 회원가입을 완료하고 토큰을 발급합니다.
     */
    @Override
    @PostMapping("/google/complete")
    public ResponseEntity<ApiResponse<LoginResponse>> completeOnboarding(
            @RequestBody @Valid GoogleOnboardingCompleteRequest request) {

        LoginServiceResponse serviceResponse = googleOAuthService.completeOnboarding(request.toServiceRequest());

        return ResponseEntity.ok(
                ApiResponse.success("회원가입 및 로그인에 성공했습니다.", LoginResponse.from(serviceResponse))
        );
    }
}
