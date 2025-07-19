package org.project.ttokttok.domain.clubboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse;
import org.project.ttokttok.domain.clubboard.service.ClubBoardUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 동아리 게시판 조회 API 컨트롤러
 * 사용자가 동아리 게시판을 조회할 수 있는 API들을 제공합니다.
 */
@Slf4j
@Tag(name = "동아리 게시판 조회", description = "사용자가 동아리 게시판을 조회하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs/{clubId}/boards")
public class ClubBoardUserController {

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
    @Operation(
            summary = "동아리 게시판 목록 조회",
            description = "특정 동아리의 게시판 목록을 커서 기반 무한스크롤로 조회합니다. 최신순으로 정렬됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "동아리를 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<ClubBoardListResponse> getBoardList(
            @Parameter(description = "동아리 ID", required = true)
            @PathVariable String clubId,
            
            @Parameter(description = "조회할 개수 (기본 20개)")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "무한스크롤 커서 (첫 요청시 생략)")
            @RequestParam(required = false) String cursor
    ) {
        ClubBoardListResponse response = clubBoardUserService.getBoardList(clubId, size, cursor);
        
        return ResponseEntity.ok(response);
    }
} 