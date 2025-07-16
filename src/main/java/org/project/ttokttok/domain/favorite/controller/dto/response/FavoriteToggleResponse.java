package org.project.ttokttok.domain.favorite.controller.dto.response;

import org.project.ttokttok.domain.favorite.service.dto.response.FavoriteToggleServiceResponse;

/**
 * 즐겨찾기 토글 응답 DTO
 */
public record FavoriteToggleResponse(
        String clubId,
        boolean favorited,
        String message
) {
    public static FavoriteToggleResponse from(FavoriteToggleServiceResponse serviceResponse) {
        return new FavoriteToggleResponse(
                serviceResponse.clubId(),
                serviceResponse.favorited(),
                serviceResponse.message()
        );
    }
} 