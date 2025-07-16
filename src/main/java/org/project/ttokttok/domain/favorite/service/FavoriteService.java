package org.project.ttokttok.domain.favorite.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
import org.project.ttokttok.domain.favorite.domain.Favorite;
import org.project.ttokttok.domain.favorite.repository.FavoriteRepository;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteListServiceRequest;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteToggleServiceRequest;
import org.project.ttokttok.domain.favorite.service.dto.response.FavoriteListServiceResponse;
import org.project.ttokttok.domain.favorite.service.dto.response.FavoriteToggleServiceResponse;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 즐겨찾기 서비스 클래스
 * 즐겨찾기 추가/제거 및 조회 관련 비즈니스 로직을 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    /**
     * 즐겨찾기 토글 (추가/제거)
     * 이미 즐겨찾기가 되어 있으면 제거하고, 없으면 추가합니다.
     *
     * @param request 즐겨찾기 토글 요청 정보
     * @return 즐겨찾기 토글 결과
     */
    @Transactional
    public FavoriteToggleServiceResponse toggleFavorite(FavoriteToggleServiceRequest request) {
        // 동아리 존재 확인
        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(ClubNotFoundException::new);

        // 사용자 존재 확인
        User user = userRepository.findByEmail(request.userEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 기존 즐겨찾기 확인
        Optional<Favorite> existingFavorite = favoriteRepository.findByUserEmailAndClubId(
                request.userEmail(), request.clubId());

        if (existingFavorite.isPresent()) {
            // 즐겨찾기 제거
            favoriteRepository.delete(existingFavorite.get());
            log.info("즐겨찾기 제거 완료: 사용자={}, 동아리={}", request.userEmail(), request.clubId());
            return FavoriteToggleServiceResponse.of(request.clubId(), false);
        } else {
            // 즐겨찾기 추가
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .club(club)
                    .build();
            favoriteRepository.save(favorite);
            log.info("즐겨찾기 추가 완료: 사용자={}, 동아리={}", request.userEmail(), request.clubId());
            return FavoriteToggleServiceResponse.of(request.clubId(), true);
        }
    }

    /**
     * 사용자의 즐겨찾기 목록 조회 (커서 기반)
     *
     * @param request 커서, 사이즈, 정렬 기준 포함 요청
     * @return 커서 기반 페이징 처리된 즐겨찾기 동아리 목록
     */
    @Transactional(readOnly = true)
    public FavoriteListServiceResponse getFavoriteList(FavoriteListServiceRequest request) {
        log.info("즐겨찾기 목록 조회 시작: {}", request);

        // "popular" 정렬은 별도 처리
        if ("popular".equals(request.sort())) {
            return getPopularFavoriteList(request);
        }

        List<Favorite> favorites = favoriteRepository.findFavoritesByRequest(request);

        boolean hasNext = favorites.size() > request.size();
        List<Favorite> actualFavorites = hasNext ? favorites.subList(0, request.size()) : favorites;
        String nextCursor = hasNext ? actualFavorites.get(actualFavorites.size() - 1).getId() : null;

        List<ClubCardServiceResponse> favoriteClubs = actualFavorites.stream()
                .map(favorite -> toClubCardServiceResponse(favorite.getClub(), true))
                .toList();

        log.info("즐겨찾기 목록 조회 완료: 사용자={}, 개수={}, 다음 페이지 존재={}", request.userEmail(), favoriteClubs.size(), hasNext);
        return new FavoriteListServiceResponse(favoriteClubs, nextCursor, hasNext);
    }

    /**
     * 인기순 즐겨찾기 목록 조회 (메모리 기반 처리)
     * 'popular' 정렬은 커서 기반을 지원하지 않으므로, 첫 페이지 요청 시에만 동작합니다.
     */
    private FavoriteListServiceResponse getPopularFavoriteList(FavoriteListServiceRequest request) {
        if (request.cursor() != null) {
            // 커서가 있다는 것은 다음 페이지 요청이지만, 'popular'는 전체 목록을 정렬하므로
            // 다음 페이지라는 개념이 없습니다. 따라서 빈 목록을 반환합니다.
            return new FavoriteListServiceResponse(Collections.emptyList(), null, false);
        }

        // 1. 사용자의 모든 즐겨찾기 동아리 정보를 가져옵니다.
        List<Favorite> allFavorites = favoriteRepository.findAllByUserEmailWithClub(request.userEmail());

        // 2. ClubCardServiceResponse로 변환합니다.
        List<ClubCardServiceResponse> allFavoriteClubs = allFavorites.stream()
                .map(favorite -> toClubCardServiceResponse(favorite.getClub(), true))
                .toList();

        // 3. 인기도 점수를 기준으로 메모리에서 내림차순 정렬합니다.
        List<ClubCardServiceResponse> sortedClubs = allFavoriteClubs.stream()
                .sorted(Comparator.comparingDouble(this::calculatePopularityScore).reversed())
                .toList();

        // 4. 요청된 사이즈만큼 잘라서 최종 결과를 생성합니다.
        List<ClubCardServiceResponse> resultClubs = sortedClubs.stream()
                .limit(request.size())
                .toList();

        // 'popular' 정렬은 커서 기반 다음 페이지를 지원하지 않으므로 hasNext는 항상 false, nextCursor는 null 입니다.
        return new FavoriteListServiceResponse(resultClubs, null, false);
    }

    /**
     * 인기도 점수 계산 (기존 로직 반영)
     * 점수 = (멤버 수 * 0.7) + (총 즐겨찾기 수 * 0.3)
     *
     * @param club 카드 응답 DTO
     * @return 계산된 인기도 점수
     */
    private double calculatePopularityScore(ClubCardServiceResponse club) {
        // ClubCardServiceResponse에서 멤버 수를 가져옵니다.
        long memberCount = club.clubMemberCount();

        // **성능 주의**: 이 부분은 각 동아리마다 즐겨찾기 수를 조회하는 추가 쿼리(N+1)를 발생시킬 수 있습니다.
        // 즐겨찾기 수가 매우 많은 사용자의 경우 성능에 영향을 줄 수 있습니다.
        long favoriteCount = favoriteRepository.countByClubId(club.id());

        return (memberCount * 0.7) + (favoriteCount * 0.3);
    }

    /**
     * 특정 동아리의 즐겨찾기 상태 확인
     *
     * @param userEmail 사용자 이메일
     * @param clubId 동아리 ID
     * @return 즐겨찾기 여부
     */
    @Transactional(readOnly = true)
    public boolean isFavorited(String userEmail, String clubId) {
        return favoriteRepository.existsByUserEmailAndClubId(userEmail, clubId);
    }

    /**
     * Club 엔티티를 ClubCardServiceResponse로 변환
     */
    private ClubCardServiceResponse toClubCardServiceResponse(Club club, boolean bookmarked) {
        return new ClubCardServiceResponse(
                club.getId(),
                club.getName(),
                club.getClubType(),
                club.getClubCategory(),
                club.getCustomCategory(),
                club.getSummary(),
                club.getProfileImageUrl(),
                club.getClubMembers().size(), // 멤버 수
                club.isRecruiting(),
                bookmarked
        );
    }
}