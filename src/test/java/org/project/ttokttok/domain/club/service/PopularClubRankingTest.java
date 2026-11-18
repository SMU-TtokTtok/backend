package org.project.ttokttok.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubSortType;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.PopularClubCandidate;

@DisplayName("PopularClubRanking - 인기 목록 순위와 페이지")
class PopularClubRankingTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 0, 0);

    private PopularClubCandidate club(String id, int members, long favorites, long views, int createdDay) {
        ClubCardQueryResponse card = new ClubCardQueryResponse(
                id, "동아리" + id, ClubType.CENTRAL, ClubCategory.ACADEMIC,
                null, "요약", "http://img", members, true, false, false, null);

        return new PopularClubCandidate(card, favorites, views, BASE.plusDays(createdDay));
    }

    private List<String> ids(List<PopularClubCandidate> page) {
        return page.stream().map(PopularClubCandidate::id).toList();
    }

    /**
     * 점수 = 0.7 × 멤버 + 2.5 × 즐겨찾기 + 0.7 × 조회.
     * a = 0.7×10 + 0.7×20 = 21.0, b = 0.7×1 + 2.5×10 = 25.7, c = 0.7×30 = 21.0 (a 와 동점), d = 0.7 (최소 점수 미만)
     * 멤버 수는 c(30)가 가장 많고, 등록은 d 가 가장 늦다.
     */
    private List<PopularClubCandidate> sample() {
        return List.of(
                club("a", 10, 0, 20, 1),
                club("b", 1, 10, 0, 2),
                club("c", 30, 0, 0, 3),
                club("d", 1, 0, 0, 4));
    }

    @Test
    @DisplayName("인기순은 점수 내림차순이고 동점이면 id 내림차순이며 최소 점수 미만은 뺀다")
    void popular_ordersByScoreThenIdAndDropsBelowMinScore() {
        List<PopularClubCandidate> page = PopularClubRanking.page(sample(), ClubSortType.POPULAR, null, 10);

        assertThat(ids(page)).containsExactly("b", "c", "a");
    }

    @Test
    @DisplayName("멤버순은 멤버 수 내림차순이다")
    void memberCount_ordersByMembersDesc() {
        List<PopularClubCandidate> page = PopularClubRanking.page(sample(), ClubSortType.MEMBER_COUNT, null, 10);

        assertThat(ids(page)).containsExactly("c", "a", "b");
    }

    @Test
    @DisplayName("최신순은 등록 시각 내림차순이다")
    void latest_ordersByCreatedAtDesc() {
        List<PopularClubCandidate> page = PopularClubRanking.page(sample(), ClubSortType.LATEST, null, 10);

        assertThat(ids(page)).containsExactly("c", "b", "a");
    }

    @Test
    @DisplayName("개수 제한을 넘기지 않는다")
    void limit_cutsTheList() {
        List<PopularClubCandidate> page = PopularClubRanking.page(sample(), ClubSortType.POPULAR, null, 2);

        assertThat(ids(page)).containsExactly("b", "c");
    }

    @Test
    @DisplayName("커서를 주면 그 동아리 바로 뒤부터 돌려준다")
    void cursor_continuesAfterThatClub() {
        List<PopularClubCandidate> page = PopularClubRanking.page(sample(), ClubSortType.POPULAR, "c", 10);

        assertThat(ids(page)).containsExactly("a");
    }

    @Test
    @DisplayName("커서의 동아리가 점수 미달로 목록에서 빠져도 그 위치 뒤부터 이어진다")
    void cursor_worksEvenIfCursorClubDroppedBelowMinScore() {
        // d 는 최소 점수 미만이라 목록에 없지만 id 로는 찾을 수 있고, 점수가 가장 낮아 뒤에 올 항목이 없다
        List<PopularClubCandidate> page = PopularClubRanking.page(sample(), ClubSortType.POPULAR, "d", 10);

        assertThat(page).isEmpty();
    }

    @Test
    @DisplayName("없는 동아리 id 를 커서로 주면 빈 목록이다")
    void cursor_unknownIdReturnsEmpty() {
        List<PopularClubCandidate> page = PopularClubRanking.page(sample(), ClubSortType.POPULAR, "없는id", 10);

        assertThat(page).isEmpty();
    }

    @Test
    @DisplayName("어느 정렬에서든 커서를 따라 끝까지 넘기면 한 번에 받은 결과와 같다")
    void paging_equalsSinglePage_forEverySort() {
        for (ClubSortType sort : ClubSortType.values()) {
            List<String> single = ids(PopularClubRanking.page(sample(), sort, null, 10));

            String cursor = null;
            java.util.ArrayList<String> paged = new java.util.ArrayList<>();
            for (int i = 0; i < 10; i++) {
                List<PopularClubCandidate> page = PopularClubRanking.page(sample(), sort, cursor, 1);
                if (page.isEmpty()) {
                    break;
                }
                paged.addAll(ids(page));
                cursor = page.get(0).id();
            }

            assertThat(paged).as("sort=%s", sort).isEqualTo(single);
        }
    }
}
