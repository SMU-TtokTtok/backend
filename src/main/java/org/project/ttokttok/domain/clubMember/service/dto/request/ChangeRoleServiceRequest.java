package org.project.ttokttok.domain.clubMember.service.dto.request;

import lombok.Builder;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;

@Builder
public record ChangeRoleServiceRequest(
        String username,
        String clubId,
        String memberId,
        MemberRole newRole
) {
    public static ChangeRoleServiceRequest of(String username,
                                              String clubId,
                                              String memberId,
                                              MemberRole newRole) {

        return ChangeRoleServiceRequest.builder()
                .username(username)
                .clubId(clubId)
                .memberId(memberId)
                .newRole(newRole)
                .build();
    }
}
