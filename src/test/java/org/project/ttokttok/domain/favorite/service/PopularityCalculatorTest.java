package org.project.ttokttok.domain.favorite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.global.config.ClubPopularityConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PopularityCalculatorTest {

    private ClubPopularityConfig config;
    private PopularityCalculator popularityCalculator;

    @BeforeEach
    void setUp() {
        config = new ClubPopularityConfig();
        popularityCalculator = new PopularityCalculator(config);
    }

    @Test
    @DisplayName("기본 가중치(멤버 0.7, 즐겨찾기 2.5, 조회수 0.7)로 인기도 점수를 계산한다.")
    void calculateWithDefaultWeight() {
        // given: ClubPopularityConfig 기본값 (members=0.7, favorites=2.5, views=0.7)
        long memberCount = 10;
        long favoriteCount = 4;
        long viewCount = 20;

        // when
        double score = popularityCalculator.calculate(memberCount, favoriteCount, viewCount);

        // then: (10*0.7) + (4*2.5) + (20*0.7) = 7 + 10 + 14 = 31
        assertThat(score).isCloseTo(31.0, within(0.0001));
    }

    @Test
    @DisplayName("application-test.yml에서 사용하는 가중치(멤버 0.7, 즐겨찾기 0.3)로도 정확히 계산한다.")
    void calculateWithTestYamlWeight() {
        // given
        config.getWeight().setMembers(0.7);
        config.getWeight().setFavorites(0.3);
        config.getWeight().setViews(0.0);

        // when
        double score = popularityCalculator.calculate(10, 20, 100);

        // then: (10*0.7) + (20*0.3) + (100*0.0) = 7 + 6 + 0 = 13
        assertThat(score).isCloseTo(13.0, within(0.0001));
    }

    @Test
    @DisplayName("멤버, 즐겨찾기, 조회수가 모두 0이면 점수는 0이다.")
    void calculateWithZeroCounts() {
        double score = popularityCalculator.calculate(0, 0, 0);

        assertThat(score).isZero();
    }

    @Test
    @DisplayName("가중치가 커스텀 값이어도 각 요소에 정확히 곱해져 합산된다.")
    void calculateWithCustomWeight() {
        // given
        config.getWeight().setMembers(2.0);
        config.getWeight().setFavorites(1.0);
        config.getWeight().setViews(0.5);

        // when
        double score = popularityCalculator.calculate(3, 5, 8);

        // then: (3*2.0) + (5*1.0) + (8*0.5) = 6 + 5 + 4 = 15
        assertThat(score).isCloseTo(15.0, within(0.0001));
    }
}
