package org.project.ttokttok.domain.club.repository.dto;

import java.time.LocalDateTime;

import org.project.ttokttok.global.policy.PopularityPolicy;

/**
 * 인기 목록에 오를 수 있는 동아리 한 곳. 카드 정보에 점수를 계산할 재료를 더한 것이다.
 * 점수, 필터, 정렬은 DB 가 아니라 서비스에서 {@link PopularityPolicy} 로 한다.
 */
public record PopularClubCandidate(
        ClubCardQueryResponse card,
        long favoriteCount,
        long viewCount,
        LocalDateTime createdAt
) {

    public String id() {
        return card.id();
    }

    public int memberCount() {
        return card.clubMemberCount();
    }

    public double score() {
        return PopularityPolicy.score(memberCount(), favoriteCount, viewCount);
    }
}
