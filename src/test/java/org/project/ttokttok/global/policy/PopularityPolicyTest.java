package org.project.ttokttok.global.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PopularityPolicyTest {

    @Test
    @DisplayName("멤버, 즐겨찾기, 조회수에 각 가중치를 곱해 합산한다.")
    void score() {
        // (10*0.7) + (4*2.5) + (20*0.7) = 7 + 10 + 14 = 31
        assertThat(PopularityPolicy.score(10, 4, 20)).isCloseTo(31.0, within(0.0001));
    }

    @Test
    @DisplayName("멤버, 즐겨찾기, 조회수가 모두 0이면 점수는 0이다.")
    void scoreWithZeroCounts() {
        assertThat(PopularityPolicy.score(0, 0, 0)).isZero();
    }
}
