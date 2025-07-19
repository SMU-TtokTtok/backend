package org.project.ttokttok.domain.clubMember.service.dto.request;

import lombok.Builder;

@Builder
public record DeleteMemberServiceRequest(
        String username,
        String clubId,
        String memberId
) {
    public static DeleteMemberServiceRequest of(String username,
                                                String clubId,
                                                String memberId) {
        return DeleteMemberServiceRequest.builder()
                .username(username)
                .clubId(clubId)
                .memberId(memberId)
                .build();
    }
}
