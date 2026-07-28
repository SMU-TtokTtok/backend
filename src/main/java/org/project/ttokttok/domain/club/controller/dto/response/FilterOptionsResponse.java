package org.project.ttokttok.domain.club.controller.dto.response;

import org.project.ttokttok.domain.club.domain.enums.ClubUniv;

import java.util.Arrays;
import java.util.List;

/**
 * 동아리 목록 필터링 옵션 응답
 *
 * <p>기존에 {@code Map<String, Object>} 와 중첩 {@code Map.of()} 로 만들던 응답을 타입화한 것입니다.
 * JSON 구조(키 이름, 항목 순서, "전체" 옵션 포함 여부)는 이전과 동일합니다.
 *
 * @param types              동아리 분류 옵션
 * @param recruitingOptions  모집 여부 옵션
 * @param universities       대학 구분 옵션 (과동아리 선택 시 사용)
 */
public record FilterOptionsResponse(
        List<FilterOption> types,
        List<FilterOption> recruitingOptions,
        List<FilterOption> universities
) {

    public static FilterOptionsResponse create() {
        return new FilterOptionsResponse(
                typeOptions(),
                recruitingStatusOptions(),
                universityOptions()
        );
    }

    /**
     * 동아리 분류 옵션.
     *
     * <p>라벨을 {@code ClubType} enum 에서 가져오지 않는 이유: enum 의 라벨은
     * "중앙"/"연합"/"학과" 인 반면 이 필터 UI 는 "중앙동아리"/"연합동아리"/"과동아리" 를 쓴다.
     * 응답을 그대로 보존하기 위해 필터 전용 라벨을 여기서 정의한다.
     */
    private static List<FilterOption> typeOptions() {
        return List.of(
                FilterOption.all(),
                FilterOption.of("CENTRAL", "중앙동아리"),
                FilterOption.of("UNION", "연합동아리"),
                FilterOption.of("DEPARTMENT", "과동아리")
        );
    }

    private static List<FilterOption> recruitingStatusOptions() {
        return List.of(
                FilterOption.all(),
                FilterOption.of("true", "모집중"),
                FilterOption.of("false", "모집마감")
        );
    }

    /**
     * 대학 구분 옵션은 {@link ClubUniv} 에서 파생한다.
     * enum 에 값이 추가/변경되면 이 응답도 자동으로 따라간다.
     */
    private static List<FilterOption> universityOptions() {
        return Arrays.stream(ClubUniv.values())
                .map(univ -> FilterOption.of(univ.name(), univ.getUnivName()))
                .toList();
    }
}
