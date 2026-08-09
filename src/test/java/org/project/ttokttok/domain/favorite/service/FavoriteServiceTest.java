package org.project.ttokttok.domain.favorite.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.applyform.repository.dto.ClubRecruitmentQueryDto;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
import org.project.ttokttok.domain.clubMember.repository.ClubMemberRepository;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberCountQueryDto;
import org.project.ttokttok.domain.favorite.domain.Favorite;
import org.project.ttokttok.domain.favorite.repository.FavoriteRepository;
import org.project.ttokttok.domain.favorite.repository.dto.ClubFavoriteCountQueryDto;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteListServiceRequest;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteToggleServiceRequest;
import org.project.ttokttok.domain.favorite.service.dto.response.FavoriteListServiceResponse;
import org.project.ttokttok.domain.favorite.service.dto.response.FavoriteToggleServiceResponse;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.exception.UserNotFoundException;
import org.project.ttokttok.domain.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @InjectMocks
    private FavoriteService favoriteService;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplyFormRepository applyFormRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private PopularityCalculator popularityCalculator;

    /** 모집중인 지원폼이 하나도 없는 상태를 만든다. */
    private void givenNoRecruitingForm() {
        given(applyFormRepository.findRecruitingFormsByClubIds(any())).willReturn(Collections.emptyList());
    }

    /** 멤버 수 집계 결과가 비어 있는 상태를 만든다 (모든 동아리 멤버 수 0). */
    private void givenNoClubMembers() {
        given(clubMemberRepository.countClubMembersForEach(any())).willReturn(Collections.emptyList());
    }

    @Nested
    @DisplayName("toggleFavorite 메서드")
    class ToggleFavoriteTest {

        @Test
        @DisplayName("즐겨찾기가 존재하지 않으면 새로 추가한다")
        void toggleFavorite_Add() {
            // given
            String userEmail = "test@test.com";
            String clubId = "club-1";
            FavoriteToggleServiceRequest request = FavoriteToggleServiceRequest.of(userEmail, clubId);

            Club club = mock(Club.class);
            User user = mock(User.class);

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(user));
            given(favoriteRepository.findByUserEmailAndClubId(userEmail, clubId)).willReturn(Optional.empty());

            // when
            FavoriteToggleServiceResponse response = favoriteService.toggleFavorite(request);

            // then
            assertThat(response.favorited()).isTrue();
            verify(favoriteRepository, times(1)).save(any(Favorite.class));
        }

        @Test
        @DisplayName("즐겨찾기가 이미 존재하면 제거한다")
        void toggleFavorite_Remove() {
            // given
            String userEmail = "test@test.com";
            String clubId = "club-1";
            FavoriteToggleServiceRequest request = FavoriteToggleServiceRequest.of(userEmail, clubId);

            Club club = mock(Club.class);
            User user = mock(User.class);
            Favorite favorite = mock(Favorite.class);

            given(clubRepository.findById(clubId)).willReturn(Optional.of(club));
            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(user));
            given(favoriteRepository.findByUserEmailAndClubId(userEmail, clubId)).willReturn(Optional.of(favorite));

            // when
            FavoriteToggleServiceResponse response = favoriteService.toggleFavorite(request);

            // then
            assertThat(response.favorited()).isFalse();
            verify(favoriteRepository, times(1)).delete(favorite);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 예외가 발생한다")
        void toggleFavorite_UserNotFound() {
            // given
            String userEmail = "nonexistent@test.com";
            FavoriteToggleServiceRequest request = FavoriteToggleServiceRequest.of(userEmail, "club-1");

            given(clubRepository.findById(anyString())).willReturn(Optional.of(mock(Club.class)));
            given(userRepository.findByEmail(userEmail)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.toggleFavorite(request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("동아리를 찾을 수 없으면 예외가 발생한다")
        void toggleFavorite_ClubNotFound() {
            // given
            String clubId = "nonexistent-club";
            FavoriteToggleServiceRequest request = FavoriteToggleServiceRequest.of("test@test.com", clubId);

            given(clubRepository.findById(clubId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.toggleFavorite(request))
                    .isInstanceOf(ClubNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getFavoriteList 메서드")
    class GetFavoriteListTest {

        @Test
        @DisplayName("즐겨찾기 목록을 조회한다")
        void getFavoriteList_Success() {
            // given
            FavoriteListServiceRequest request = FavoriteListServiceRequest.builder()
                    .userEmail("test@test.com")
                    .size(10)
                    .sort("latest")
                    .build();

            Club club = mock(Club.class);
            Favorite favorite = mock(Favorite.class);
            given(favorite.getClub()).willReturn(club);
            given(club.getId()).willReturn("club-1");
            givenNoRecruitingForm();
            givenNoClubMembers();

            given(favoriteRepository.findFavoritesByRequest(request)).willReturn(List.of(favorite));

            // when
            FavoriteListServiceResponse response = favoriteService.getFavoriteList(request);

            // then
            assertThat(response.favoriteClubs()).hasSize(1);
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("인기순 정렬 요청 시 getPopularFavoriteList를 호출한다")
        void getFavoriteList_Popular() {
            // given
            FavoriteListServiceRequest request = FavoriteListServiceRequest.builder()
                    .userEmail("test@test.com")
                    .size(10)
                    .sort("popular")
                    .build();

            lenient().when(favoriteRepository.findAllByUserEmailWithClub(request.userEmail())).thenReturn(Collections.emptyList());

            // when
            FavoriteListServiceResponse response = favoriteService.getFavoriteList(request);

            // then
            assertThat(response.favoriteClubs()).isEmpty();
            verify(favoriteRepository, times(1)).findAllByUserEmailWithClub(anyString());
        }
    }

    @Nested
    @DisplayName("getPopularFavoriteList 메서드")
    class GetPopularFavoriteListTest {

        @Test
        @DisplayName("인기도 점수에 따라 정렬된 목록을 반환한다")
        void getPopularFavoriteList_Sorting() {
            // given
            FavoriteListServiceRequest request = FavoriteListServiceRequest.builder()
                    .userEmail("test@test.com")
                    .size(10)
                    .sort("popular")
                    .build();

            Club club1 = mock(Club.class);
            Club club2 = mock(Club.class);
            given(club1.getId()).willReturn("club-1");
            given(club2.getId()).willReturn("club-2");
            given(club1.getViewCount()).willReturn(100L);
            given(club2.getViewCount()).willReturn(200L);

            Favorite fav1 = mock(Favorite.class);
            Favorite fav2 = mock(Favorite.class);
            given(fav1.getClub()).willReturn(club1);
            given(fav2.getClub()).willReturn(club2);

            given(favoriteRepository.findAllByUserEmailWithClub(request.userEmail())).willReturn(List.of(fav1, fav2));
            given(favoriteRepository.countClubFavoritesForEach(any())).willReturn(Collections.emptyList());
            givenNoClubMembers();

            given(popularityCalculator.calculate(eq(0L), eq(0L), eq(100L))).willReturn(10.0);
            given(popularityCalculator.calculate(eq(0L), eq(0L), eq(200L))).willReturn(20.0);

            givenNoRecruitingForm();

            // when
            FavoriteListServiceResponse response = favoriteService.getFavoriteList(request);

            // then
            assertThat(response.favoriteClubs()).hasSize(2);
            assertThat(response.favoriteClubs().get(0).id()).isEqualTo("club-2");
            assertThat(response.favoriteClubs().get(1).id()).isEqualTo("club-1");
        }

        @Test
        @DisplayName("커서가 존재하면 빈 목록을 반환한다")
        void getPopularFavoriteList_WithCursor_ReturnsEmpty() {
            // given
            FavoriteListServiceRequest request = FavoriteListServiceRequest.builder()
                    .userEmail("test@test.com")
                    .cursor("some-cursor")
                    .size(10)
                    .sort("popular")
                    .build();

            // when
            FavoriteListServiceResponse response = favoriteService.getFavoriteList(request);

            // then
            assertThat(response.favoriteClubs()).isEmpty();
            assertThat(response.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("isFavorited 메서드")
    class IsFavoritedTest {

        @Test
        @DisplayName("즐겨찾기 여부를 확인한다")
        void isFavorited_True() {
            // given
            String userEmail = "test@test.com";
            String clubId = "club-1";
            given(favoriteRepository.existsByUserEmailAndClubId(userEmail, clubId)).willReturn(true);

            // when
            boolean result = favoriteService.isFavorited(userEmail, clubId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("즐겨찾기가 존재하지 않으면 false를 반환한다")
        void isFavorited_False() {
            // given
            String userEmail = "test@test.com";
            String clubId = "club-1";
            given(favoriteRepository.existsByUserEmailAndClubId(userEmail, clubId)).willReturn(false);

            // when
            boolean result = favoriteService.isFavorited(userEmail, clubId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("toClubCardServiceResponse 로직 테스트")
    class ToClubCardServiceResponseTest {

        private FavoriteListServiceRequest givenRequest() {
            return FavoriteListServiceRequest.builder()
                    .userEmail("test@test.com")
                    .size(10)
                    .sort("latest")
                    .build();
        }

        /**
         * 마감일이 {@code applyEndDate} 인 모집중 지원폼이 걸린 즐겨찾기 목록을 조회한다.
         *
         * <p>마감 임박 판정은 {@code ApplyDeadlinePolicy} 에 위임되므로 boolean 을 스텁하지 않는다.
         * 마감일만 주고 실제 날짜 규칙이 돌게 둬야 경계값이 검증된다.
         */
        private FavoriteListServiceResponse getFavoriteListWithDeadline(LocalDate applyEndDate) {
            FavoriteListServiceRequest request = givenRequest();

            Club club = mock(Club.class);
            Favorite favorite = mock(Favorite.class);
            given(favorite.getClub()).willReturn(club);
            given(club.getId()).willReturn("club-1");
            givenNoClubMembers();

            given(applyFormRepository.findRecruitingFormsByClubIds(any()))
                    .willReturn(List.of(new ClubRecruitmentQueryDto("club-1", applyEndDate)));
            given(favoriteRepository.findFavoritesByRequest(request)).willReturn(List.of(favorite));

            return favoriteService.getFavoriteList(request);
        }

        @Test
        @DisplayName("모집 마감일이 7일 이내이면 isDeadlineImminent가 true이다")
        void toClubCardServiceResponse_DeadlineImminent_True() {
            FavoriteListServiceResponse response = getFavoriteListWithDeadline(LocalDate.now().plusDays(5));

            assertThat(response.favoriteClubs().get(0).isDeadlineImminent()).isTrue();
        }

        @Test
        @DisplayName("모집 마감일이 7일보다 많이 남았으면 isDeadlineImminent가 false이다")
        void toClubCardServiceResponse_DeadlineImminent_False() {
            FavoriteListServiceResponse response = getFavoriteListWithDeadline(LocalDate.now().plusDays(10));

            assertThat(response.favoriteClubs().get(0).isDeadlineImminent()).isFalse();
        }

        @Test
        @DisplayName("마감일 당일(0일 남음)과 정확히 7일 남은 경우 모두 마감 임박이다 - 경계값")
        void toClubCardServiceResponse_DeadlineImminent_Boundaries() {
            assertThat(getFavoriteListWithDeadline(LocalDate.now())
                    .favoriteClubs().get(0).isDeadlineImminent()).isTrue();
            assertThat(getFavoriteListWithDeadline(LocalDate.now().plusDays(7))
                    .favoriteClubs().get(0).isDeadlineImminent()).isTrue();
            assertThat(getFavoriteListWithDeadline(LocalDate.now().plusDays(8))
                    .favoriteClubs().get(0).isDeadlineImminent()).isFalse();
        }

        @Test
        @DisplayName("마감일이 이미 지났으면 마감 임박이 아니다")
        void toClubCardServiceResponse_DeadlineAlreadyPassed() {
            FavoriteListServiceResponse response = getFavoriteListWithDeadline(LocalDate.now().minusDays(1));

            assertThat(response.favoriteClubs().get(0).isDeadlineImminent()).isFalse();
        }

        @Test
        @DisplayName("마감일 정보가 없으면 마감 임박이 아니다")
        void toClubCardServiceResponse_NoDeadline() {
            FavoriteListServiceResponse response = getFavoriteListWithDeadline(null);

            assertThat(response.favoriteClubs().get(0).isDeadlineImminent()).isFalse();
        }

        @Test
        @DisplayName("모집중인 지원폼이 없으면 모집중이 아니고 마감 임박도 아니다")
        void toClubCardServiceResponse_NoRecruitingApplyForm() {
            FavoriteListServiceRequest request = givenRequest();

            Club club = mock(Club.class);
            Favorite favorite = mock(Favorite.class);
            given(favorite.getClub()).willReturn(club);
            given(club.getId()).willReturn("club-1");
            givenNoClubMembers();

            givenNoRecruitingForm();
            given(favoriteRepository.findFavoritesByRequest(request)).willReturn(List.of(favorite));

            FavoriteListServiceResponse response = favoriteService.getFavoriteList(request);

            assertThat(response.favoriteClubs().get(0).recruiting()).isFalse();
            assertThat(response.favoriteClubs().get(0).isDeadlineImminent()).isFalse();
        }

        @Test
        @DisplayName("멤버 수는 배치 집계 결과에서 가져온다")
        void toClubCardServiceResponse_MemberCountFromBatch() {
            FavoriteListServiceRequest request = givenRequest();

            Club club = mock(Club.class);
            Favorite favorite = mock(Favorite.class);
            given(favorite.getClub()).willReturn(club);
            given(club.getId()).willReturn("club-1");

            given(clubMemberRepository.countClubMembersForEach(any()))
                    .willReturn(List.of(new ClubMemberCountQueryDto("club-1", 7L)));
            givenNoRecruitingForm();
            given(favoriteRepository.findFavoritesByRequest(request)).willReturn(List.of(favorite));

            FavoriteListServiceResponse response = favoriteService.getFavoriteList(request);

            assertThat(response.favoriteClubs().get(0).clubMemberCount()).isEqualTo(7);
            verify(club, never()).getClubMembers();
        }

        @Test
        @DisplayName("집계 결과에 없는 동아리의 멤버 수는 0이다")
        void toClubCardServiceResponse_MemberCountDefaultsToZero() {
            FavoriteListServiceRequest request = givenRequest();

            Club club = mock(Club.class);
            Favorite favorite = mock(Favorite.class);
            given(favorite.getClub()).willReturn(club);
            given(club.getId()).willReturn("club-1");

            givenNoClubMembers();
            givenNoRecruitingForm();
            given(favoriteRepository.findFavoritesByRequest(request)).willReturn(List.of(favorite));

            FavoriteListServiceResponse response = favoriteService.getFavoriteList(request);

            assertThat(response.favoriteClubs().get(0).clubMemberCount()).isZero();
        }
    }

    @Nested
    @DisplayName("배치 조회 횟수")
    class BatchQueryCountTest {

        private FavoriteListServiceRequest givenRequest() {
            return FavoriteListServiceRequest.builder()
                    .userEmail("test@test.com")
                    .size(10)
                    .sort("latest")
                    .build();
        }

        private Favorite favoriteOf(String clubId) {
            Club club = mock(Club.class);
            Favorite favorite = mock(Favorite.class);
            given(favorite.getClub()).willReturn(club);
            given(club.getId()).willReturn(clubId);
            return favorite;
        }

        /**
         * 즐겨찾기 개수가 늘어도 지원폼/멤버수 조회는 각각 한 번씩만 나가야 한다.
         *
         * <p>이 검증이 없으면 동아리마다 조회하는 형태로 되돌아가도 다른 테스트는 모두 통과한다.
         */
        @Test
        @DisplayName("즐겨찾기가 여러 개여도 지원폼/멤버수 조회는 각각 한 번씩만 실행된다")
        void batchQueriesRunOncePerRequest() {
            FavoriteListServiceRequest request = givenRequest();

            // 스터빙 중첩(willReturn 인자 안에서 다시 given 호출)을 피하려고 먼저 만들어 둔다
            List<Favorite> favorites = List.of(favoriteOf("club-1"), favoriteOf("club-2"), favoriteOf("club-3"));

            givenNoRecruitingForm();
            givenNoClubMembers();
            given(favoriteRepository.findFavoritesByRequest(request)).willReturn(favorites);

            favoriteService.getFavoriteList(request);

            verify(applyFormRepository, times(1)).findRecruitingFormsByClubIds(any());
            verify(clubMemberRepository, times(1)).countClubMembersForEach(any());
        }

        @Test
        @DisplayName("즐겨찾기가 없으면 배치 조회를 실행하지 않는다")
        void noBatchQueriesWhenEmpty() {
            FavoriteListServiceRequest request = givenRequest();

            given(favoriteRepository.findFavoritesByRequest(request)).willReturn(Collections.emptyList());

            FavoriteListServiceResponse response = favoriteService.getFavoriteList(request);

            assertThat(response.favoriteClubs()).isEmpty();
            verify(applyFormRepository, never()).findRecruitingFormsByClubIds(any());
            verify(clubMemberRepository, never()).countClubMembersForEach(any());
        }
    }
}
