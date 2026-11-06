package org.project.ttokttok.domain.clubmember.service.dto.request;

public record ClubMemberPageRequest(
        String username,
        int page,
        int size
) {
}
