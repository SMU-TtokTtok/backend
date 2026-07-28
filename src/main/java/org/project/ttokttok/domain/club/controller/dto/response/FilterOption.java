package org.project.ttokttok.domain.club.controller.dto.response;

/**
 * 필터 드롭다운 항목 하나
 *
 * @param value 프론트에서 쿼리 파라미터로 되돌려 보내는 값 ("전체"는 문자열 {@code "null"})
 * @param label 화면에 표시되는 한글 라벨
 */
public record FilterOption(String value, String label) {

    /** 필터를 걸지 않는 "전체" 옵션의 value 값 */
    private static final String ALL_VALUE = "null";

    public static FilterOption all() {
        return new FilterOption(ALL_VALUE, "전체");
    }

    public static FilterOption of(String value, String label) {
        return new FilterOption(value, label);
    }
}
