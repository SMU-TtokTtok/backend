package org.project.ttokttok.domain.clubboard.controller;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.clubboard.controller.docs.ClubBoardUserDocs;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardDetailResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse;
import org.project.ttokttok.domain.clubboard.service.ClubBoardUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 동아리 게시판 조회 API 컨트롤러
 * 사용자가 동아리 게시판을 조회할 수 있는 API들을 제공합니다.
 * Swagger 문서는 {@link ClubBoardUserDocs}에 분리되어 있습니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs/{clubId}/boards")
public class ClubBoardUserController implements ClubBoardUserDocs {

    private final ClubBoardUserService clubBoardUserService;

    /**
     * 동아리 게시판 목록 조회 API
     * 특정 동아리의 게시판 목록을 커서 기반 무한스크롤로 조회합니다.
     *
     * @param clubId 동아리 ID
     * @param size 조회할 개수 (기본값: 20)
     * @param cursor 커서 (첫 요청시 생략)
     * @return 게시판 목록과 페이징 정보
     */
    @Override
    @GetMapping
    public ResponseEntity<ClubBoardListResponse> getBoardList(
            @PathVariable String clubId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor
    ) {
        ClubBoardListResponse response = clubBoardUserService.getBoardList(clubId, size, cursor);

        return ResponseEntity.ok(response);
    }

    /**
     * 동아리 게시판 상세 조회 API
     * 게시글 단건의 상세 정보(본문 전문 포함)를 조회합니다.
     *
     * @param clubId 동아리 ID
     * @param boardId 게시글 ID
     * @return 게시글 상세 정보
     */
    @Override
    @GetMapping("/{boardId}")
    public ResponseEntity<ClubBoardDetailResponse> getBoardDetail(
            @PathVariable String clubId,
            @PathVariable String boardId
    ) {
        ClubBoardDetailResponse response = clubBoardUserService.getBoardDetail(clubId, boardId);

        return ResponseEntity.ok(response);
    }
}
