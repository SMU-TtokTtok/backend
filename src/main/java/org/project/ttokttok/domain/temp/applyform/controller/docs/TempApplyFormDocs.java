package org.project.ttokttok.domain.temp.applyform.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.project.ttokttok.domain.temp.applyform.controller.dto.request.TempApplyFormSaveRequest;
import org.project.ttokttok.domain.temp.applyform.controller.dto.response.TempApplyFormSaveResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "[관리자] 임시 지원폼 API", description = "지원폼 임시 저장 관리용 API 입니다.")
public interface TempApplyFormDocs {

    @Operation(
            summary = "임시 지원폼 저장",
            description = """
                    지원폼 작성 중 임시 저장을 수행합니다.
                    관리자가 지원폼을 작성하는 과정에서 임시로 데이터를 저장할 때 사용됩니다.
                    
                    *주의사항*
                    - 관리자 권한이 필요합니다.
                    - 동아리 ID는 필수 입력 사항입니다.
                    - 다른 필드들은 임시저장을 위해 선택사항입니다.
                    - 임시 저장된 데이터는 나중에 정식 지원폼으로 변환할 수 있습니다.
                    - 질문 목록(formJson)은 작성되지 않은 필드는 null을 허용합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "임시 지원폼 저장 성공",
                    content = @Content(schema = @Schema(implementation = TempApplyFormSaveResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효하지 않은 요청 데이터)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "동아리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<TempApplyFormSaveResponse> saveTempApplyForm(
            @Parameter(description = "인증된 사용자 이메일", hidden = true)
            @AuthUserInfo String userEmail,

            @Parameter(description = "임시 지원폼 저장 요청 데이터",
                    content = @Content(schema = @Schema(implementation = TempApplyFormSaveRequest.class)))
            @Valid @RequestBody TempApplyFormSaveRequest request
    );
}
