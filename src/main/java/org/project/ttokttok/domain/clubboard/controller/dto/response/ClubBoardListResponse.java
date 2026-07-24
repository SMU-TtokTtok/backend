package org.project.ttokttok.domain.clubboard.controller.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 동아리 게시판 목록 응답 DTO (인스타그램식 썸네일 피드)
 * 본문 등 상세 정보는 상세 조회 API(ClubBoardDetailResponse)로 제공합니다.
 */
public record ClubBoardListResponse(
        List<ClubBoardSummary> boards,
        boolean hasNext,
        String nextCursor
) {

    /**
     * 게시판 요약 정보 DTO — 피드 렌더링에 필요한 최소 필드만 포함
     */
    public record ClubBoardSummary(
            String boardId,
            String thumbnailUrl,    // 대표(썸네일) 이미지 URL. 레거시 게시글은 null
            LocalDateTime createdAt // 프론트에서 "19시간 전" 형식으로 변환
    ) {}

    /**
     * 서비스 응답으로부터 컨트롤러 응답을 생성합니다.
     */
    public static ClubBoardListResponse of(List<ClubBoardSummary> boards, boolean hasNext, String nextCursor) {
        return new ClubBoardListResponse(boards, hasNext, nextCursor);
    }
}
