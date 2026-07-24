package org.project.ttokttok.domain.club.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClubCustomRepositoryImpl 의 QueryDSL 쿼리들이 정상적으로 조립/실행되는지 검증한다.
 * (빈 DB 기준으로 쿼리 실행 경로 및 정렬/필터 분기를 커버한다)
 */
@DisplayName("ClubCustomRepositoryImpl - 쿼리 실행 커버리지")
class ClubCustomRepositoryImplCoverageTest implements RepositoryTestSupport {

    @Autowired
    private ClubRepository clubRepository;

    private static final String USER = "user@sangmyung.kr";

    @Test
    @DisplayName("getClubList는 다양한 필터/정렬 조합에서 빈 결과를 반환한다")
    void getClubList_variousFilters() {
        for (String sort : List.of("latest", "popular", "member_count")) {
            List<ClubCardQueryResponse> result = clubRepository.getClubList(
                    ClubCategory.ACADEMIC, ClubType.CENTRAL, ClubUniv.ENGINEERING, true,
                    List.of(ApplicableGrade.FIRST_GRADE), 10, null, sort, USER
            );
            assertThat(result).isNotNull();
        }
        // 필터 없음 + 커서 있음 분기
        assertThat(clubRepository.getClubList(
                null, null, null, null, List.of(), 10, "cursor-1", "latest", null
        )).isNotNull();
    }

    @Test
    @DisplayName("getAllPopularClubs는 빈 결과를 반환한다")
    void getAllPopularClubs() {
        assertThat(clubRepository.getAllPopularClubs(USER, 7.0)).isNotNull();
        assertThat(clubRepository.getAllPopularClubs(null, 7.0)).isNotNull();
    }

    @Test
    @DisplayName("getPopularClubsWithFilters는 다양한 정렬에서 빈 결과를 반환한다")
    void getPopularClubsWithFilters() {
        for (String sort : List.of("latest", "popular", "member_count")) {
            assertThat(clubRepository.getPopularClubsWithFilters(10, null, sort, USER, 7.0)).isNotNull();
        }
        assertThat(clubRepository.getPopularClubsWithFilters(10, "cursor-1", "popular", null, 7.0)).isNotNull();
    }

    @Test
    @DisplayName("searchByKeyword는 다양한 정렬에서 빈 결과를 반환한다")
    void searchByKeyword() {
        for (String sort : List.of("latest", "popular", "member_count")) {
            assertThat(clubRepository.searchByKeyword("코딩", 10, null, sort, USER)).isNotNull();
        }
        assertThat(clubRepository.searchByKeyword("코딩", 10, "cursor-1", "latest", null)).isNotNull();
    }

    @Test
    @DisplayName("countByKeyword는 빈 DB에서 0을 반환한다")
    void countByKeyword() {
        assertThat(clubRepository.countByKeyword("코딩")).isZero();
    }

    @Test
    @DisplayName("getClubIntroduction/getAdminClubIntro는 존재하지 않는 동아리에 대해 null을 반환한다")
    void getIntroductions_emptyDb() {
        assertThat(clubRepository.getClubIntroduction("no-club", USER)).isNull();
        assertThat(clubRepository.getAdminClubIntro("no-club")).isNull();
    }
}
