package org.project.ttokttok.domain.applyform.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.applyform.controller.dto.response.ActiveApplyFormResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "[사용자] 지원폼 API", description = "사용자용 지원폼 조회 및 제출 관련 API 입니다.")
public interface ApplyFormUserDocs {

    @Operation(
            summary = "활성화된 지원폼 조회",
            description = """
                    특정 동아리의 활성화된 지원폼을 조회합니다.
                    지원폼의 기본 정보와 질문 목록을 반환합니다.
                    
                    *주의사항*
                    - 활성화된 지원폼이 없는 경우 404 에러가 발생합니다.
                    - clubId는 유효한 동아리 ID여야 합니다.
                    - 반환되는 질문들은 JSONB 형태로 저장된 데이터입니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "활성화된 지원폼 조회 성공",
                    content = @Content(schema = @Schema(implementation = ActiveApplyFormResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효하지 않은 clubId 형식)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "동아리를 찾을 수 없거나 활성화된 지원폼이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ActiveApplyFormResponse> getActiveApplyForm(
            @Parameter(description = "동아리 ID", example = "UUID")
            String clubId
    );
}
