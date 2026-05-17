package org.project.ttokttok.domain.favorite.repository.dto;

public record ClubFavoriteCountQueryDto(
        String clubId,
        long count
) {
}
