package org.project.ttokttok.domain.clubMember.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberRoleTest {

    @Test
    @DisplayName("각 역할은 올바른 한글 이름을 가진다")
    void memberRoleName_success() {
        assertThat(MemberRole.PRESIDENT.getMemberRoleName()).isEqualTo("회장");
        assertThat(MemberRole.VICE_PRESIDENT.getMemberRoleName()).isEqualTo("부회장");
        assertThat(MemberRole.EXECUTIVE.getMemberRoleName()).isEqualTo("임원진");
        assertThat(MemberRole.MEMBER.getMemberRoleName()).isEqualTo("부원");
    }

    @Test
    @DisplayName("MemberRole은 4개의 상수를 가진다")
    void memberRole_hasFourValues() {
        assertThat(MemberRole.values()).hasSize(4);
    }
}
