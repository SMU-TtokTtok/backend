package org.project.ttokttok.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "사용자 인증", description = "회원가입, 로그인, 이메일 인증 등 사용자 인증 관련 API")
@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    /**
     * 이메일 인증코드 발송
     * */
    @Operation(
            summary = "이메일 인증코드 발송",
            description = "상명대학교 이메일로 6자리 인증코드를 발송합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인증코드 발송 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "상명대 이메일이 아니거나 형식 오류"
            )
    })
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
    @Operation(
            summary = "이메일 인증코드 검증",
            description = "발송된 6자리 인증코드를 검증합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인증 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 인증코드 또는 만료"
            )
    })
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
    @Operation(
            summary = "회원가입",
            description = "이메일 인증 완료 후 회원가입을 진행합니다. 강력한 비밀번호 필요."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이미 가입된 이메일 또는 유효성 검사 실패"
            )
    })
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
    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인합니다. 로그인 유지 옵션 선택 시 리프레시 토큰이 쿠키로 설정됩니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공, 액세스 토큰 헤더로 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 이메일 또는 비밀번호"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자"
            )
    })
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
    @Operation(
            summary = "비밀번호 재설정 코드 발송",
            description = "가입된 이메일로 6자리 비밀번호 재설정 코드를 발송합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 재설정 코드 발송 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 이메일 형식"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자"
            )
    })
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
    @Operation(
            summary = "비밀번호 재설정",
            description = "발송된 재설정 코드와 새 비밀번호로 비밀번호를 재설정합니다. 강력한 비밀번호 필요."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 재설정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 재설정 코드 또는 비밀번호 형식 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자"
            )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {

        userAuthService.resetPassword(request.toServiceRequest());

        return ResponseEntity.ok(
                ApiResponse.success("비밀번호가 재설정되었습니다.")
        );
    }
}
