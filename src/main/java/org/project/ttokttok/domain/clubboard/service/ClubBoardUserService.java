package org.project.ttokttok.domain.clubboard.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardDetailResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse.ClubBoardSummary;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.exception.ClubBoardNotFoundException;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubBoardUserService {

    private final ClubBoardRepository clubBoardRepository;
    private final ClubRepository clubRepository;

    /**
     * 동아리 게시판 목록을 커서 기반으로 조회합니다. (인스타그램식 썸네일 피드)
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
     * 동아리 게시판 게시글을 단건 상세 조회합니다.
     *
     * @param clubId 동아리 ID
     * @param boardId 게시글 ID
     * @return 게시글 상세 응답
     */
    public ClubBoardDetailResponse getBoardDetail(String clubId, String boardId) {
        // 해당 동아리 소속 게시글만 조회된다 (다른 동아리의 boardId 접근 시 404)
        ClubBoard board = clubBoardRepository.findByIdAndClubIdWithClub(boardId, clubId)
                .orElseThrow(ClubBoardNotFoundException::new);

        return ClubBoardDetailResponse.from(board);
    }

    /**
     * ClubBoard 엔티티를 피드용 ClubBoardSummary DTO로 변환합니다.
     */
    private ClubBoardSummary toClubBoardSummary(ClubBoard board) {
        return new ClubBoardSummary(
                board.getId(),
                board.getThumbnailUrl(),
                board.getCreatedAt()
        );
    }
}
