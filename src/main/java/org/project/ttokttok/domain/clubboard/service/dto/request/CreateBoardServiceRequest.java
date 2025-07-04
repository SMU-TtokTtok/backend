package org.project.ttokttok.domain.clubboard.service.dto.request;

public record CreateBoardServiceRequest(
        String adminName,
        String clubId,
        String title,
        String content
) {
}
