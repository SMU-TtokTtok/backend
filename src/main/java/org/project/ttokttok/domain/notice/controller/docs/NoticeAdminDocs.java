package org.project.ttokttok.domain.notice.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.notice.controller.dto.request.CreateNoticeRequest;
import org.project.ttokttok.domain.notice.controller.dto.response.NoticeCreateResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "[운영자] 공지사항 관리 API", description = "서비스 운영/유지보수 팀이 전역 공지사항을 작성하는 API 입니다.")
public interface NoticeAdminDocs {

    @Operation(
            summary = "공지사항 저장",
            description = """
                    서비스 전역 공지사항을 저장합니다.
                    운영자(ROLE_SUPER_ADMIN) 권한을 가진 계정만 호출할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "공지사항 저장 성공",
                    content = @Content(schema = @Schema(implementation = NoticeCreateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (제목/내용 누락)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "운영자 권한이 없는 요청"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<NoticeCreateResponse> createNotice(
            @Parameter(description = "인증된 운영자 아이디", hidden = true) String username,
            @Parameter(description = "공지사항 저장 요청 (제목, 내용)") CreateNoticeRequest request
    );
}
