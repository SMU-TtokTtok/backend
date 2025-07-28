package org.project.ttokttok.domain.memo.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.memo.controller.dto.request.MemoRequest;
import org.project.ttokttok.domain.memo.controller.dto.response.MemoCreateResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "[관리자] 메모 API", description = "지원자 별 메모 관리용 API 입니다.")
public interface MemoDocs {

    @Operation(
            summary = "메모 생성",
            description = """
                    특정 지원자에 대한 메모를 생성합니다.
                    관리자 권한이 필요하며, 지원서 검토 과정에서 사용됩니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 생성 가능합니다.
                    - 메모 내용은 필수 입력 사항입니다.
                    - 생성된 메모는 해당 지원자와 연결됩니다.
                    - 메모 내용은 최대 100자까지 입력할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "메모 생성 성공",
                    content = @Content(schema = @Schema(implementation = String.class, example = "Memo created successfully with ID: UUID"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 필드 누락 또는 형식 오류)",
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
                    description = "존재하지 않는 지원자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<MemoCreateResponse> createMemo(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "지원자 ID", required = true, example = "UUID")
            String applicantId,
            @Parameter(description = "메모 생성 요청 데이터")
            MemoRequest request
    );

    @Operation(
            summary = "메모 수정",
            description = """
                    기존 메모의 내용을 수정합니다.
                    관리자 권한이 필요합니다.
                    
                    *주의사항*
                    - 메모 내용은 필수 입력 사항입니다.
                    - 수정된 메모는 최대 100자까지 입력할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "메모 수정 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 필드 누락 또는 형식 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "메모 작성자가 아니거나 권한 부족",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 지원자 또는 메모",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> updateMemo(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "지원자 ID", required = true, example = "UUID")
            String applicantId,
            @Parameter(description = "메모 ID", required = true, example = "UUID")
            String memoId,
            @Parameter(description = "메모 수정 요청 데이터")
            MemoRequest request
    );

    @Operation(
            summary = "메모 삭제",
            description = """
                    기존 메모를 삭제합니다.
                    관리자 권한이 필요합니다.
                    
                    *주의사항*
                    - 관리자만 삭제 가능합니다.
                    - 삭제된 메모는 복구할 수 없습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "메모 삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "메모 작성자가 아니거나 권한 부족",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 지원자 또는 메모",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> deleteMemo(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            @AuthUserInfo String username,
            @Parameter(description = "지원자 ID", required = true, example = "UUID")
            @PathVariable String applicantId,
            @Parameter(description = "삭제할 메모 ID", required = true, example = "UUID")
            @PathVariable String memoId
    );
}
