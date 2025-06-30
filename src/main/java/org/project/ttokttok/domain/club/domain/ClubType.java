package org.project.ttokttok.domain.club.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClubType {
    // 동아리 유형
    CENTRAL("중앙"),
    UNION("연합"),
    DEPARTMENT("학과");

    final String type;
}
