package org.project.ttokttok.domain.club.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClubCategory {

    // 동아리 카테고리 (6개)
    // 전체는 enum 에 포함하지 않고, API 에서 필터 없이 처리
    VOLUNTEER("봉사"),
    CULTURE("문화"),
    ACADEMIC("학술"),
    SPORTS("스포츠"),      // "체육" -> "스포츠" 로 변경
    RELIGION("종교"),
    ETC("기타");

    // 제거된 항목들 : ART("예술") - 제거, SOCIAL("친목") - 제거

    final String category;
}