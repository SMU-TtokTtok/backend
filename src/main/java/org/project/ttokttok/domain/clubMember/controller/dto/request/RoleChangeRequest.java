package org.project.ttokttok.domain.clubMember.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;

public record RoleChangeRequest(
        @NotNull(message = "바꾸려는 역할은 Null일 수 없습니다.")
        MemberRole role
) {
}
