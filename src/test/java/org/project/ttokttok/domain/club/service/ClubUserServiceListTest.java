package org.project.ttokttok.domain.club.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.PopularClubCandidate;
import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.domain.favorite.repository.FavoriteRepository;
import org.project.ttokttok.global.policy.PopularityPolicy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubUserService - 목록/인기/검색 조회")
class ClubUserServiceListTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private PopularClubCandidateCache popularClubCandidateCache;

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private ClubUserService clubUserService;

    private static final String USER = "user@sangmyung.kr";

    private ClubCardQueryResponse card(String id, LocalDate deadline) {
        return new ClubCardQueryResponse(
                id, "동아리" + id, ClubType.CENTRAL, ClubCategory.ACADEMIC,
                null, "요약", "http://img", 10, true, false, false, deadline
        );
    }

    /** 인기 점수 = 10 × 0.7 + views × 0.7. views 가 클수록 앞선다. */
    private PopularClubCandidate candidate(String id, long views) {
        return new PopularClubCandidate(card(id, null), 0, views, LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    @DisplayName("동아리 목록 조회 시 size보다 많으면 hasNext=true, nextCursor가 설정된다")
    void getClubList_hasNext() {
        // given: size=2인데 3개 반환 -> hasNext true, 마지막 잘림
        given(clubRepository.getClubList(any(), any(), any(), any(), any(), anyInt(), any(), anyString(), anyString()))
                .willReturn(List.of(
                        card("1", LocalDate.now().plusDays(3)),   // 마감 임박
                        card("2", null),
                        card("3", null)
                ));

        // when
        ClubListServiceResponse response = clubUserService.getClubList(
                ClubCategory.ACADEMIC, ClubType.CENTRAL, null, true, List.of(), 2, null, "latest", USER
        );

        // then
        assertThat(response.hasNext()).isTrue();
        assertThat(response.clubs()).hasSize(2);
        assertThat(response.nextCursor()).isEqualTo("2");
        assertThat(response.clubs().get(0).isDeadlineImminent()).isTrue();
        assertThat(response.clubs().get(1).isDeadlineImminent()).isFalse();
    }

    @Test
    @DisplayName("동아리 목록 조회 시 size 이하면 hasNext=false, nextCursor는 null이다")
    void getClubList_noNext() {
        given(clubRepository.getClubList(any(), any(), any(), any(), any(), anyInt(), any(), anyString(), anyString()))
                .willReturn(List.of(card("1", null)));

        ClubListServiceResponse response = clubUserService.getClubList(
                null, null, null, null, List.of(), 5, null, "latest", USER
        );

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.clubs()).hasSize(1);
    }

    @Test
    @DisplayName("마감일이 8일 이후면 마감 임박이 아니다")
    void getClubList_notImminentWhenFar() {
        given(clubRepository.getClubList(any(), any(), any(), any(), any(), anyInt(), any(), anyString(), anyString()))
                .willReturn(List.of(card("1", LocalDate.now().plusDays(8))));

        ClubListServiceResponse response = clubUserService.getClubList(
                null, null, null, null, List.of(), 5, null, "latest", USER
        );

        assertThat(response.clubs().get(0).isDeadlineImminent()).isFalse();
    }

    @Test
    @DisplayName("전체 인기 동아리 목록은 점수 높은 순이고 최소 점수 미만은 뺀다")
    void getAllPopularClubs() {
        // 멤버 1명, 조회 0 → 점수 0.7 로 최소 점수(7.0) 미만
        PopularClubCandidate belowMinScore = new PopularClubCandidate(
                new ClubCardQueryResponse("3", "동아리3", ClubType.CENTRAL, ClubCategory.ACADEMIC,
                        null, "요약", "http://img", 1, true, false, false, null),
                0, 0, LocalDateTime.of(2026, 1, 1, 0, 0));
        given(popularClubCandidateCache.get())
                .willReturn(List.of(candidate("1", 20), candidate("2", 30), belowMinScore));

        ClubListServiceResponse response = clubUserService.getAllPopularClubs(USER);

        assertThat(response.clubs()).extracting(ClubCardServiceResponse::id).containsExactly("2", "1");
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("전체 인기 동아리 목록은 배너 최대 개수까지만 돌려준다")
    void getAllPopularClubs_limitsToBannerSize() {
        List<PopularClubCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < PopularityPolicy.BANNER_SIZE + 3; i++) {
            candidates.add(candidate("c" + i, 20 + i));
        }
        given(popularClubCandidateCache.get()).willReturn(candidates);

        ClubListServiceResponse response = clubUserService.getAllPopularClubs(USER);

        // 조회수가 클수록 앞서므로 가장 큰 c12 부터 BANNER_SIZE 개다
        List<String> expected = new ArrayList<>();
        for (int i = candidates.size() - 1; i >= candidates.size() - PopularityPolicy.BANNER_SIZE; i--) {
            expected.add("c" + i);
        }
        assertThat(response.clubs()).extracting(ClubCardServiceResponse::id).containsExactlyElementsOf(expected);
    }

    @Test
    @DisplayName("필터가 있는 인기 동아리 목록을 무한스크롤 조회한다")
    void getPopularClubsWithFilters_hasNext() {
        given(popularClubCandidateCache.get())
                .willReturn(List.of(candidate("1", 30), candidate("2", 20), candidate("3", 10)));

        ClubListServiceResponse response = clubUserService.getPopularClubsWithFilters(2, null, "popular", USER);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.clubs()).hasSize(2);
        assertThat(response.nextCursor()).isEqualTo("2");
    }

    @Test
    @DisplayName("커서를 주면 그 동아리 다음부터 이어서 돌려준다")
    void getPopularClubsWithFilters_continuesAfterCursor() {
        given(popularClubCandidateCache.get())
                .willReturn(List.of(candidate("1", 30), candidate("2", 20), candidate("3", 10)));

        ClubListServiceResponse response = clubUserService.getPopularClubsWithFilters(2, "2", "popular", USER);

        assertThat(response.clubs()).extracting(ClubCardServiceResponse::id).containsExactly("3");
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("인기 목록의 북마크는 캐시가 아니라 로그인 사용자의 즐겨찾기로 정한다")
    void popularClubs_bookmarkedComesFromUserFavorites() {
        given(popularClubCandidateCache.get())
                .willReturn(List.of(candidate("1", 30), candidate("2", 20), candidate("3", 10)));
        given(favoriteRepository.findClubIdsByUserEmail(USER)).willReturn(Set.of("2"));

        ClubListServiceResponse banner = clubUserService.getAllPopularClubs(USER);
        ClubListServiceResponse page = clubUserService.getPopularClubsWithFilters(5, null, "popular", USER);
        ClubListServiceResponse anonymous = clubUserService.getAllPopularClubs(null);

        assertThat(banner.clubs()).extracting(ClubCardServiceResponse::id, ClubCardServiceResponse::bookmarked)
                .containsExactly(tuple("1", false), tuple("2", true), tuple("3", false));
        assertThat(page.clubs()).extracting(ClubCardServiceResponse::bookmarked).containsExactly(false, true, false);
        assertThat(anonymous.clubs()).extracting(ClubCardServiceResponse::bookmarked).containsOnly(false);
    }

    @Test
    @DisplayName("키워드로 동아리를 검색하면 총 개수와 결과를 반환한다")
    void searchClubs() {
        given(clubRepository.countByKeyword("코딩")).willReturn(5L);
        given(clubRepository.searchByKeyword(eq("코딩"), anyInt(), any(), anyString(), anyString()))
                .willReturn(List.of(card("1", null), card("2", null), card("3", null)));

        ClubListServiceResponse response = clubUserService.searchClubs("코딩", "member_count", null, 2, USER);

        assertThat(response.totalCount()).isEqualTo(5L);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.clubs()).hasSize(2);
        assertThat(response.nextCursor()).isEqualTo("2");
    }
}
