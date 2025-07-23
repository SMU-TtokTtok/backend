package org.project.ttokttok.domain.club.service.dto.request;

import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record MarkdownImageUpdateRequest(
        String username,
        String clubId,
        MultipartFile imageFile
) {
    public static MarkdownImageUpdateRequest of(String username, String clubId, MultipartFile imageFile) {
        return MarkdownImageUpdateRequest.builder()
                .username(username)
                .clubId(clubId)
                .imageFile(imageFile)
                .build();
    }
}
