package org.project.ttokttok.domain.club.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.repository.ClubMemberRepository;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ClubCustomRepositoryImpl} 의 정렬 동작 검증.
 *
 * <p>정렬 키 문자열 분기를 {@code ClubSortType} enum switch로 바꾸면서 QueryDSL 경로와
 * native SQL 경로 양쪽이 함께 수정됐다. {@code ClubCustomRepositoryImplCoverageTest} 는
 * 빈 DB에서 쿼리가 조립/실행되는지만 확인하므로, <b>실제 정렬 순서</b>는 여기서 검증한다.
 *
 * <p>정렬 기준이 서로 다른 결과를 내도록 픽스처를 잡았다.
 * <ul>
 *   <li>{@code 조회수많은동아리} — 멤버 0명, 조회수 100 → 인기 점수 70.0</li>
 *   <li>{@code 멤버많은동아리} — 멤버 2명, 조회수 0 → 인기 점수 1.4</li>
 * </ul>
 * 따라서 {@code popular} 와 {@code member_count} 는 <b>반대 순서</b>가 나와야 하며,
 * 두 분기가 뒤바뀌면 즉시 실패한다.
 */
@DisplayName("ClubCustomRepositoryImpl - 정렬 순서")
class ClubCustomRepositorySortingTest implements RepositoryTestSupport {

    private static final String USER = "user@sangmyung.kr";
    private static final String HIGH_VIEW_CLUB = "조회수많은동아리";
    private static final String MANY_MEMBER_CLUB = "멤버많은동아리";

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private EntityManager em;

    private int adminSequence;

    @BeforeEach
    void setUp() {
        adminSequence = 0;
    }

    /**
     * 동아리마다 별도의 관리자를 만든다.
     * {@code Club.admin} 은 {@code @OneToOne} 이라 {@code clubs.admin_id} 에 유니크 제약이 걸려 있어
     * 한 관리자가 여러 동아리를 가질 수 없다.
     */
    private Admin givenAdmin() {
        int sequence = adminSequence++;

        return adminRepository.save(Admin.adminJoin(
                "sortadmin" + sequence, "password123!", "sort-admin" + sequence + "@sangmyung.kr"));
    }

    /** 조회수와 멤버 수를 지정해 동아리를 만든다. */
    private Club givenClub(String name, int viewCount, int memberCount) {
        Club club = clubRepository.save(Club.builder()
                .admin(givenAdmin())
                .clubName(name)
                .clubUniv(ClubUniv.ENGINEERING)
                .build());

        // 조회수 증가는 엔티티 메서드가 아니라 원자적 UPDATE 로만 가능하다 (#346).
        // 영속성 컨텍스트를 우회하므로 뒤이어 club 을 save 할 필요가 없다.
        for (int i = 0; i < viewCount; i++) {
            clubRepository.increaseViewCount(club.getId());
        }

        for (int i = 0; i < memberCount; i++) {
            clubMemberRepository.save(ClubMember.create(
                    club, name + "-멤버" + i, MemberRole.MEMBER, Grade.FIRST_GRADE,
                    "컴퓨터공학과", name + i + "@sangmyung.kr", "010-0000-0000", Gender.MALE));
        }

        return club;
    }

    /** 인기순과 멤버순이 반대 결과를 내도록 두 동아리를 만든다. */
    private void givenContrastingClubs() {
        givenClub(HIGH_VIEW_CLUB, 100, 0);
        givenClub(MANY_MEMBER_CLUB, 0, 2);
        flushAndClear();
    }

