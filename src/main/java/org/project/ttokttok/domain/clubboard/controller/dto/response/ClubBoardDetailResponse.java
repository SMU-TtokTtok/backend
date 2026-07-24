package org.project.ttokttok.domain.clubboard.controller.dto.response;

import org.project.ttokttok.domain.clubboard.domain.ClubBoard;

import java.time.LocalDateTime;

/**
 * 동아리 게시판 상세 조회 응답 DTO
 */
public record ClubBoardDetailResponse(
        String boardId,
        String title,
        String content,
        String thumbnailUrl,    // 대표(썸네일) 이미지 URL. 레거시 게시글은 null
        String clubName,
        LocalDateTime createdAt
) {
    public static ClubBoardDetailResponse from(ClubBoard board) {
        return new ClubBoardDetailResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getThumbnailUrl(),
                board.getClub().getName(),
                board.getCreatedAt()
        );
    }
}
