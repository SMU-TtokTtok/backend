package org.project.ttokttok.domain.clubmember.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import org.project.ttokttok.domain.clubmember.domain.MemberRole;

public record RoleChangeRequest(
        @NotNull(message = "바꾸려는 역할은 Null일 수 없습니다.")
        MemberRole role
) {
}
