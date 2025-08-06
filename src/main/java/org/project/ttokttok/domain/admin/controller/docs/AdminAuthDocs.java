package org.project.ttokttok.domain.admin.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.admin.controller.dto.request.AdminLoginRequest;
import org.project.ttokttok.domain.admin.controller.dto.response.AdminLoginResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@Tag(name = "[관리자] 관리자 인증 API", description = "관리자 인증 / 인가 관련 API 입니다.")
public interface AdminAuthDocs {

    @Operation(
            summary = "관리자 로그인",
            description = """
                    관리자 계정으로 로그인합니다.
                    성공 시 액세스 토큰과 리프레시 토큰이 쿠키로 설정됩니다.
                    응답 본문에 동아리 ID와 이름이 포함됩니다.
                    
                    *주의사항*
                    - 관리자 계정은 최소 8글자 입력입니다.
                    - 비밀번호는 최소 12글자 입력입니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AdminLoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (아이디/비밀번호 형식 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (잘못된 이메일 또는 비밀번호)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "관리자를 찾을 수 없음."
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<AdminLoginResponse> login(
            @Parameter(description = "관리자 로그인 요청 (아이디, 비밀번호)")
            AdminLoginRequest request
    );


    @Operation(
            summary = "관리자 로그아웃",
            description = """
                    관리자 계정에서 로그아웃합니다.
                    토큰 쿠키가 만료 처리됩니다.
                    Redis 에 저장된 토큰 정보도 삭제됩니다.
                    
                    주의 사항
                    - 이미 로그아웃 되어있다면 409 응답을 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 로그아웃 상태인 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Map<String, String>> logout(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String adminName
    );

    @Operation(
            summary = "토큰 재발급",
            description = """
                    Authorization 헤더의 리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급받습니다.
                    Redis에 저장된 리프레시 토큰과 비교하여 유효성을 검증합니다.
                    토큰이 만료된 경우, 다시 로그인을 시도해야 합니다.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "refreshTokenAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 리프레시 토큰",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "만료된 리프레시 토큰",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Map<String, Object>> reissue(
            @Parameter(description = "Authorization 헤더의 리프레시 토큰")
            @RequestHeader(value = "Authorization", required = false) String authHeader
    );
}
