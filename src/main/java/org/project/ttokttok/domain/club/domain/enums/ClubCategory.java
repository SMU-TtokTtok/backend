package org.project.ttokttok.domain.club.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClubCategory {

    // 동아리 카테고리 (8개)
    // 전체는 enum 에 포함하지 않고, API 에서 필터 없이 처리
    // *** FIXME: 프론트 뷰에 맞추어 수정할 것 ***
    // 예술,친목 추가 필요.

    VOLUNTEER("봉사"),
    CULTURE("문화"),
    ACADEMIC("학술"),
    SPORTS("체육"),
    RELIGION("종교"),
    ARTS("예술"),
    SOCIAL("친목"),
    ETC("기타");

    final String category;
}