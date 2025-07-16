package org.project.ttokttok.domain.favorite.service.dto.request;

import lombok.Builder;

/**
 * 즐겨찾기 토글 서비스 요청 DTO
 */
@Builder
public record FavoriteToggleServiceRequest(
        String userEmail,
        String clubId
) {
    public static FavoriteToggleServiceRequest of(String userEmail, String clubId) {
        return FavoriteToggleServiceRequest.builder()
                .userEmail(userEmail)
                .clubId(clubId)
                .build();
    }
} 