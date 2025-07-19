package org.project.ttokttok.domain.clubMember.service.dto.request;

public record ClubMemberPageRequest(
        String username,
        int page,
        int size
) {
}
