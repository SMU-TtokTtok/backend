package org.project.ttokttok.domain.clubMember.repository.dto;

import lombok.Builder;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;

import java.util.List;

@Builder
public record ClubMemberPageQueryResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        List<ClubMember> clubMembers
) {
}
