package org.project.ttokttok.domain.favorite.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applyform.domain.ApplyDeadlinePolicy;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 즐겨찾기 서비스 클래스 즐겨찾기 추가/제거 및 조회 관련 비즈니스 로직을 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ApplyFormRepository applyFormRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final PopularityCalculator popularityCalculator;

    /**
     * 즐겨찾기 토글 (추가/제거) 이미 즐겨찾기가 되어 있으면 제거하고, 없으면 추가합니다.
     */
    @Transactional
    public FavoriteToggleServiceResponse toggleFavorite(FavoriteToggleServiceRequest request) {
        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(ClubNotFoundException::new);

        User user = userRepository.findByEmail(request.userEmail())
                .orElseThrow(UserNotFoundException::new);

        Optional<Favorite> existingFavorite = favoriteRepository.findByUserEmailAndClubId(
                request.userEmail(), request.clubId());

        if (existingFavorite.isPresent()) {
            favoriteRepository.delete(existingFavorite.get());
            return FavoriteToggleServiceResponse.of(request.clubId(), false);
        }

        Favorite favorite = Favorite.create(user, club);

        favoriteRepository.save(favorite);

        return FavoriteToggleServiceResponse.of(request.clubId(), true);
    }

    @Transactional(readOnly = true)
    public FavoriteListServiceResponse getFavoriteList(FavoriteListServiceRequest request) {
        if ("popular".equals(request.sort())) {
            return getPopularFavoriteList(request);
        }

        List<Favorite> favorites = favoriteRepository.findFavoritesByRequest(request);

        boolean hasNext = favorites.size() > request.size();
        List<Favorite> actualFavorites = hasNext ? favorites.subList(0, request.size()) : favorites;
        String nextCursor = hasNext ? actualFavorites.get(actualFavorites.size() - 1).getId() : null;

        List<Club> clubs = actualFavorites.stream()
                .map(Favorite::getClub)
                .toList();

        return new FavoriteListServiceResponse(toClubCardServiceResponses(clubs), nextCursor, hasNext);
    }

    /**
     * [개선 후] Batch Query를 활용한 인기순 조회 로직
     */
    private FavoriteListServiceResponse getPopularFavoriteList(FavoriteListServiceRequest request) {
        if (request.cursor() != null) {
            return new FavoriteListServiceResponse(Collections.emptyList(), null, false);
        }

        List<Club> clubs = favoriteRepository.findAllByUserEmailWithClub(request.userEmail()).stream()
                .map(Favorite::getClub)
                .toList();

        if (clubs.isEmpty()) {
            return new FavoriteListServiceResponse(Collections.emptyList(), null, false);
        }

        List<String> clubIds = toClubIds(clubs);

        Map<String, Long> favoriteCountMap = favoriteRepository.countClubFavoritesForEach(clubIds).stream()
                .collect(Collectors.toMap(ClubFavoriteCountQueryDto::clubId, ClubFavoriteCountQueryDto::count));
        // 정렬 비교자가 전체 즐겨찾기를 훑으므로, 멤버 수는 정렬 이전에 한 번에 모아둔다
        Map<String, Long> memberCountMap = findMemberCounts(clubIds);

        List<Club> sortedClubs = clubs.stream()
                .sorted(Comparator.comparingDouble(
                        (Club club) -> popularityCalculator.calculate(
                                memberCountMap.getOrDefault(club.getId(), 0L),
                                favoriteCountMap.getOrDefault(club.getId(), 0L),
                                club.getViewCount())).reversed())
                .limit(request.size())
                .toList();

        return new FavoriteListServiceResponse(
                toClubCardServiceResponses(sortedClubs, memberCountMap), null, false);
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(String userEmail, String clubId) {
        return favoriteRepository.existsByUserEmailAndClubId(userEmail, clubId);
    }

    private List<ClubCardServiceResponse> toClubCardServiceResponses(List<Club> clubs) {
        if (clubs.isEmpty()) {
            return List.of();
        }
        return toClubCardServiceResponses(clubs, findMemberCounts(toClubIds(clubs)));
    }

    /**
     * 동아리 목록을 카드 응답으로 일괄 변환한다.
     *
     * <p>모집 여부와 멤버 수를 동아리마다 조회하면 즐겨찾기 개수에 비례해 쿼리가 늘어나므로,
     * 두 값을 각각 배치 쿼리 한 번으로 모아 맵으로 조회한다.
     *
     * @param memberCountMap 이미 조회해 둔 동아리별 멤버 수 (인기순 경로는 정렬에 먼저 쓰므로 재사용한다)
     */
    private List<ClubCardServiceResponse> toClubCardServiceResponses(List<Club> clubs,
                                                                     Map<String, Long> memberCountMap) {
        if (clubs.isEmpty()) {
            return List.of();
        }

        Map<String, ClubRecruitmentQueryDto> recruitingFormMap =
                applyFormRepository.findRecruitingFormsByClubIds(toClubIds(clubs)).stream()
                        .collect(Collectors.toMap(ClubRecruitmentQueryDto::clubId, dto -> dto, (first, ignored) -> first));

        return clubs.stream()
                .map(club -> toClubCardServiceResponse(club, recruitingFormMap.get(club.getId()),
                        memberCountMap.getOrDefault(club.getId(), 0L)))
                .toList();
    }

    /**
     * @param recruitingForm 모집중인 지원폼. 모집중이 아니면 {@code null} 이다.
     */
    private ClubCardServiceResponse toClubCardServiceResponse(Club club,
                                                              ClubRecruitmentQueryDto recruitingForm,
                                                              long memberCount) {
        boolean recruiting = recruitingForm != null;
        boolean isDeadlineImminent =
                recruiting && ApplyDeadlinePolicy.isImminent(recruitingForm.applyEndDate());

        return new ClubCardServiceResponse(
                club.getId(),
                club.getName(),
                club.getClubType(),
                club.getClubCategory(),
                club.getCustomCategory(),
                club.getSummary(),
                club.getProfileImageUrl(),
                (int) memberCount,
                recruiting,
                true,
                isDeadlineImminent
        );
    }

    private Map<String, Long> findMemberCounts(List<String> clubIds) {
        return clubMemberRepository.countClubMembersForEach(clubIds).stream()
                .collect(Collectors.toMap(ClubMemberCountQueryDto::clubId, ClubMemberCountQueryDto::count));
    }

    private List<String> toClubIds(List<Club> clubs) {
        return clubs.stream()
                .map(Club::getId)
                .toList();
    }
}
