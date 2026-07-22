package org.project.ttokttok.domain.notice.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeDetailResponse;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeListResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "[공용] 공지사항 조회 API", description = "누구나(비로그인 포함) 서비스 공지사항을 조회하는 API 입니다.")
public interface NoticeUserDocs {

    @Operation(
            summary = "공지사항 목록 조회",
            description = """
                    서비스 공지사항 목록을 페이지 번호(1-based) 기반으로 조회합니다. 최신순으로 정렬됩니다.
                    keyword가 있으면 제목에 해당 키워드가 포함된 공지만 조회합니다.
                    응답의 totalCount로 "총 N건"을 표시할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = NoticeListResponse.class))
            )
    })
    ResponseEntity<NoticeListResponse> getNotices(
            @Parameter(description = "페이지 번호 (기본값 1)") int page,
            @Parameter(description = "페이지당 개수 (기본값 10)") int size,
            @Parameter(description = "제목 검색어 (선택)") String keyword
    );

    @Operation(
            summary = "공지사항 상세 조회",
            description = """
                    공지사항 상세 내용을 조회합니다. 조회 시 조회수가 1 증가합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = NoticeDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "공지사항을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<NoticeDetailResponse> getNoticeDetail(
            @Parameter(description = "공지사항 ID", required = true) String noticeId
    );
}
