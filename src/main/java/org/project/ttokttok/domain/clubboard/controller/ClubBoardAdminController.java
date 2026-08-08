package org.project.ttokttok.domain.clubboard.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.clubboard.controller.docs.ClubBoardAdminDocs;
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
 * Swagger 문서는 {@link ClubBoardAdminDocs}에 분리되어 있습니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/clubs/{clubId}/boards")
public class ClubBoardAdminController implements ClubBoardAdminDocs {

    private final ClubBoardAdminService clubBoardAdminService;

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClubBoardCreateResponse> createBoard(
            @AuthUserInfo String username,
            @PathVariable String clubId,
            @RequestPart("request") @Valid CreateBoardRequest request,
            @RequestPart("thumbnail") MultipartFile thumbnail
    ) {
        String boardId = clubBoardAdminService.createBoard(request.toServiceRequest(username, clubId, thumbnail));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ClubBoardCreateResponse(boardId));
    }

    @Override
    @PatchMapping(value = "/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> updateBoard(
            @AuthUserInfo String username,
            @PathVariable String clubId,
            @PathVariable String boardId,
            @RequestPart(value = "request", required = false) @Valid ClubBoardUpdateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        ClubBoardUpdateRequest safeRequest = request != null
                ? request
                : new ClubBoardUpdateRequest(null, null);

        clubBoardAdminService.updateBoard(safeRequest.toServiceRequest(username, clubId, boardId, thumbnail));

        return ResponseEntity.ok()
                .body(Map.of("message", "게시글이 수정되었습니다."));
    }

    @Override
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Map<String, String>> deleteBoard(
            @AuthUserInfo String username,
            @PathVariable String clubId,
            @PathVariable String boardId
    ) {
        clubBoardAdminService.deleteBoard(new DeleteBoardServiceRequest(username, clubId, boardId));

        return ResponseEntity.ok()
                .body(Map.of("message", "게시글이 삭제되었습니다."));
    }
}
