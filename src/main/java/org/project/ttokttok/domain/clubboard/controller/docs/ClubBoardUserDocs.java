package org.project.ttokttok.domain.clubboard.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "[사용자] 동아리 게시판 조회", description = "사용자가 동아리 게시판을 조회하는 API")
public interface ClubBoardUserDocs {

    @Operation(
            summary = "동아리 게시판 목록 조회",
            description = """
                    특정 동아리의 게시판 목록을 커서 기반 무한스크롤로 조회합니다. 최신순으로 정렬됩니다.

                    **응답 필드**:
                    - `thumbnailUrl`: 대표(썸네일) 이미지 URL — 썸네일 도입 이전의 레거시 게시글은 null
                    - `hasImages`: 썸네일이 있거나 content에 이미지가 포함되어 있는지
                    - `nextCursor`: 다음 페이지 요청 시 cursor 파라미터로 전달
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ClubBoardListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "동아리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClubBoardListResponse> getBoardList(
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId,
            @Parameter(description = "조회할 개수 (기본 20개)", example = "20")
            int size,
            @Parameter(description = "무한스크롤 커서 (첫 요청시 생략)")
            String cursor
    );
}
