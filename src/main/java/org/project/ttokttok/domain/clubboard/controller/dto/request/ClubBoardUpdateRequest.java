package org.project.ttokttok.domain.clubboard.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.project.ttokttok.domain.clubboard.service.dto.request.ClubBoardUpdateServiceRequest;
import org.springframework.web.multipart.MultipartFile;

public record ClubBoardUpdateRequest(
        @Schema(description = "선택적 필드 (생략 또는 null이면 기존 값 유지)", nullable = true, example = "값 또는 null")
        // 저장 컬럼이 VARCHAR(255) 라 초과하면 DB 단에서 터진다. 여기서 막아 400 으로 돌려준다.
        @Size(max = 255, message = "제목은 최대 255자까지 입력할 수 있습니다.")
        String title,

        @Schema(description = "선택적 필드 (생략 또는 null이면 기존 값 유지)", nullable = true, example = "값 또는 null")
        // 저장 컬럼은 TEXT 라 무제한이지만, 저장소·응답 비대화를 막으려면 API 단에서 상한이 필요하다.
        @Size(max = 10000, message = "내용은 최대 10000자까지 입력할 수 있습니다.")
        String content
) {
    public ClubBoardUpdateServiceRequest toServiceRequest(String username, String clubId, String boardId, MultipartFile thumbnail) {
        return ClubBoardUpdateServiceRequest.builder()
                .username(username)
                .clubId(clubId)
                .boardId(boardId)
                .title(title)
                .content(content)
                .thumbnail(thumbnail)
                .build();
    }
}
