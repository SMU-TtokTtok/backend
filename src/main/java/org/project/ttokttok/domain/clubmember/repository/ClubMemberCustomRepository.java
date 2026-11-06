package org.project.ttokttok.domain.clubmember.repository;

import org.project.ttokttok.domain.clubmember.repository.dto.ClubMemberCountQueryResponse;
import org.project.ttokttok.domain.clubmember.repository.dto.ClubMemberPageQueryResponse;

public interface ClubMemberCustomRepository {
    ClubMemberPageQueryResponse findClubMemberPageByClubId(
            String clubId,
            int pageNum,
            int pageSize
    );

    ClubMemberCountQueryResponse countClubMembersByClubId(String clubId);
}
