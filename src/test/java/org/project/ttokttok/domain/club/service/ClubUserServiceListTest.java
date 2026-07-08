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
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.global.config.ClubPopularityConfig;
import org.project.ttokttok.infrastructure.s3.service.S3Service;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    private S3Service s3Service;

    @Mock
    private ClubPopularityConfig popularityConfig;

    @InjectMocks
    private ClubUserService clubUserService;

    private static final String USER = "user@sangmyung.kr";

    private ClubCardQueryResponse card(String id, LocalDate deadline) {
        return new ClubCardQueryResponse(
                id, "동아리" + id, ClubType.CENTRAL, ClubCategory.ACADEMIC,
                null, "요약", "http://img", 10, true, false, deadline
        );
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
    @DisplayName("전체 인기 동아리 목록을 조회한다")
    void getAllPopularClubs() {
        given(popularityConfig.getMinScore()).willReturn(7.0);
        given(clubRepository.getAllPopularClubs(eq(USER), eq(7.0)))
                .willReturn(List.of(card("1", null), card("2", null)));

        ClubListServiceResponse response = clubUserService.getAllPopularClubs(USER);

        assertThat(response.clubs()).hasSize(2);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("필터가 있는 인기 동아리 목록을 무한스크롤 조회한다")
    void getPopularClubsWithFilters_hasNext() {
        given(popularityConfig.getMinScore()).willReturn(7.0);
        given(clubRepository.getPopularClubsWithFilters(anyInt(), any(), anyString(), anyString(), eq(7.0)))
                .willReturn(List.of(card("1", null), card("2", null), card("3", null)));

        ClubListServiceResponse response = clubUserService.getPopularClubsWithFilters(2, null, "popular", USER);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.clubs()).hasSize(2);
        assertThat(response.nextCursor()).isEqualTo("2");
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
