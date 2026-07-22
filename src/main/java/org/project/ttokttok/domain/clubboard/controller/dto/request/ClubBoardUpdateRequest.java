package org.project.ttokttok.domain.clubboard.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.ttokttok.domain.clubboard.service.dto.request.ClubBoardUpdateServiceRequest;
import org.springframework.web.multipart.MultipartFile;

public record ClubBoardUpdateRequest(
        @Schema(description = "선택적 필드 (생략 또는 null이면 기존 값 유지)", nullable = true, example = "값 또는 null")
        String title,

        @Schema(description = "선택적 필드 (생략 또는 null이면 기존 값 유지)", nullable = true, example = "값 또는 null")
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
