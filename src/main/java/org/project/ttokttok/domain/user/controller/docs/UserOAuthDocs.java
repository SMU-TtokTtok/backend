package org.project.ttokttok.domain.user.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.project.ttokttok.domain.user.controller.dto.request.GoogleLoginRequest;
import org.project.ttokttok.domain.user.controller.dto.request.GoogleOnboardingCompleteRequest;
import org.project.ttokttok.domain.user.controller.dto.response.ApiResponse;
import org.project.ttokttok.domain.user.controller.dto.response.GoogleLoginResponse;
import org.project.ttokttok.domain.user.controller.dto.response.LoginResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "[사용자] 구글 OAuth 인증", description = "구글 ID 토큰 기반 로그인 및 온보딩(약관 동의) API")
public interface UserOAuthDocs {

    @Operation(
            summary = "구글 로그인",
            description = """
                    프론트엔드가 Google Identity Services 로 발급받은 구글 ID 토큰을 검증하고 로그인을 처리합니다.

                    - 기존 사용자(또는 이메일 일치로 자동 연동된 사용자): needsOnboarding=false + 액세스/리프레시 토큰 반환
                    - 신규 사용자: needsOnboarding=true + 온보딩 토큰(10분 유효) 반환 → 약관 동의 후 완료 API 호출 필요
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 또는 온보딩 필요 (needsOnboarding 필드로 구분)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "구글 계정의 이메일이 검증되지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 구글 ID 토큰 (서명/만료/발급자/대상 불일치)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 다른 구글 계정과 연동된 이메일"
            )
    })
    ResponseEntity<ApiResponse<GoogleLoginResponse>> googleLogin(@Valid GoogleLoginRequest request);

    @Operation(
            summary = "구글 온보딩 완료 (약관 동의 후 가입)",
            description = """
                    구글 로그인 응답으로 받은 온보딩 토큰과 약관 동의, 이름을 받아 회원가입을 완료하고 토큰을 발급합니다.

                    - 온보딩 토큰은 10분간 유효하며 일회성입니다 (재요청 시 이미 가입된 경우 정상 로그인 처리)
                    - 만료 시 구글 로그인부터 다시 시작해야 합니다
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "가입 완료 및 로그인 성공 (기존 로그인 응답과 동일)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "약관 미동의 또는 이름 유효성 검사 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "온보딩 토큰이 만료되었거나 유효하지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 처리된 가입 요청 (계정 없음 - 이상 상태)"
            )
    })
    ResponseEntity<ApiResponse<LoginResponse>> completeOnboarding(@Valid GoogleOnboardingCompleteRequest request);
}
