package org.project.ttokttok.domain.clubboard.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardDetailResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "[사용자] 동아리 게시판 조회", description = "사용자가 동아리 게시판을 조회하는 API")
public interface ClubBoardUserDocs {

    @Operation(
            summary = "동아리 게시판 목록 조회 (썸네일 피드)",
            description = """
                    특정 동아리의 게시판 목록을 커서 기반 무한스크롤로 조회합니다. 최신순으로 정렬됩니다.

                    인스타그램식 썸네일 피드용 API로, 각 항목은 최소 필드만 내려갑니다.
                    본문 등 상세 정보는 상세 조회 API(GET /api/clubs/{clubId}/boards/{boardId})를 사용하세요.

                    **응답 필드 (항목당)**:
                    - `boardId`: 게시글 ID (상세 조회에 사용)
                    - `thumbnailUrl`: 대표(썸네일) 이미지 URL — 썸네일 도입 이전의 레거시 게시글은 null
                    - `createdAt`: 작성 시각
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
            @Parameter(description = "조회할 개수 (기본 20개, 1~50 범위로 보정)", example = "20")
            int size,
            @Parameter(description = "무한스크롤 커서 (첫 요청시 생략)")
            String cursor
    );

    @Operation(
            summary = "동아리 게시판 상세 조회",
            description = """
                    게시글 단건의 상세 정보를 조회합니다.

                    **응답 필드**:
                    - `boardId`, `title`, `content`(본문 전문), `thumbnailUrl`(레거시 게시글은 null), `clubName`, `createdAt`

                    *주의사항*:
                    - 요청한 동아리(clubId) 소속 게시글이 아니면 404가 반환됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ClubBoardDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음 (해당 동아리 소속이 아닌 경우 포함)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClubBoardDetailResponse> getBoardDetail(
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId,
            @Parameter(description = "게시글 ID", required = true, example = "UUID")
            String boardId
    );
}
