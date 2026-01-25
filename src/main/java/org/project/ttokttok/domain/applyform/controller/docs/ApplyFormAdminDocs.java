package org.project.ttokttok.domain.applyform.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.applyform.controller.dto.request.ApplyFormCreateRequest;
import org.project.ttokttok.domain.applyform.controller.dto.request.ApplyFormUpdateRequest;
import org.project.ttokttok.domain.applyform.controller.dto.response.ApplyFormCreateResponse;
import org.project.ttokttok.domain.applyform.controller.dto.response.ApplyFormDetailResponse;
import org.project.ttokttok.domain.applyform.controller.dto.response.BeforeQuestionsResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Tag(name = "[관리자] 지원폼 관리 API", description = "동아리 관리자용 지원폼 생성/수정/조회 API 입니다.")
public interface ApplyFormAdminDocs {

    @Operation(
            summary = "지원폼 상세 조회",
            description = """
                    동아리의 현재 활성화된 지원폼 정보를 상세 조회합니다.
                    관리자 권한이 필요하며, 지원폼 수정 시 참고할 수 있습니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 조회 가능합니다.
                    - 현재 활성화된 지원폼만 조회됩니다.
                    - 질문 목록과 설정 정보가 포함됩니다.
                    - 임시 지원폼이 존재할 경우, 일부 필드가 **null**로 내려갈 수 있습니다.
                    - 아무런 정보도 존재하지 않을 경우, formJson 필드는 빈 배열, 다른 필드는 null이 반환됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "지원폼 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApplyFormDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 동아리 또는 활성화된 지원폼이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ApplyFormDetailResponse> getApplyFormsByClubId(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            @AuthUserInfo String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            @PathVariable String clubId
    );

    @Operation(
            summary = "지원폼 생성",
            description = """
                    동아리 지원폼을 생성합니다.
                    관리자 권한이 필요하며, 모집 시작 전에 생성해야 합니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 생성 가능합니다.
                    - 하나의 동아리당 하나의 활성 지원폼만 가질 수 있습니다.
                    - 질문 형태는 JSON 배열로 구성됩니다.
                    - 모집 시작일과 종료일을 정확히 설정해야 합니다.
                    - 모집 마감일은 모집 시작일 이후여야 하며, 모집 시작일 이전으로 설정할 수 없습니다.
                    - 기존에 임시저장했던 임시 지원 폼이 존재한다면, 삭제하고 지원 폼으로 변환 생성됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "지원폼 생성 성공",
                    content = @Content(schema = @Schema(implementation = ApplyFormCreateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 필드 누락, 날짜 형식 오류, 중복 지원폼 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 동아리",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ApplyFormCreateResponse> createApplyForm(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId,
            @Parameter(description = "지원폼 생성 요청 데이터")
            ApplyFormCreateRequest request
    );

    @Operation(
            summary = "지원폼 수정",
            description = """
                    기존 지원폼의 내용을 부분적으로 수정합니다.
                    JsonNullable을 사용하여 선택적 업데이트를 지원합니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 수정 가능합니다.
                    - 이미 제출된 지원서가 있는 경우 질문 수정 시 주의해야 합니다.
                    - questions 필드 수정 시 전체 질문 배열이 교체됩니다.
                    
                    *요청 예시*
                    ```json
                    {
                        "title": "새로운 제목",
                        "subtitle": null,
                        // questions는 전송하지 않으면 수정되지 않음
                    }
                    ```
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "지원폼 수정 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 필드 누락, 날짜 형식 오류 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 지원폼",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Map<String, String>> updateApplyForm(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "지원폼 ID", required = true, example = "UUID")
            String formId,
            @Parameter(description = "지원폼 수정 요청 데이터")
            ApplyFormUpdateRequest request
    );

    @Operation(
            summary = "이전 지원폼 질문 조회",
            description = """
                    이전에 만들어둔 지원폼의 질문 형태를 조회합니다.
                    새로운 지원폼 생성 시 참고용으로 사용할 수 있습니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 조회 가능합니다.
                    - 질문 구조만 반환됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "이전 질문 조회 성공",
                    content = @Content(schema = @Schema(implementation = BeforeQuestionsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 지원폼에 대한 접근 권한이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 지원폼",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<BeforeQuestionsResponse> getPreviousQuestions(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "이전 지원폼 ID", required = true, example = "UUID")
            String formId
    );

    @Operation(
            summary = "지원자 평가 종료",
            description = """
                    지원자 평가를 완전히 종료합니다.
                    이 작업은 되돌릴 수 없으므로 신중하게 사용해야 합니다.
                    
                    *실행되는 작업*
                    1. 모든 임시 지원자 데이터 삭제
                    2. 모든 지원자 및 관련 평가 정보 삭제 (서류전형, 면접전형 포함)
                    3. 지원폼 비활성화
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 실행 가능합니다.
                    - 삭제된 데이터는 복구할 수 없습니다.
                    - 모든 지원자의 정보가 영구적으로 삭제됩니다.
                    - CASCADE 옵션으로 연관된 모든 데이터가 자동 삭제됩니다.
                    - 지원폼은 더 이상 활성화되지 않습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "지원자 평가 종료 성공",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 지원폼에 대한 관리자 권한이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 지원폼",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 (데이터 삭제 중 오류 발생)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Map<String, String>> finishEvaluating(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "종료할 지원폼 ID", required = true, example = "UUID")
            String formId
    );
}
