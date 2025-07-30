package org.project.ttokttok.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.project.ttokttok.global.util.CookieUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.*;

import static org.project.ttokttok.global.auth.jwt.TokenExpiry.ACCESS_TOKEN_EXPIRY_TIME;
import static org.project.ttokttok.global.auth.jwt.TokenExpiry.REFRESH_TOKEN_EXPIRY_TIME;
import static org.project.ttokttok.global.auth.jwt.TokenProperties.REFRESH_KEY;
import static org.project.ttokttok.global.auth.jwt.TokenProperties.ACCESS_TOKEN_COOKIE;

import java.time.Duration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Tag(name = "사용자 인증", description = "회원가입, 로그인, 이메일 인증 등 사용자 인증 관련 API")
@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;
    private final CookieUtil cookieUtil;

    /**
     * 이메일 인증코드 발송 API
     * 상명대학교 이메일로 6자리 인증코드를 발송합니다.
     *
     * @param request 이메일 인증 요청 정보 (이메일 주소 포함)
     * @return 인증코드 발송 결과
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
            @Parameter(description = "이메일 인증 요청 (상명대 이메일 주소)")
            @RequestBody @Valid SendVerificationRequest request) {

        userAuthService.sendVerificationCode(request.email());

        return ResponseEntity.ok(
                ApiResponse.success("인증코드가 발송되었습니다.")
        );
    }

    /**
     * 이메일 인증코드 검증 API
     * 발송된 6자리 인증코드를 검증합니다.
     *
     * @param request 이메일 인증 검증 요청 정보 (이메일 주소, 인증코드 포함)
     * @return 인증 검증 결과
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
            @Parameter(description = "이메일 인증 검증 요청 (이메일 주소, 6자리 인증코드)")
            @RequestBody @Valid VerifyEmailRequest request) {

        userAuthService.verifyEmail(request.email(), request.code());

        return ResponseEntity.ok(
                ApiResponse.success("이메일 인증이 완료되었습니다.")
        );
    }

    /**
     * 회원가입 API
     * 이메일 인증 완료 후 회원가입을 진행합니다.
     *
     * @param request 회원가입 요청 정보 (이메일, 비밀번호, 닉네임 포함)
     * @return 회원가입 완료된 사용자 정보
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
            @Parameter(description = "회원가입 요청 (이메일, 비밀번호, 닉네임)")
            @RequestBody @Valid SignupRequest request) {

        UserServiceResponse serviceResponse = userAuthService.signup(request.toServiceRequest());
        UserResponse userResponse = UserResponse.from(serviceResponse);

        return ResponseEntity.ok(
                ApiResponse.success("회원가입이 완료되었습니다.", userResponse)
        );
    }

    /**
     * 로그인 API
     * 이메일과 비밀번호로 로그인을 진행합니다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호, 로그인 유지 여부 포함)
     * @return 로그인 결과 및 토큰 정보
     */
    @Operation(
            summary = "로그인",
            description = """
                    이메일과 비밀번호로 로그인합니다. 로그인 유지 옵션 선택 시 리프레시 토큰이 쿠키로 설정됩니다.
                    테스트 로그인 계정:
                    - 이메일: test@sangmyung.kr
                    - 비밀번호 : TestPass123!
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공, 액세스 토큰 쿠키로 반환"
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
            @Parameter(description = "로그인 요청 (이메일, 비밀번호, 로그인 유지 여부)")
            @RequestBody @Valid LoginRequest request) {

        LoginServiceResponse serviceResponse = userAuthService.login(request.toServiceRequest());
        LoginResponse loginResponse = LoginResponse.from(serviceResponse);

        // 액세스 토큰 쿠키 생성
        ResponseCookie accessCookie = cookieUtil.createResponseCookie(
                ACCESS_TOKEN_COOKIE.getValue(),
                serviceResponse.accessToken(),
                Duration.ofMillis(ACCESS_TOKEN_EXPIRY_TIME.getExpiry())
        );

        // 리프레시 토큰 쿠키 생성 (항상 설정)
        ResponseCookie refreshCookie = cookieUtil.createResponseCookie(
                REFRESH_KEY.getValue(),
                serviceResponse.refreshToken(),
                Duration.ofMillis(REFRESH_TOKEN_EXPIRY_TIME.getExpiry())
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(
                        ApiResponse.success("로그인 성공", loginResponse)
                );
    }

    /**
     * 비밀번호 재설정 코드 발송 API
     * 가입된 이메일로 6자리 비밀번호 재설정 코드를 발송합니다.
     *
     * @param request 비밀번호 재설정 코드 발송 요청 (이메일 주소 포함)
     * @return 재설정 코드 발송 결과
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
            @Parameter(description = "비밀번호 재설정 코드 발송 요청 (가입된 이메일 주소")
            @RequestBody @Valid SendVerificationRequest request) {

        userAuthService.sendPasswordResetCode(request.email());

        return ResponseEntity.ok(
                ApiResponse.success("비밀번호 재설정 코드가 발송되었습니다.")
        );
    }

    /**
     * 비밀번호 재설정 API
     * 발송된 재설정 코드와 새 비밀번호로 비밀번호를 재설정합니다.
     *
     * @param request 비밀번호 재설정 요청 (이메일, 재설정 코드, 새 비밀번호 포함)
     * @return 비밀번호 재설정 결과
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
            @Parameter(description = "비밀번호 재설정 요청 (이메일, 6자리 재설정 코드, 새 비밀번호)")
            @RequestBody @Valid ResetPasswordRequest request) {

        userAuthService.resetPassword(request.toServiceRequest());

        return ResponseEntity.ok(
                ApiResponse.success("비밀번호가 재설정되었습니다.")
        );
    }

    /**
     * 로그아웃 API
     * 사용자 로그아웃을 처리합니다. 토큰 쿠키가 만료 처리되고 Redis에 저장된 토큰 정보도 삭제됩니다.
     *
     * @param userEmail 인증된 사용자 이메일
     * @param request HTTP 요청 객체 (쿠키 추출용)
     * @return 로그아웃 결과
     */
    @Operation(
            summary = "사용자 로그아웃",
            description = """
                    사용자 계정에서 로그아웃합니다.
                    토큰 쿠키가 만료 처리됩니다.
                    Redis에 저장된 토큰 정보도 삭제됩니다.
                    액세스 토큰은 블랙리스트에 추가되어 즉시 무효화됩니다.
                    
                    주의 사항
                    - 이미 로그아웃 되어있다면 409 응답을 반환합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "로그아웃 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 로그아웃 상태인 계정"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류"
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthUserInfo String userEmail, HttpServletRequest request) {
        // 쿠키에서 액세스 토큰 추출
        String accessToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.getValue().equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    log.info("로그아웃 - 액세스 토큰 추출: {}", accessToken);
                    break;
                }
            }
        } else {
            log.warn("로그아웃 - 쿠키가 없습니다.");
        }

        userAuthService.logout(userEmail, accessToken);

        // 두 쿠키 모두 만료시키기
        ResponseCookie[] expiredCookies = cookieUtil.expireBothTokenCookies();
        log.info("로그아웃 - 쿠키 만료 설정: {}", expiredCookies[0].toString());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookies[0].toString())
                .header(HttpHeaders.SET_COOKIE, expiredCookies[1].toString())
                .build();
    }
}
