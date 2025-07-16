package org.project.ttokttok.domain.favorite.service.dto.response;

import lombok.Builder;

/**
 * 즐겨찾기 토글 서비스 응답 DTO
 */
@Builder
public record FavoriteToggleServiceResponse(
        String clubId,
        boolean favorited,
        String message
) {
    public static FavoriteToggleServiceResponse of(String clubId, boolean favorited) {
        String message = favorited ? "즐겨찾기에 추가되었습니다." : "즐겨찾기에서 제거되었습니다.";
        return FavoriteToggleServiceResponse.builder()
                .clubId(clubId)
                .favorited(favorited)
                .message(message)
                .build();
    }
} 