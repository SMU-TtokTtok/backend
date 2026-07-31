package org.project.ttokttok.domain.club.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 동아리 목록 정렬 기준
 *
 * <p>기존에 {@code ClubCustomRepositoryImpl} 의 QueryDSL 분기와 native SQL 분기 두 곳에
 * 흩어져 있던 정렬 키 문자열을 한 곳으로 모은 것입니다.
 */
@Getter
@RequiredArgsConstructor
public enum ClubSortType {

    /** 인기도순 (점수 기준) */
    POPULAR("popular"),

    /** 멤버 많은 순 */
    MEMBER_COUNT("member_count"),

    /** 최신 등록순 — 알 수 없는 값이 들어왔을 때의 기본값이기도 하다. */
    LATEST("latest");

    private final String key;

    /**
     * 정렬 키 문자열을 enum 으로 변환한다.
     *
     * <p><b>주의:</b> 기존 리포지토리 동작을 그대로 보존하기 위해 <b>대소문자를 구분</b>한다.
     * 즉 {@code "POPULAR"} 는 매칭되지 않고 {@link #LATEST} 로 떨어진다.
     * 컨트롤러가 받은 {@code sort} 쿼리 파라미터는 정규화 없이 리포지토리까지 그대로 전달되므로,
     * 클라이언트가 대문자로 보내면 의도와 다른 정렬이 나간다.
     * 관대한 매칭({@code equalsIgnoreCase})으로 바꾸는 것은 동작 변경이라 별도 이슈에서 다루며,
     * 그때 수정할 지점은 이 메서드 한 곳이다.
     *
     * @param value 정렬 키 문자열 (null 허용)
     * @return 일치하는 정렬 기준, 없으면 {@link #LATEST}
     */
    public static ClubSortType from(String value) {
        return Arrays.stream(values())
                .filter(sortType -> sortType.key.equals(value))
                .findFirst()
                .orElse(LATEST);
    }
}
