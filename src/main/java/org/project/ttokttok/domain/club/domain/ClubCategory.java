package org.project.ttokttok.domain.club.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClubCategory {

    // 동아리 카테고리
    VOLUNTEER("봉사"),
    ART("예술"),
    CULTURE("문화"),
    ACADEMIC("학술"),
    SOCIAL("친목"),
    SPORTS("체육"),
    RELIGION("종교"),
    ETC("기타");

    final String category;
}