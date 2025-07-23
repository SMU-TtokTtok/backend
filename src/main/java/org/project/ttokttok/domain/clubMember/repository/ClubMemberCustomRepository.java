package org.project.ttokttok.domain.clubMember.repository;

import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberCountQueryResponse;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberPageQueryResponse;

public interface ClubMemberCustomRepository {
    ClubMemberPageQueryResponse findClubMemberPageByClubId(
            String clubId,
            int pageNum,
            int pageSize
    );

    ClubMemberCountQueryResponse countClubMembersByClubId(String clubId);
}
