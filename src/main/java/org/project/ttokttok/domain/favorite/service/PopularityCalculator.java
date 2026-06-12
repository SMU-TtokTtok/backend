package org.project.ttokttok.domain.favorite.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.global.config.ClubPopularityConfig;
import org.springframework.stereotype.Component;

/**
 * 동아리 인기도 점수 계산을 담당하는 클래스
 */
@Component
@RequiredArgsConstructor
public class PopularityCalculator {

    private final ClubPopularityConfig config;

    /**
     * 동아리 인기도 점수 계산 (설정된 가중치 반영)
     * 점수 = (멤버 수 * 0.7) + (즐겨찾기 수 * 2.5) + (조회수 * 0.7)
     *
     * @param memberCount   동아리 멤버 수
     * @param favoriteCount 동아리 총 즐겨찾기 수
     * @param viewCount     동아리 총 조회수
     * @return 계산된 인기도 점수
     */
    public double calculate(long memberCount, long favoriteCount, long viewCount) {
        double memberWeight = config.getWeight().getMembers();
        double favoriteWeight = config.getWeight().getFavorites();
        double viewWeight = config.getWeight().getViews();

        return (memberCount * memberWeight) + (favoriteCount * favoriteWeight) + (viewCount * viewWeight);
    }
}
