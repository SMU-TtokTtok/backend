package org.project.ttokttok.domain.club.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.project.ttokttok.domain.club.domain.enums.ClubSortType;
import org.project.ttokttok.domain.club.repository.dto.PopularClubCandidate;
import org.project.ttokttok.global.policy.PopularityPolicy;

/**
 * 인기 목록의 순위와 페이지 자르기.
 *
 * <p>점수가 {@link PopularityPolicy#MIN_SCORE} 이상인 동아리만 목록에 오른다. 정렬 키가 같으면 id 내림차순이다.
 * 커서는 이전 페이지 마지막 동아리의 id 다. 그 동아리의 현재 위치를 기준으로 그 뒤를 돌려주므로,
 * 정렬 키가 id 가 아니어도 페이지 사이에 누락과 중복이 생기지 않는다.
 */
final class PopularClubRanking {

    private PopularClubRanking() {
    }

    /**
     * @param all    후보 전체 (점수 미달 포함)
     * @param cursor 이전 페이지 마지막 동아리 id. 처음이면 null. 없는 id 면 빈 목록
     * @param limit  최대 개수
     */
    static List<PopularClubCandidate> page(List<PopularClubCandidate> all, ClubSortType sort,
                                           String cursor, int limit) {
        Comparator<PopularClubCandidate> order = orderOf(sort);

        Stream<PopularClubCandidate> ranked = all.stream()
                .filter(candidate -> candidate.score() >= PopularityPolicy.MIN_SCORE)
                .sorted(order);

        if (cursor != null) {
            Optional<PopularClubCandidate> last = all.stream()
                    .filter(candidate -> candidate.id().equals(cursor))
                    .findFirst();
            if (last.isEmpty()) {
                return List.of();
            }
            ranked = ranked.filter(candidate -> order.compare(candidate, last.get()) > 0);
        }

        return ranked.limit(limit).toList();
    }

    /** 앞에 올수록 작은 순서다. 인기순은 점수, 멤버순은 멤버 수, 최신순은 등록 시각이 큰 쪽이 앞이다. */
    private static Comparator<PopularClubCandidate> orderOf(ClubSortType sort) {
        Comparator<PopularClubCandidate> byKey = switch (sort) {
            case POPULAR -> Comparator.comparingDouble(PopularClubCandidate::score).reversed();
            case MEMBER_COUNT -> Comparator.comparingInt(PopularClubCandidate::memberCount).reversed();
            case LATEST -> Comparator.comparing(PopularClubCandidate::createdAt).reversed();
        };

        return byKey.thenComparing(PopularClubCandidate::id, Comparator.reverseOrder());
    }
}
