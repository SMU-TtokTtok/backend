package org.project.ttokttok.domain.temp.applicant.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempApplicantSaveRequest;
import org.project.ttokttok.domain.temp.applicant.controller.dto.response.TempApplicantSaveResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "[사용자] 임시 지원서 API", description = "지원서 임시 저장 관리용 API 입니다.")
public interface TempApplicantDocs {

    @Operation(
            summary = "임시 지원서 저장",
            description = """
                    지원서 작성 중 임시 저장을 수행합니다.
                    사용자가 지원서를 작성하는 과정에서 임시로 데이터를 저장할 때 사용됩니다.
                    
                    *주의사항*
                    - 사용자 인증이 필요합니다.
                    - 지원폼 ID는 필수 입력 사항입니다.
                    - 임시 데이터(tempData)는 지원서 작성 중인 내용을 JSON 형태로 저장합니다.
                    - 임시 저장된 데이터는 나중에 정식 지원서로 제출할 수 있습니다.
                    - 임시 데이터 필드들은 작성되지 않은 필드는 null을 허용합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "임시 지원서 저장 성공",
                    content = @Content(schema = @Schema(implementation = TempApplicantSaveResponse.class))
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
                    responseCode = "404",
                    description = "지원폼을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<TempApplicantSaveResponse> saveTempApplicant(
            @Parameter(description = "인증된 사용자 이메일", hidden = true)
            @AuthUserInfo String userEmail,

            @Parameter(description = "임시 지원서 저장 요청 데이터",
                    content = @Content(schema = @Schema(implementation = TempApplicantSaveRequest.class)))
            @Valid @RequestBody TempApplicantSaveRequest request
    );
}
