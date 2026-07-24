package org.project.ttokttok.domain.clubboard.service.dto.request;

import org.springframework.web.multipart.MultipartFile;

public record CreateBoardServiceRequest(
        String adminName,
        String clubId,
        String title,
        String content,
        MultipartFile thumbnail
) {
}
