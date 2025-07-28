package org.project.ttokttok.domain.club.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClubUniv {

    GLOBAL_AREA("글로벌지역학부"),

    DESIGN("디자인대학"),

    ENGINEERING("공대"),

    CONVERGENCE_TECHNOLOGY("융합기술대"),

    ARTS("예술대");

    final String univName;
}
