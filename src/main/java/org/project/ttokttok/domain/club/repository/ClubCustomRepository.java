package org.project.ttokttok.domain.club.repository;

import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailAdminQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.PopularClubCandidate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClubCustomRepository {
    // queryDsl 사용을 위한 인터페이스
    ClubDetailQueryResponse getClubIntroduction(String clubId, String email);

    // 무한스크롤을 위한 메서드
    List<ClubCardQueryResponse> getClubList(
            ClubCategory category,
            ClubType type,
            ClubUniv clubUniv,
            Boolean recruiting,
            List<ApplicableGrade> grades,
            int size,
            String cursor,
            String sort,
            String userEmail
    );

    // 인기 목록 후보 전체 조회. 점수 계산, 필터, 정렬, 페이징은 서비스에서 한다.
    List<PopularClubCandidate> findPopularClubCandidates(String userEmail);

    // 키워드 검색을 위한 메서드
    List<ClubCardQueryResponse> searchByKeyword(
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("cursor") String cursor,
            @Param("sort") String sort,
            @Param("userEmail") String userEmail
    );

    long countByKeyword(String keyword);

    ClubDetailAdminQueryResponse getAdminClubIntro(String clubId);
}
