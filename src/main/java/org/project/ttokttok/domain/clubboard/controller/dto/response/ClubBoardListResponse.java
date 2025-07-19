package org.project.ttokttok.domain.clubboard.controller.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 동아리 게시판 목록 응답 DTO
 */
public record ClubBoardListResponse(
        List<ClubBoardSummary> boards,
        boolean hasNext,
        String nextCursor
) {
    
    /**
     * 게시판 요약 정보 DTO
     */
    public record ClubBoardSummary(
            String boardId,
            String title,
            String clubName,
            boolean hasImages,      // content에 이미지가 포함되어 있는지
            LocalDateTime createdAt // 프론트에서 "19시간 전" 형식으로 변환
    ) {}
    
    /**
     * 서비스 응답으로부터 컨트롤러 응답을 생성합니다.
     */
    public static ClubBoardListResponse of(List<ClubBoardSummary> boards, boolean hasNext, String nextCursor) {
        return new ClubBoardListResponse(boards, hasNext, nextCursor);
    }
} 