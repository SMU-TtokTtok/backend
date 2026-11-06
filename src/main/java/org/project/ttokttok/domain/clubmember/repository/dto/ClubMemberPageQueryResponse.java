package org.project.ttokttok.domain.clubmember.repository.dto;

import lombok.Builder;
import org.project.ttokttok.domain.clubmember.domain.ClubMember;

import java.util.List;

@Builder
public record ClubMemberPageQueryResponse(
        int currentPage,
        int totalPage,
        int totalCount,
        List<ClubMember> clubMembers
) {
}
