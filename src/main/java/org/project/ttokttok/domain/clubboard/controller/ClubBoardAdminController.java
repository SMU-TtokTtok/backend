package org.project.ttokttok.domain.clubboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.clubboard.controller.dto.request.ClubBoardUpdateRequest;
import org.project.ttokttok.domain.clubboard.controller.dto.request.CreateBoardRequest;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardCreateResponse;
import org.project.ttokttok.domain.clubboard.service.ClubBoardAdminService;
import org.project.ttokttok.domain.clubboard.service.dto.request.DeleteBoardServiceRequest;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 동아리 게시판 관리 API 컨트롤러
 * 동아리 관리자가 게시글을 생성, 수정, 삭제할 수 있는 API들을 제공합니다.
 */
@Tag(name = "[관리자] 동아리 게시판 관리", description = "동아리 관리자가 게시판 게시글을 관리하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/clubs/{clubId}/boards")
public class ClubBoardAdminController {

    private final ClubBoardAdminService clubBoardAdminService;

    @Operation(
            summary = "게시글 생성",
            description = "동아리 관리자가 게시판에 새 게시글을 작성합니다. multipart/form-data로 request(JSON)와 thumbnail(대표 이미지, 필수)을 전송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "썸네일 누락 또는 이미지 형식이 아님"),
            @ApiResponse(responseCode = "403", description = "해당 동아리의 관리자가 아님"),
            @ApiResponse(responseCode = "404", description = "동아리를 찾을 수 없음")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClubBoardCreateResponse> createBoard(
            @Parameter(hidden = true) @AuthUserInfo String username,
            @Parameter(description = "동아리 ID", required = true) @PathVariable String clubId,
            @RequestPart("request") @Valid CreateBoardRequest request,
            @RequestPart("thumbnail") MultipartFile thumbnail
    ) {
        String boardId = clubBoardAdminService.createBoard(request.toServiceRequest(username, clubId, thumbnail));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ClubBoardCreateResponse(boardId));
    }

    @Operation(
            summary = "게시글 수정",
            description = "동아리 관리자가 기존 게시글의 제목/내용/썸네일을 수정합니다. multipart/form-data로 request(JSON)와 thumbnail(대표 이미지)을 전송하며, 생략하거나 null인 필드는 기존 값을 유지합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "썸네일이 이미지 형식이 아님"),
            @ApiResponse(responseCode = "403", description = "해당 동아리의 관리자가 아님"),
            @ApiResponse(responseCode = "404", description = "동아리 또는 게시글을 찾을 수 없음")
    })
    @PatchMapping(value = "/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> updateBoard(
            @Parameter(hidden = true) @AuthUserInfo String username,
            @Parameter(description = "동아리 ID", required = true) @PathVariable String clubId,
            @Parameter(description = "게시글 ID", required = true) @PathVariable String boardId,
            @RequestPart(value = "request", required = false) ClubBoardUpdateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        ClubBoardUpdateRequest safeRequest = request != null
                ? request
                : new ClubBoardUpdateRequest(null, null);

        clubBoardAdminService.updateBoard(safeRequest.toServiceRequest(username, clubId, boardId, thumbnail));

        return ResponseEntity.ok()
                .body(Map.of("message", "게시글이 수정되었습니다."));
    }

    @Operation(
            summary = "게시글 삭제",
            description = "동아리 관리자가 게시글을 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "해당 동아리의 관리자가 아님"),
            @ApiResponse(responseCode = "404", description = "동아리 또는 게시글을 찾을 수 없음")
    })
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Map<String, String>> deleteBoard(
            @Parameter(hidden = true) @AuthUserInfo String username,
            @Parameter(description = "동아리 ID", required = true) @PathVariable String clubId,
            @Parameter(description = "게시글 ID", required = true) @PathVariable String boardId
    ) {
        clubBoardAdminService.deleteBoard(new DeleteBoardServiceRequest(username, clubId, boardId));

        return ResponseEntity.ok()
                .body(Map.of("message", "게시글이 삭제되었습니다."));
    }
}
