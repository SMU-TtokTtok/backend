package org.project.ttokttok.domain.club.repository;

import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailAdminQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
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
            Boolean recruiting,
            List<ApplicableGrade> grades,
            int size,
            String cursor,
            String sort,
            String userEmail
    );

    // 복합 점수 기반 인기 동아리 조회 - 페이지네이션 제거
    List<ClubCardQueryResponse> getAllPopularClubs(String userEmail, double minScore);

    // 인기 동아리 조회 - 필터링 및 페이지네이션
    List<ClubCardQueryResponse> getPopularClubsWithFilters(
            int size,
            String cursor,
            String sort,
            String userEmail,
            double minScore
    );

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
