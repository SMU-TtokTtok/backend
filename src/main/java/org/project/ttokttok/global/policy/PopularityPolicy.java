package org.project.ttokttok.global.policy;

/**
 * 동아리 인기도 규칙
 *
 * 점수 = 멤버 수 × 멤버 가중치 + 즐겨찾기 수 × 즐겨찾기 가중치 + 조회수 × 조회 가중치.
 * 배너와 인기 목록의 순위, 즐겨찾기 목록의 인기순 정렬이 모두 이 규칙 하나를 쓴다.
 * 배너와 인기 목록에는 점수가 {@link #MIN_SCORE} 이상인 동아리만 노출한다.
 *
 * 가중치는 코드 규칙이라 설정 파일에 두지 않는다. 바꾸려면 이 상수를 고친다.
 */
public final class PopularityPolicy {

    public static final double MEMBER_WEIGHT = 0.7;
    public static final double FAVORITE_WEIGHT = 2.5;
    public static final double VIEW_WEIGHT = 0.7;
    public static final double MIN_SCORE = 7.0;

    /** 메인 배너에 노출하는 인기 동아리 최대 개수 */
    public static final int BANNER_SIZE = 10;

    private PopularityPolicy() {
    }

    public static double score(long memberCount, long favoriteCount, long viewCount) {
        return memberCount * MEMBER_WEIGHT + favoriteCount * FAVORITE_WEIGHT + viewCount * VIEW_WEIGHT;
    }
}
