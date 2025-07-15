package org.project.ttokttok.domain.clubboard.service.dto.request;

public record DeleteBoardServiceRequest(
        String adminName,
        String clubId,
        String boardId
) {
}