    private List<String> namesOf(List<ClubCardQueryResponse> clubs) {
        return clubs.stream().map(ClubCardQueryResponse::name).toList();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("getClubList(): QueryDSL 정렬 경로")
    class ClubListSorting {

        @Test
        @DisplayName("popular 정렬은 인기 점수 내림차순으로 반환한다")
        void popularSort_ordersByScoreDesc() {
            givenContrastingClubs();

            List<ClubCardQueryResponse> result = clubRepository.getClubList(
                    null, null, null, null, List.of(), 10, null, "popular", USER);

            assertThat(namesOf(result)).containsExactly(HIGH_VIEW_CLUB, MANY_MEMBER_CLUB);
        }

        @Test
        @DisplayName("member_count 정렬은 멤버 수 내림차순으로 반환한다 - popular와 반대 순서다")
        void memberCountSort_ordersByMemberCountDesc() {
            givenContrastingClubs();

            List<ClubCardQueryResponse> result = clubRepository.getClubList(
                    null, null, null, null, List.of(), 10, null, "member_count", USER);

            assertThat(namesOf(result)).containsExactly(MANY_MEMBER_CLUB, HIGH_VIEW_CLUB);
        }

        @Test
        @DisplayName("latest 정렬은 점수나 멤버 수와 무관하며 호출마다 같은 순서를 반환한다")
        void latestSort_isIndependentOfScoreAndStable() {
            givenContrastingClubs();

            List<ClubCardQueryResponse> first = clubRepository.getClubList(
                    null, null, null, null, List.of(), 10, null, "latest", USER);
            List<ClubCardQueryResponse> second = clubRepository.getClubList(
                    null, null, null, null, List.of(), 10, null, "latest", USER);

            assertThat(namesOf(first))
                    .containsExactlyInAnyOrder(HIGH_VIEW_CLUB, MANY_MEMBER_CLUB);
            assertThat(namesOf(second)).isEqualTo(namesOf(first));
        }

        @Test
        @DisplayName("알 수 없는 정렬 값과 null은 latest와 동일하게 동작한다")
        void unknownSort_behavesAsLatest() {
            givenContrastingClubs();

            List<String> latest = namesOf(clubRepository.getClubList(
                    null, null, null, null, List.of(), 10, null, "latest", USER));
            List<String> unknown = namesOf(clubRepository.getClubList(
                    null, null, null, null, List.of(), 10, null, "알 수 없는 정렬", USER));
            List<String> nullSort = namesOf(clubRepository.getClubList(
                    null, null, null, null, List.of(), 10, null, null, USER));

            assertThat(unknown).isEqualTo(latest);
            assertThat(nullSort).isEqualTo(latest);
        }

        @Test
        @DisplayName("정렬 키는 대소문자를 구분한다 - POPULAR는 latest로 떨어진다 (기존 동작)")
        void sortKeyIsCaseSensitive() {
            givenContrastingClubs();

            List<String> upperCase = namesOf(clubRepository.getClubList(
                    null, null, null, null, List.of(), 10, null, "POPULAR", USER));
            List<String> latest = namesOf(clubRepository.getClubList(
                    null, null, null, null, List.of(), 10, null, "latest", USER));

            assertThat(upperCase).isEqualTo(latest);
        }

        @Test
        @DisplayName("size+1건까지 조회해 hasNext 판별을 지원한다")
        void fetchesOneMoreThanSize() {
            givenClub("동아리A", 3, 0);
            givenClub("동아리B", 2, 0);
            givenClub("동아리C", 1, 0);
            flushAndClear();

            List<ClubCardQueryResponse> result = clubRepository.getClubList(
                    null, null, null, null, List.of(), 2, null, "popular", USER);

            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("searchByKeyword(): 검색 결과 정렬")
    class SearchSorting {

        @Test
        @DisplayName("검색 결과에도 popular 정렬이 적용된다")
        void searchAppliesPopularSort() {
            givenClub("코딩동아리낮음", 1, 0);
            givenClub("코딩동아리높음", 100, 0);
            givenClub("전혀다른동아리", 500, 0);
            flushAndClear();

            List<ClubCardQueryResponse> result = clubRepository.searchByKeyword(
                    "코딩동아리", 10, null, "popular", USER);

            assertThat(namesOf(result)).containsExactly("코딩동아리높음", "코딩동아리낮음");
        }

        @Test
        @DisplayName("검색 결과에도 member_count 정렬이 적용된다")
        void searchAppliesMemberCountSort() {
            givenContrastingClubs();

            List<ClubCardQueryResponse> result = clubRepository.searchByKeyword(
                    "동아리", 10, null, "member_count", USER);

            assertThat(namesOf(result)).containsExactly(MANY_MEMBER_CLUB, HIGH_VIEW_CLUB);
        }
    }

    @Nested
    @DisplayName("getPopularClubsWithFilters(): native SQL 정렬 경로")
    class PopularClubsNativeSorting {

        @Test
        @DisplayName("popular 정렬은 인기 점수 내림차순으로 반환한다")
        void popularSort_ordersByScoreDesc() {
            givenContrastingClubs();

            List<ClubCardQueryResponse> result =
                    clubRepository.getPopularClubsWithFilters(10, null, "popular", USER, 0.0);

            assertThat(namesOf(result)).containsExactly(HIGH_VIEW_CLUB, MANY_MEMBER_CLUB);
        }

        @Test
        @DisplayName("member_count 정렬은 멤버 수 내림차순으로 반환한다 - popular와 반대 순서다")
        void memberCountSort_ordersByMemberCountDesc() {
            givenContrastingClubs();

            List<ClubCardQueryResponse> result =
                    clubRepository.getPopularClubsWithFilters(10, null, "member_count", USER, 0.0);

            assertThat(namesOf(result)).containsExactly(MANY_MEMBER_CLUB, HIGH_VIEW_CLUB);
        }

        @Test
        @DisplayName("모든 정렬 값에서 SQL이 깨지지 않고 같은 동아리 집합을 반환한다")
        void everySortKeyExecutes() {
            // native SQL 문자열을 switch로 조립하도록 바꾼 경로다.
            // ORDER BY 절이 깨지면 여기서 SQL 예외로 드러난다.
            givenContrastingClubs();

            for (String sort : List.of("popular", "member_count", "latest", "알 수 없는 값")) {
                List<ClubCardQueryResponse> result =
                        clubRepository.getPopularClubsWithFilters(10, null, sort, USER, 0.0);

                assertThat(namesOf(result))
                        .as("sort=%s 는 정렬만 다를 뿐 같은 동아리 집합을 반환한다", sort)
                        .containsExactlyInAnyOrder(HIGH_VIEW_CLUB, MANY_MEMBER_CLUB);
            }
        }

        @Test
        @DisplayName("minScore 미만인 동아리는 제외된다")
        void filtersOutBelowMinScore() {
            givenContrastingClubs();

            // 점수: 조회수많은동아리 = 100 * 0.7 = 70.0, 멤버많은동아리 = 2 * 0.7 = 1.4
            List<ClubCardQueryResponse> result =
                    clubRepository.getPopularClubsWithFilters(10, null, "popular", USER, 10.0);

            assertThat(namesOf(result)).containsExactly(HIGH_VIEW_CLUB);
        }
    }
}
