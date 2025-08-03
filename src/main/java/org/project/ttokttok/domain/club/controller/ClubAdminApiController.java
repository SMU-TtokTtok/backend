package org.project.ttokttok.domain.club.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.club.controller.docs.ClubAdminApiDocs;
import org.project.ttokttok.domain.club.controller.dto.request.UpdateClubContentRequest;
import org.project.ttokttok.domain.club.controller.dto.response.ClubAdminDetailResponse;
import org.project.ttokttok.domain.club.controller.dto.response.UpdateImageResponse;
import org.project.ttokttok.domain.club.service.ClubAdminService;
import org.project.ttokttok.domain.club.service.dto.request.MarkdownImageUpdateRequest;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/clubs")
public class ClubAdminApiController implements ClubAdminApiDocs {

    private final ClubAdminService clubAdminService;

    // 동아리 소개 수정 로직
    @PatchMapping(value = "/{clubId}/content",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> updateClubContent(@AuthUserInfo String username,
                                                                 @PathVariable String clubId,
                                                                 @Valid @RequestPart UpdateClubContentRequest request,
                                                                 @RequestPart(required = false) MultipartFile profileImage) {
        clubAdminService.updateContent(
                username, request.toServiceRequest(clubId, JsonNullable.of(profileImage))
        );

        return ResponseEntity.ok()
                .body(Map.of("message", "동아리 소개가 수정되었습니다."));
    }

    @PostMapping(value = "/{clubId}/update-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UpdateImageResponse> updateMarkdownImage(@AuthUserInfo String username,
                                                                   @PathVariable String clubId,
                                                                   @RequestPart("image") MultipartFile imageFile) {

        MarkdownImageUpdateRequest request = MarkdownImageUpdateRequest.of(
                username,
                clubId,
                imageFile
        );

        String imageKey = clubAdminService.updateMarkdownImage(request);

        UpdateImageResponse response = new UpdateImageResponse(imageKey);

        return ResponseEntity.ok()
                .body(response);
    }

    // 모집 마감, 재시작 토글 api
    @PatchMapping("/{clubId}/toggle-recruitment")
    public ResponseEntity<Map<String, String>> toggleRecruitment(@AuthUserInfo String username,
                                                                 @PathVariable String clubId) {
        clubAdminService.toggleRecruitment(username, clubId);

        return ResponseEntity.ok()
                .body(Map.of("message", "동아리 모집 상태가 변경되었습니다."));
    }

    // 관리자 동아리 소개 조회 API
    @GetMapping("/{clubId}/content")
    public ResponseEntity<ClubAdminDetailResponse> getClubContent(@PathVariable String clubId) {
        ClubAdminDetailResponse response = ClubAdminDetailResponse.from(
                clubAdminService.getClubContent(clubId)
        );

        return ResponseEntity.ok()
                .body(response);
    }
}
