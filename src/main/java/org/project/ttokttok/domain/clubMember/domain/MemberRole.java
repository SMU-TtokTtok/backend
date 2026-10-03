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

    // 현재 역할이 회장 혹은 부회장인지 판단.
    public boolean isExclusive() {
        return this == PRESIDENT || this == VICE_PRESIDENT;
    }

    // 입력받은 역할 명에 따른 열거형 반환 - 허용역할이 2개이기에 if문으로 처리
    public static MemberRole fromRegistrationRole(String roleName) {
        if (EXECUTIVE.name().equalsIgnoreCase(roleName)) {
            return EXECUTIVE;
        }

        if (MEMBER.name().equalsIgnoreCase(roleName)) {
            return MEMBER;
        }

        throw new IllegalArgumentException("잘못된 역할명입니다: " + roleName);
    }
}
