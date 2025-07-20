package org.project.ttokttok.domain.clubMember.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {
    PRESIDENT("회장"), // 회장
    VICE_PRESIDENT("부회장"), // 부회장
    EXECUTIVE("임원진"), // 임원진
    MEMBER("부원"); // 일반 부원

    final String memberRoleName;
}
