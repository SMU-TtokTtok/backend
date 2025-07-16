package org.project.ttokttok.domain.favorite.controller.dto.response;

import org.project.ttokttok.domain.club.controller.dto.response.ClubCardResponse;
import org.project.ttokttok.domain.favorite.service.dto.response.FavoriteListServiceResponse;

import java.util.List;

/**
 * 즐겨찾기 목록 응답 DTO (커서 기반)
 */
public record FavoriteListResponse(
        List<ClubCardResponse> favoriteClubs,
        String nextCursor, // 다음 페이지를 요청할 때 사용할 커서 (마지막 아이템의 ID)
        boolean hasNext    // 다음 페이지 존재 여부
) {
    public static FavoriteListResponse from(FavoriteListServiceResponse serviceResponse) {
        List<ClubCardResponse> clubCards = serviceResponse.favoriteClubs().stream()
            .map(ClubCardResponse::from)
            .toList();

        return new FavoriteListResponse(
                clubCards,
                serviceResponse.nextCursor(),
                serviceResponse.hasNext()
        );
    }
}