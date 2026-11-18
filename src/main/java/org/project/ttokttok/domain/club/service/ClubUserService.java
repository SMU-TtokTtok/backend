package org.project.ttokttok.domain.club.service;

import java.util.List;

import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubSortType;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.ClubDetailQueryResponse;
import org.project.ttokttok.domain.club.repository.dto.PopularClubCandidate;
import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.global.policy.PopularityPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 동아리 서비스 클래스
 * 동아리 조회 관련 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class ClubUserService {

    // 소개글 조회
    private final ClubRepository clubRepository;

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
        // 상세 조회를 먼저 하고 조회수 증가를 마지막에 둔다.
        // PostgreSQL 은 UPDATE 가 잡은 행 배타 락을 문장 종료가 아니라 커밋까지 유지한다.
        // 증가를 앞에 두면 뒤따르는 상세 조회 쿼리가 모두 락 구간에 들어가 처리량이 절반이 된다
        // (실측: RPS 601 -> 327). 부수적으로, 존재하지 않는 동아리 판정을 조회 결과로 직접 할 수 있다.
        ClubDetailQueryResponse detail = clubRepository.getClubIntroduction(clubId, username);
        if (detail == null) {
            throw new ClubNotFoundException();
        }

        clubRepository.increaseViewCount(clubId);

        return ClubDetailServiceResponse.from(detail);
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

        // 다음 커서 생성
        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            nextCursor = results.get(results.size() - 1).id();
        }

        List<ClubCardServiceResponse> clubs = results.stream()
                .map(ClubCardServiceResponse::from)
                .toList();

        return new ClubListServiceResponse(clubs, clubs.size(), 0L, hasNext, nextCursor);
    }

    /**
     * 메인 배너용 인기 동아리 목록 조회 (인기도 점수 기준)
     * 인기도 점수가 최소 점수 이상인 동아리를 점수가 높은 순으로 최대 {@code PopularityPolicy.BANNER_SIZE}개 조회합니다.
     *
     * @return 상위 인기 동아리 목록
     * */
    public ClubListServiceResponse getAllPopularClubs(String userEmail) {

        List<ClubCardServiceResponse> clubs = PopularClubRanking
                .page(clubRepository.findPopularClubCandidates(userEmail), ClubSortType.POPULAR, null,
                        PopularityPolicy.BANNER_SIZE)
                .stream()
                .map(candidate -> ClubCardServiceResponse.from(candidate.card()))
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

        List<ClubCardQueryResponse> results = PopularClubRanking
                .page(clubRepository.findPopularClubCandidates(userEmail), ClubSortType.from(sort), cursor, size + 1)
                .stream()
                .map(PopularClubCandidate::card)
                .toList();

        // hasNext 확인을 위해 size+1로 조회했으므로
        boolean hasNext = results.size() > size;
        if (hasNext) {
            results = results.subList(0, size);  // 실제 size만큼만 반환
        }

        // 다음 커서 생성
        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            nextCursor = results.get(results.size() - 1).id();
        }

        List<ClubCardServiceResponse> clubs = results.stream()
                .map(ClubCardServiceResponse::from)
                .toList();

        return new ClubListServiceResponse(clubs, clubs.size(), 0L, hasNext, nextCursor);
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
                .map(ClubCardServiceResponse::from)
                .toList();

        // 5. 다음 커서 생성
        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            nextCursor = results.get(results.size() - 1).id();
        }

        // 6. 최종 응답 생성
        return new ClubListServiceResponse(results, results.size(), totalCount, hasNext, nextCursor);
    }
}