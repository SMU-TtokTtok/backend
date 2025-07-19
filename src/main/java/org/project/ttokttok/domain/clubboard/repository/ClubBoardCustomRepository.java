package org.project.ttokttok.domain.clubboard.repository;

import org.project.ttokttok.domain.clubboard.domain.ClubBoard;

import java.util.List;

public interface ClubBoardCustomRepository {
    
    /**
     * 동아리 게시판 목록을 커서 기반으로 조회합니다.
     * 
     * @param clubId 동아리 ID
     * @param size 조회할 개수 (다음 페이지 확인을 위해 +1 개 더 조회)
     * @param cursor 커서 (게시글 ID 기준, null이면 첫 페이지)
     * @return 게시글 목록 (최신순 정렬)
     */
    List<ClubBoard> findBoardsByClubIdWithCursor(String clubId, int size, String cursor);
} 