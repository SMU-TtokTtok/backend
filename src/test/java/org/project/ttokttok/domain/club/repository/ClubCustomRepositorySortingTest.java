package org.project.ttokttok.domain.club.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.service.ClubUserService;
import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.domain.clubmember.domain.ClubMember;
import org.project.ttokttok.domain.clubmember.domain.MemberRole;
import org.project.ttokttok.domain.clubmember.repository.ClubMemberRepository;
import org.project.ttokttok.global.policy.PopularityPolicy;
import org.project.ttokttok.support.RepositoryTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
 *   <li>{@code 멤버많은동아리} — 멤버 11명, 조회수 0 → 인기 점수 7.7</li>
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

    @Autowired
    private ClubUserService clubUserService;

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
                "sortadmin" + sequence, "password123!"));
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

    /**
     * 인기순과 멤버순이 반대 결과를 내도록 두 동아리를 만든다.
     * 인기 목록 최소 점수(7.0)를 넘도록 점수는 조회수많은동아리 70.0, 멤버많은동아리 7.7 이다.
     */
    private void givenContrastingClubs() {
        givenClub(HIGH_VIEW_CLUB, 100, 0);
        givenClub(MANY_MEMBER_CLUB, 0, 11);
        flushAndClear();
    }

    private List<String> namesOf(List<ClubCardQueryResponse> clubs) {
        return clubs.stream().map(ClubCardQueryResponse::name).toList();
    }

    private List<String> namesOf(ClubListServiceResponse response) {
        return response.clubs().stream().map(ClubCardServiceResponse::name).toList();
    }

    private void setCreatedAt(Club club, LocalDateTime createdAt) {
        em.createNativeQuery("UPDATE clubs SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", club.getId())
                .executeUpdate();
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
    @DisplayName("getPopularClubsWithFilters(): 인기 목록 정렬")
    class PopularClubsNativeSorting {

        @Test
        @DisplayName("popular 정렬은 인기 점수 내림차순으로 반환한다")
        void popularSort_ordersByScoreDesc() {
            givenContrastingClubs();

            ClubListServiceResponse result =
                    clubUserService.getPopularClubsWithFilters(10, null, "popular", USER);

            assertThat(namesOf(result)).containsExactly(HIGH_VIEW_CLUB, MANY_MEMBER_CLUB);
        }

        @Test
        @DisplayName("member_count 정렬은 멤버 수 내림차순으로 반환한다 - popular와 반대 순서다")
        void memberCountSort_ordersByMemberCountDesc() {
            givenContrastingClubs();

            ClubListServiceResponse result =
                    clubUserService.getPopularClubsWithFilters(10, null, "member_count", USER);

            assertThat(namesOf(result)).containsExactly(MANY_MEMBER_CLUB, HIGH_VIEW_CLUB);
        }

        @Test
        @DisplayName("모든 정렬 값에서 SQL이 깨지지 않고 같은 동아리 집합을 반환한다")
        void everySortKeyExecutes() {
            // 정렬 키마다 비교 기준이 갈리는 경로다. 어느 분기가 깨져도 여기서 드러난다.
            givenContrastingClubs();

            for (String sort : List.of("popular", "member_count", "latest", "알 수 없는 값")) {
                ClubListServiceResponse result =
                        clubUserService.getPopularClubsWithFilters(10, null, sort, USER);

                assertThat(namesOf(result))
                        .as("sort=%s 는 정렬만 다를 뿐 같은 동아리 집합을 반환한다", sort)
                        .containsExactlyInAnyOrder(HIGH_VIEW_CLUB, MANY_MEMBER_CLUB);
            }
        }

        @Test
        @DisplayName("latest 정렬은 등록 시각이 늦은 동아리가 먼저 온다")
        void latestSort_ordersByCreatedAtDesc() {
            // 만든 순서와 반대로 시각을 줘서 id 나 저장 순서로는 우연히 맞지 않게 한다.
            Club newest = givenClub("최신동아리", 20, 0);
            Club middle = givenClub("중간동아리", 20, 0);
            Club oldest = givenClub("오래된동아리", 20, 0);
            flushAndClear();
            setCreatedAt(newest, LocalDateTime.of(2026, 3, 1, 0, 0));
            setCreatedAt(middle, LocalDateTime.of(2026, 2, 1, 0, 0));
            setCreatedAt(oldest, LocalDateTime.of(2026, 1, 1, 0, 0));

            ClubListServiceResponse result =
                    clubUserService.getPopularClubsWithFilters(10, null, "latest", USER);

            assertThat(namesOf(result)).containsExactly("최신동아리", "중간동아리", "오래된동아리");
        }

        @Test
        @DisplayName("minScore 미만인 동아리는 제외된다")
        void filtersOutBelowMinScore() {
            givenContrastingClubs();
            givenClub("점수낮은동아리", 0, 1);
            flushAndClear();

            // 점수: 조회수많은동아리 = 70.0, 멤버많은동아리 = 7.7, 점수낮은동아리 = 1 * 0.7 = 0.7 (최소 점수 7.0 미만)
            ClubListServiceResponse result =
                    clubUserService.getPopularClubsWithFilters(10, null, "popular", USER);

            assertThat(namesOf(result)).containsExactly(HIGH_VIEW_CLUB, MANY_MEMBER_CLUB);
        }
    }

    /**
     * 커서는 서비스가 만들고(nextCursor) 다시 받으므로 서비스를 거쳐 끝까지 넘겨 본다.
     * 서비스 메서드 시그니처는 커서 형식이 바뀌어도 유지된다.
     */
    @Nested
    @DisplayName("getPopularClubsWithFilters(): 커서로 끝까지 넘기기 (#464)")
    class PopularClubsCursorPaging {

        private static final int CLUB_COUNT = 6;

        /**
         * 인기 점수와 멤버 수가 모두 서로 다르고 두 정렬이 반대 순서가 되도록 만든다.
         * 점수 = 0.7 × (조회수 + 멤버 수) = 0.7 × (15 + i) 라서 모두 PopularityPolicy 최소 점수(7.0) 이상이다.
         * 동아리 id 는 UUID 라 점수·멤버 수와 무관한 임의 순서다.
         */
        private void givenPagedClubs() {
            for (int i = 0; i < CLUB_COUNT; i++) {
                givenClub("페이징동아리" + i, 10 + 2 * i, 5 - i);
            }
            flushAndClear();
        }

        /** nextCursor 를 따라 마지막 페이지까지 받아 이름을 이어 붙인다. */
        private List<String> fetchAllPages(String sort, int size) {
            List<String> names = new ArrayList<>();
            String cursor = null;

            for (int page = 0; page <= CLUB_COUNT; page++) {
                ClubListServiceResponse response =
                        clubUserService.getPopularClubsWithFilters(size, cursor, sort, USER);
                response.clubs().forEach(club -> names.add(club.name()));

                if (!response.hasNext()) {
                    return names;
                }
                cursor = response.nextCursor();
            }
            throw new AssertionError("hasNext 가 끝나지 않는다: " + names);
        }

        @ParameterizedTest(name = "sort={0}")
        @ValueSource(strings = {"popular", "member_count", "latest"})
        @DisplayName("size=2로 끝까지 넘긴 결과가 한 번에 받은 결과와 같다 - 누락·중복 없음")
        void pagedResultEqualsSinglePage(String sort) {
            givenPagedClubs();

            List<String> singlePage = fetchAllPages(sort, 100);
            List<String> paged = fetchAllPages(sort, 2);

            assertThat(singlePage).hasSize(CLUB_COUNT);
            assertThat(paged).isEqualTo(singlePage);
        }
    }
}
