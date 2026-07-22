package org.project.ttokttok.domain.clubboard.service.dto.request;

import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record ClubBoardUpdateServiceRequest(
        String username,
        String clubId,
        String boardId,
        String title,
        String content,
        MultipartFile thumbnail
) {
}
