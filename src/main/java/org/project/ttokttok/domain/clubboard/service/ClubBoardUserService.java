package org.project.ttokttok.domain.clubboard.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse.ClubBoardSummary;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubBoardUserService {

    private final ClubBoardRepository clubBoardRepository;
    private final ClubRepository clubRepository;

    /**
     * 동아리 게시판 목록을 커서 기반으로 조회합니다.
     * 
     * @param clubId 동아리 ID
     * @param size 조회할 개수
     * @param cursor 커서 (null이면 첫 페이지)
     * @return 게시판 목록 응답
     */
    public ClubBoardListResponse getBoardList(String clubId, int size, String cursor) {
        // 동아리 존재 여부 확인
        if (!clubRepository.existsById(clubId)) {
            throw new ClubNotFoundException();
        }

        // 게시글 조회 (size + 1개를 조회하여 다음 페이지 존재 여부 확인)
        List<ClubBoard> boards = clubBoardRepository.findBoardsByClubIdWithCursor(clubId, size, cursor);
        
        // 다음 페이지 존재 여부 확인
        boolean hasNext = boards.size() > size;
        if (hasNext) {
            boards = boards.subList(0, size); // 실제 반환할 개수만큼 자르기
        }

        // 다음 커서 설정
        String nextCursor = hasNext && !boards.isEmpty() 
            ? boards.get(boards.size() - 1).getId() 
            : null;

        // DTO 변환
        List<ClubBoardSummary> summaries = boards.stream()
                .map(this::toClubBoardSummary)
                .toList();

        return ClubBoardListResponse.of(summaries, hasNext, nextCursor);
    }

    /**
     * ClubBoard 엔티티를 ClubBoardSummary DTO로 변환합니다.
     */
    private ClubBoardSummary toClubBoardSummary(ClubBoard board) {
        return new ClubBoardSummary(
                board.getId(),
                board.getTitle(),
                board.getClub().getName(),
                hasImages(board.getContent()),
                board.getCreatedAt()
        );
    }

    /**
     * 게시글 내용에 이미지가 포함되어 있는지 확인합니다.
     * HTML img 태그나 이미지 URL 패턴을 검사합니다.
     */
    private boolean hasImages(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        // HTML img 태그 검사
        if (content.toLowerCase().contains("<img")) {
            return true;
        }
        
        // 이미지 URL 패턴 검사 (board-images/ 경로 또는 일반적인 이미지 확장자)
        String lowerContent = content.toLowerCase();
        return lowerContent.contains("board-images/") ||
               lowerContent.contains(".jpg") ||
               lowerContent.contains(".jpeg") ||
               lowerContent.contains(".png") ||
               lowerContent.contains(".gif") ||
               lowerContent.contains(".webp");
    }
}
