package org.project.ttokttok.domain.club.repository;

import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;

public interface ClubCustomRepository {
    // queryDsl 사용을 위한 인터페이스
    ClubDetailQueryResponse getClubIntroduction(String clubId, String email);
}
