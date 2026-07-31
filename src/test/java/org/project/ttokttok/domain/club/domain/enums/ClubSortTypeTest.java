package org.project.ttokttok.domain.club.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClubSortType - 정렬 키 변환")
class ClubSortTypeTest {

    @Test
    @DisplayName("정의된 정렬 키를 해당 enum으로 변환한다")
    void from_mapsKnownKeys() {
        assertThat(ClubSortType.from("popular")).isEqualTo(ClubSortType.POPULAR);
        assertThat(ClubSortType.from("member_count")).isEqualTo(ClubSortType.MEMBER_COUNT);
        assertThat(ClubSortType.from("latest")).isEqualTo(ClubSortType.LATEST);
    }

    @ParameterizedTest(name = "\"{0}\" -> LATEST")
    @NullSource
    @ValueSource(strings = {"", "unknown", "인기순"})
    @DisplayName("알 수 없는 값은 기본값 LATEST로 떨어진다")
    void from_fallsBackToLatest(String value) {
        assertThat(ClubSortType.from(value)).isEqualTo(ClubSortType.LATEST);
    }

    @ParameterizedTest(name = "\"{0}\" -> LATEST (대소문자 구분)")
    @ValueSource(strings = {"POPULAR", "Popular", "MEMBER_COUNT"})
    @DisplayName("대소문자를 구분한다 — 기존 리포지토리 동작을 그대로 보존한 것으로, 관대한 매칭은 별도 이슈에서 다룬다")
    void from_isCaseSensitive_preservingLegacyBehavior(String value) {
        assertThat(ClubSortType.from(value)).isEqualTo(ClubSortType.LATEST);
    }
}
