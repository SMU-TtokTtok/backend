package org.project.ttokttok.domain.clubMember.service.dto.request;

import lombok.Builder;

@Builder
public record ClubMemberSearchRequest(
        String username,
        String clubId,
        String keyword
) {
    public static ClubMemberSearchRequest of(String username,
                                             String clubId,
                                             String keyword) {
        return ClubMemberSearchRequest.builder()
                .username(username)
                .clubId(clubId)
                .keyword(keyword)
                .build();
    }
}
