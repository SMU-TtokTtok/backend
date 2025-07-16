package org.project.ttokttok.domain.favorite.service.dto.response;

import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 즐겨찾기 목록 서비스 응답 DTO
 */
public record FavoriteListServiceResponse(
        List<ClubCardServiceResponse> favoriteClubs,
        String nextCursor,
        boolean hasNext
) {
} 