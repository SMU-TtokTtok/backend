package org.project.ttokttok.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.user.controller.dto.request.*;
import org.project.ttokttok.domain.user.controller.dto.response.ApiResponse;
import org.project.ttokttok.domain.user.controller.dto.response.LoginResponse;
import org.project.ttokttok.domain.user.controller.dto.response.UserResponse;
import org.project.ttokttok.domain.user.service.UserAuthService;
import org.project.ttokttok.domain.user.service.dto.response.LoginServiceResponse;
import org.project.ttokttok.domain.user.service.dto.response.UserServiceResponse;
import org.project.ttokttok.global.util.CookieUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.*;

import static org.project.ttokttok.global.auth.jwt.TokenExpiry.REFRESH_TOKEN_EXPIRY_TIME;
import static org.project.ttokttok.global.auth.jwt.TokenProperties.AUTH_HEADER;
import static org.project.ttokttok.global.auth.jwt.TokenProperties.REFRESH_KEY;


import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    /**
     * 이메일 인증코드 발송
     * */
    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @RequestBody @Valid SendVerificationRequest request) {

        userAuthService.sendVerificationCode(request.email());

        return ResponseEntity.ok(
                ApiResponse.success("인증코드가 발송되었습니다.")
        );
    }

    /**
     * 이메일 인증코드 검증
     * */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody @Valid VerifyEmailRequest request) {

        userAuthService.verifyEmail(request.email(), request.code());

        return ResponseEntity.ok(
                ApiResponse.success("이메일 인증이 완료되었습니다.")
        );
    }

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(
            @RequestBody @Valid SignupRequest request) {

        UserServiceResponse serviceResponse = userAuthService.signup(request.toServiceRequest());
        UserResponse userResponse = UserResponse.from(serviceResponse);

        return ResponseEntity.ok(
                ApiResponse.success("회원가입이 완료되었습니다.", userResponse)
        );
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request) {

        LoginServiceResponse serviceResponse = userAuthService.login(request.toServiceRequest());
        LoginResponse loginResponse = LoginResponse.from(serviceResponse);

        // 리프레시 토큰을 쿠키로 설정 (로그인 유지 옵션이 true인 경우에만)
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .header(AUTH_HEADER.getValue(), "Bearer " + serviceResponse.accessToken());

        if (request.rememberMe()) {
            ResponseCookie refreshCookie = CookieUtil.createResponseCookie(
                    REFRESH_KEY.getValue(),
                    serviceResponse.refreshToken(),
                    Duration.ofMillis(REFRESH_TOKEN_EXPIRY_TIME.getExpiry())
            );
            responseBuilder.header(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        return responseBuilder.body(
                ApiResponse.success("로그인 성공", loginResponse)
        );
    }

    /**
     * 비밀번호 재설정 코드 발송
     */
    @PostMapping("/send-reset-code")
    public ResponseEntity<ApiResponse<Void>> sendPasswordResetCode(
            @RequestBody @Valid SendVerificationRequest request) {

        userAuthService.sendPasswordResetCode(request.email());

        return ResponseEntity.ok(
                ApiResponse.success("비밀번호 재설정 코드가 발송되었습니다.")
        );
    }

    /**
     * 비밀번호 재설정
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {

        userAuthService.resetPassword(request.toServiceRequest());

        return ResponseEntity.ok(
                ApiResponse.success("비밀번호가 재설정되었습니다.")
        );
    }
}
