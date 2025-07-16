package org.project.ttokttok.domain.favorite.service.dto.request;

import lombok.Builder;

@Builder
public record FavoriteListServiceRequest(
        String userEmail,
        String cursor,  // 커서로 사용될 ID
        int size,
        String sort  // 정렬 기준
) {
}
