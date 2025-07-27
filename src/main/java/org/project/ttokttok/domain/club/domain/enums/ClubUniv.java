package org.project.ttokttok.domain.club.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClubUniv {
    // 동아리 소속 대학교

    // 글로벌지역학부
    GLOBAL_AREA("글로벌지역학부"),

    // 디자인대학
    DESIGN("디자인대학"),

    // 공대
    ENGINEERING("공대"),

    // 융합기술대
    CONVERGENCE_TECHNOLOGY("융합기술대"),

    // 예술대
    ARTS("예술대");

    final String univName;
}
