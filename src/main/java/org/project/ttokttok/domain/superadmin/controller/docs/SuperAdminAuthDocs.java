package org.project.ttokttok.domain.superadmin.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.superadmin.controller.dto.request.SuperAdminLoginRequest;
import org.project.ttokttok.domain.superadmin.controller.dto.response.SuperAdminLoginResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "[운영자] 운영자 인증 API", description = "서비스 운영/유지보수 팀(ROLE_SUPER_ADMIN) 인증 API 입니다.")
public interface SuperAdminAuthDocs {

    @Operation(
            summary = "운영자 로그인",
            description = """
                    운영자 계정으로 로그인합니다.
                    성공 시 ROLE_SUPER_ADMIN 권한을 담은 액세스 토큰과 리프레시 토큰을 반환합니다.
                    이 액세스 토큰으로 공지사항 저장 API(/api/super-admin/notices)를 호출할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = SuperAdminLoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (아이디/비밀번호 누락)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "비밀번호 불일치"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "운영자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<SuperAdminLoginResponse> login(
            @Parameter(description = "운영자 로그인 요청 (아이디, 비밀번호)") SuperAdminLoginRequest request
    );
}
