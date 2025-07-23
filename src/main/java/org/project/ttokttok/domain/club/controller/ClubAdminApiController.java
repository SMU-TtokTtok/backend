package org.project.ttokttok.domain.club.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.club.controller.dto.request.UpdateClubContentRequest;
import org.project.ttokttok.domain.club.service.ClubAdminService;
import org.project.ttokttok.domain.club.service.dto.request.MarkdownImageUpdateRequest;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/clubs")
public class ClubAdminApiController {

    private final ClubAdminService clubAdminService;

    // 동아리 소개 수정 로직
    @PatchMapping(value = "/{clubId}/content",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateClubContent(@AuthUserInfo String username,
                                                    @PathVariable String clubId,
                                                    @RequestPart @Valid UpdateClubContentRequest request) {
        clubAdminService.updateContent(username, request.toServiceRequest(clubId));

        return ResponseEntity.ok()
                .body("Club content updated successfully.");
    }

    @PostMapping(value = "/{clubId}/update-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateMarkdownImage(@AuthUserInfo String username,
                                                      @PathVariable String clubId,
                                                      @RequestPart("image") MultipartFile imageFile) {

        MarkdownImageUpdateRequest request = MarkdownImageUpdateRequest.of(
                username,
                clubId,
                imageFile
        );

        String imageKey = clubAdminService.updateMarkdownImage(request);

        return ResponseEntity.ok()
                .body("Markdown image Updated successfully. Image Key: " + imageKey);
    }

    @GetMapping("/image")
    public ResponseEntity<String> getImageUrl(@RequestParam String imageKey) {
        String imageUrl = clubAdminService.getImageUrl(imageKey);

        return ResponseEntity.ok(imageUrl);
    }
}
