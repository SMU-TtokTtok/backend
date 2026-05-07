package org.project.ttokttok.domain.favorite.service;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
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

        List<ClubCardServiceResponse> favoriteClubs = actualFavorites.stream()
                .map(favorite -> toClubCardServiceResponse(favorite.getClub()))
                .toList();

        return new FavoriteListServiceResponse(favoriteClubs, nextCursor, hasNext);
    }

    /**
     * [개선 후] Batch Query를 활용한 인기순 조회 로직
     */
    private FavoriteListServiceResponse getPopularFavoriteList(FavoriteListServiceRequest request) {
        if (request.cursor() != null) {
            return new FavoriteListServiceResponse(Collections.emptyList(), null, false);
        }

        List<Favorite> allFavorites = favoriteRepository.findAllByUserEmailWithClub(request.userEmail());

        List<String> clubIds = allFavorites.stream()
                .map(f -> f.getClub().getId())
                .toList();
        
        Map<String, Long> favoriteCountMap = favoriteRepository.countClubFavoritesForEach(clubIds).stream()
                .collect(Collectors.toMap(ClubFavoriteCountQueryDto::clubId, ClubFavoriteCountQueryDto::count));

        List<ClubCardServiceResponse> resultClubs = allFavorites.stream()
                .map(Favorite::getClub)
                .sorted((club1, club2) -> {
                    double score1 = popularityCalculator.calculate(
                            club1.getClubMembers().size(), 
                            favoriteCountMap.getOrDefault(club1.getId(), 0L),
                            club1.getViewCount());
                    double score2 = popularityCalculator.calculate(
                            club2.getClubMembers().size(), 
                            favoriteCountMap.getOrDefault(club2.getId(), 0L),
                            club2.getViewCount());
                    return Double.compare(score2, score1);
                })
                .limit(request.size())
                .map(this::toClubCardServiceResponse)
                .toList();

        return new FavoriteListServiceResponse(resultClubs, null, false);
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(String userEmail, String clubId) {
        return favoriteRepository.existsByUserEmailAndClubId(userEmail, clubId);
    }

    private ClubCardServiceResponse toClubCardServiceResponse(Club club) {
        Optional<ApplyForm> activeApplyForm = applyFormRepository.findByClubIdAndStatus(club.getId(), ACTIVE);
        boolean recruiting = activeApplyForm.isPresent();

        boolean isDeadlineImminent = false;
        if (activeApplyForm.isPresent() && activeApplyForm.get().getApplyEndDate() != null) {
            LocalDate today = LocalDate.now();
            LocalDate deadline = activeApplyForm.get().getApplyEndDate();
            long daysUntilDeadline = today.until(deadline, DAYS);
            isDeadlineImminent = daysUntilDeadline >= 0 && daysUntilDeadline <= 7;
        }

        return new ClubCardServiceResponse(
                club.getId(),
                club.getName(),
                club.getClubType(),
                club.getClubCategory(),
                club.getCustomCategory(),
                club.getSummary(),
                club.getProfileImageUrl(),
                club.getClubMembers().size(),
                recruiting,
                true,
                isDeadlineImminent
        );
    }
}
