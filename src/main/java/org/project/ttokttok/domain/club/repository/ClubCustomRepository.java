package org.project.ttokttok.domain.club.repository;

import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;

import java.util.List;

public interface ClubCustomRepository {
    // queryDsl 사용을 위한 인터페이스
    ClubDetailQueryResponse getClubIntroduction(String clubId, String email);

    // 무한스크롤을 위한 메서드
    List<ClubCardQueryResponse> getClubList(
            ClubCategory category,
            ClubType type,
            Boolean recruiting,
            List<ApplicableGrade> grades,
            int size,
            String cursor,
            String sort,
            String userEmail
    );

    // 복합 점수 기반 인기 동아리 조회
    List<ClubCardQueryResponse> getPopularClubs(int offset, int limit, String userEmail, double minScore);

    List<ClubCardQueryResponse> getPopularClubsWithFilters(
            ClubCategory category,
            ClubType type,
            Boolean recruiting,
            int size,
            String cursor,
            String userEmail,
            double minScore
    );
}
