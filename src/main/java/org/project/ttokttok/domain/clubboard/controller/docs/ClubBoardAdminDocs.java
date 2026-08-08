package org.project.ttokttok.domain.clubboard.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.clubboard.controller.dto.request.ClubBoardUpdateRequest;
import org.project.ttokttok.domain.clubboard.controller.dto.request.CreateBoardRequest;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardCreateResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "[관리자] 동아리 게시판 관리", description = "동아리 관리자가 게시판 게시글을 관리하는 API")
public interface ClubBoardAdminDocs {

    @Operation(
            summary = "게시글 생성",
            description = """
                    동아리 관리자가 게시판에 새 게시글을 작성합니다.

                    **요청 형식**: Multipart Form Data

                    **요청 파라미터**:
                    - `request`: JSON 형태의 게시글 정보 (CreateBoardRequest, 제목 필수 / 내용 선택)
                    - `thumbnail`: 대표(썸네일) 이미지 파일 (**필수**)

                    *주의사항*:
                    - 해당 동아리의 관리자만 작성 가능합니다.
                    - 내용은 생략하거나 null로 보낼 수 있으며, 이 경우 빈 내용으로 저장됩니다.
                    - 썸네일은 JPG, PNG, WEBP, GIF, HEIC 형식만 지원됩니다. (최대 20MB)
                    - 업로드된 썸네일은 S3(board-images/)에 저장되며 목록 조회 응답의 thumbnailUrl로 내려갑니다.
                    """
    )
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schemaProperties = {
                            @SchemaProperty(name = "request", schema = @Schema(type = "string", format = "json", description = "게시글 생성 데이터 (title 필수, content 선택)")),
                            @SchemaProperty(name = "thumbnail", schema = @Schema(type = "string", format = "binary", description = "대표(썸네일) 이미지 파일 (필수)"))
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = ClubBoardCreateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "썸네일 누락, 이미지 형식이 아님 또는 필수 필드 누락",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "동아리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClubBoardCreateResponse> createBoard(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId,
            @Schema(type = "string", format = "json", description = "게시글 생성 데이터")
            CreateBoardRequest request,
            @Schema(type = "string", format = "binary", description = "대표(썸네일) 이미지 파일 (JPG, PNG, WEBP, GIF, HEIC 지원, 최대 20MB)")
            MultipartFile thumbnail
    );

    @Operation(
            summary = "게시글 수정",
            description = """
                    동아리 관리자가 기존 게시글의 제목/내용/썸네일을 수정합니다.

                    **요청 형식**: Multipart Form Data

                    **요청 파라미터**:
                    - `request`: JSON 형태의 수정 데이터 (선택, 생략하거나 null인 필드는 기존 값 유지)
                    - `thumbnail`: 교체할 대표 이미지 파일 (선택, 전송 시 기존 이미지는 삭제됨)

                    *주의사항*:
                    - 해당 동아리의 관리자만 수정 가능합니다.
                    - 썸네일만 보내 이미지 교체만 수행할 수도 있습니다.
                    """
    )
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schemaProperties = {
                            @SchemaProperty(name = "request", schema = @Schema(type = "string", format = "json", description = "게시글 수정 데이터 (title, content — 선택)")),
                            @SchemaProperty(name = "thumbnail", schema = @Schema(type = "string", format = "binary", description = "교체할 대표 이미지 파일 (선택)"))
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = Map.class, example = "{\"message\": \"게시글이 수정되었습니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "썸네일이 이미지 형식이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "동아리 또는 게시글을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Map<String, String>> updateBoard(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId,
            @Parameter(description = "게시글 ID", required = true, example = "UUID")
            String boardId,
            @Schema(type = "string", format = "json", description = "게시글 수정 데이터 (선택)")
            ClubBoardUpdateRequest request,
            @Schema(type = "string", format = "binary", description = "교체할 대표 이미지 파일 (선택)")
            MultipartFile thumbnail
    );

    @Operation(
            summary = "게시글 삭제",
            description = """
                    동아리 관리자가 게시글을 삭제합니다.

                    *주의사항*:
                    - 해당 동아리의 관리자만 삭제 가능합니다.
                    - 게시글에 연결된 S3 썸네일 파일도 함께 삭제됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = Map.class, example = "{\"message\": \"게시글이 삭제되었습니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "동아리 또는 게시글을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Map<String, String>> deleteBoard(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId,
            @Parameter(description = "게시글 ID", required = true, example = "UUID")
            String boardId
    );
}
