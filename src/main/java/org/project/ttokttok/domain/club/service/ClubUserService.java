package org.project.ttokttok.domain.club.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.global.config.ClubPopularityConfig;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 동아리 서비스 클래스
 * 동아리 조회 관련 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class ClubUserService {

    // 소개글 조회
    private final ClubRepository clubRepository;
    private final S3Service s3Service;  // Optional 제거 (develop 방식 적용)

    // 동아리 인기글 조회
    private final ClubPopularityConfig popularityConfig;

    /**
     * 동아리 상세 정보 조회
     * 특정 동아리의 상세 정보를 조회합니다.
     * 
     * @param username 사용자 이메일
     * @param clubId 동아리 ID
     * @return 동아리 상세 정보
     * @throws ClubNotFoundException 동아리를 찾을 수 없는 경우
     */
    @Transactional
    public ClubDetailServiceResponse getClubIntroduction(String username, String clubId) {
        Club targetClub = clubRepository.findById(clubId)
                .orElseThrow(ClubNotFoundException::new);

        targetClub.updateViewCount();

        return ClubDetailServiceResponse.from(clubRepository.getClubIntroduction(clubId, username));
    }

    /**
     * 동아리 목록 조회
     * 필터링 조건에 따라 동아리 목록을 무한스크롤 조회합니다.
     * 
     * @param category 동아리 카테고리 필터
     * @param type 동아리 분류 필터 (전체: null, 중앙: CENTRAL, 연합: UNION, 과동아리: DEPARTMENT)
     * @param clubUniv 대학 구분 필터 (과동아리 선택 시 사용)
     * @param recruiting 모집 여부 필터 (전체: null, 모집중: true, 모집마감: false)
     * @param size 조회할 개수
     * @param cursor 커서 (무한스크롤용)
     * @param sort 정렬 방식
     * @return 필터링된 동아리 목록과 페이징 정보
     */
    public ClubListServiceResponse getClubList(
            ClubCategory category,
            ClubType type,
            ClubUniv clubUniv,
            Boolean recruiting,
            List<ApplicableGrade> grades, // 추가
            int size,
            String cursor,
            String sort,
            String userEmail) {

        List<ClubCardQueryResponse> results = clubRepository.getClubList(
                category, type, clubUniv, recruiting, grades, size, cursor, sort, userEmail
        );

        // hasNext 확인을 위해 size+1로 조회했으므로
        boolean hasNext = results.size() > size;
        if (hasNext) {
            results = results.subList(0, size);  // 실제 size만큼만 반환
        }

        // 다음 커서 생성 (정렬 방식에 따라 다르게 생성)
        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            ClubCardQueryResponse lastItem = results.get(results.size() - 1);
            nextCursor = generateNextCursor(lastItem.id(), sort);
        }

        List<ClubCardServiceResponse> clubs = results.stream()
                .map(this::toServiceResponse)
                .toList();

        return new ClubListServiceResponse(clubs, clubs.size(), 0L, hasNext, nextCursor);
    }

    /**
     * 현재 인증된 사용자의 이메일 조회
     *
     * @return 사용자 이메일
     * */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();    // 실제 사용자 이메일 반환
        }

        // 개발/테스트 환경에서는 임시 사용자 이메일 반환
        return "test@sangmyung.kr";
    }

    /**
     * ClubCardQueryResponse 를 ClubCardServiceResponse 로 변환
     *
     * @param queryResponse Repository 에서 조회한 결과
     * @return Service 레이어 응답 DTO
     * */
    private ClubCardServiceResponse toServiceResponse(ClubCardQueryResponse queryResponse) {
        return new ClubCardServiceResponse(
                queryResponse.id(),
                queryResponse.name(),
                queryResponse.clubType(),
                queryResponse.clubCategory(),
                queryResponse.customCategory(),
                queryResponse.summary(),
                queryResponse.profileImageUrl(),
                queryResponse.clubMemberCount(),
                queryResponse.recruiting(),
                queryResponse.bookmarked()
        );
    }

    /**
     * 메인 배너용 인기 동아리 목록 조회 (멤버수 기준)
     * 멤버수가 많은 순으로 동아리를 조회합니다.
     *
     * @return 전체 인기 동아리 목록
     * */
    public ClubListServiceResponse getAllPopularClubs(String userEmail) {

        List<ClubCardQueryResponse> results = clubRepository.getAllPopularClubs(
                userEmail, popularityConfig.getMinScore()
        );

        List<ClubCardServiceResponse> clubs = results.stream()
                .map(this::toServiceResponse)
                .toList();

        return new ClubListServiceResponse(clubs, clubs.size(), (long) clubs.size(), false, null);
    }

    /**
     * 전체 인기 동아리 목록 조회 (필터링 지원)
     * "더보기" 클릭 시 보여지는 전체 인기 동아리 목록을 조회합니다.
     * '인기도순', '멤버많은순', '최신등록순' 정렬과 무한스크롤을 지원합니다.
     *
     * @param size 조회할 개수
     * @param cursor 커서 (무한스크롤용)
     * @param sort 정렬 방식
     * @return 정렬된 인기 동아리 목록
     */
    public ClubListServiceResponse getPopularClubsWithFilters(
            int size,
            String cursor,
            String sort,
            String userEmail) {

        // 새로운 복합 인기도 기준 메서드 적용
        List<ClubCardQueryResponse> results = clubRepository.getPopularClubsWithFilters(
                size, cursor, sort, userEmail, popularityConfig.getMinScore()
        );

        // hasNext 확인을 위해 size+1로 조회했으므로
        boolean hasNext = results.size() > size;
        if (hasNext) {
            results = results.subList(0, size);  // 실제 size만큼만 반환
        }

        // 다음 커서 생성
        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            ClubCardQueryResponse lastItem = results.get(results.size() - 1);
            // 'getClubList'에서 사용하던 커서 생성 로직 재활용
            nextCursor = generateNextCursor(lastItem.id(), sort);
        }

        List<ClubCardServiceResponse> clubs = results.stream()
                .map(this::toServiceResponse)
                .toList();

        return new ClubListServiceResponse(clubs, clubs.size(), 0L, hasNext, nextCursor);
    }

    /**
     * 정렬 방식에 따라 다음 커서 생성
     *
     * @param lastItemId 마지막으로 조회된 아이템의 ID
     * @param sort 정렬 방식
     * @return 다음 커서 문자열
     */
    private String generateNextCursor(String lastItemId, String sort) {
        // 정렬 방식에 따라 다른 커서 생성
        switch (sort) {
            case "latest":
                // 최신순은 ID 기준으로 정렬되므로 ID 사용
                return lastItemId;
            case "popular":
            case "member_count":
                // 인기도순과 멤버많은순은 복합 정렬이므로 ID만 사용
                // TODO: 향후 정렬 기준값과 ID를 조합한 복합 커서로 개선 가능
                return lastItemId;
            default:
                return lastItemId;
        }
    }

    /**
     * 동아리 검색 서비스 로직
     *
     * @param keyword 검색어
     * @param sort 정렬 기준 (latest, member, popular)
     * @param cursor 커서 기반 페이징용 기준 값
     * @param size 한 페이지당 개수
     */
    public ClubListServiceResponse searchClubs(String keyword, String sort, String cursor, int size, String userEmail) {

        // 1. 전체 카운트 조회
        long totalCount = clubRepository.countByKeyword(keyword);

        // 2. 실제 데이터 조회 (무한 스크롤)
        List<ClubCardQueryResponse> queryResults = clubRepository.searchByKeyword(keyword, size, cursor, sort, userEmail);

        // 3. hasNext 계산
        boolean hasNext = queryResults.size() > size;
        if (hasNext) {
            queryResults = queryResults.subList(0, size); // Remove extra item
        }

        // 4. DTO 변환
        List<ClubCardServiceResponse> results = queryResults.stream()
                .map(this::toServiceResponse)
                .toList();

        // 5. 다음 커서 생성
        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            nextCursor = generateNextCursor(results.get(results.size() - 1).id(), sort);
        }

        // 6. 최종 응답 생성
        return new ClubListServiceResponse(results, results.size(), totalCount, hasNext, nextCursor);
    }
}