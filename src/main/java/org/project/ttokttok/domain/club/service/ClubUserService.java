package org.project.ttokttok.domain.club.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
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
import java.util.Optional;

/**
 * 동아리 서비스 클래스
 * 동아리 조회 관련 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class ClubUserService {

    // 소개글 조회
    private final ClubRepository clubRepository;
    private final Optional<S3Service> s3Service;

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
    public ClubDetailServiceResponse getClubIntroduction(String username, String clubId) {
        if (!clubRepository.existsById(clubId))
            throw new ClubNotFoundException();

        return ClubDetailServiceResponse.from(clubRepository.getClubIntroduction(clubId, username));
    }

    /**
     * 동아리 목록 조회
     * 필터링 조건에 따라 동아리 목록을 무한스크롤 조회합니다.
     * 
     * @param category 동아리 카테고리 필터
     * @param type 동아리 분류 필터
     * @param recruiting 모집 여부 필터
     * @param size 조회할 개수
     * @param cursor 커서 (무한스크롤용)
     * @param sort 정렬 방식
     * @return 필터링된 동아리 목록과 페이징 정보
     */
    public ClubListServiceResponse getClubList(
            ClubCategory category,
            ClubType type,
            Boolean recruiting,
            List<ApplicableGrade> grades, // 추가
            int size,
            String cursor,
            String sort) {

        List<ClubCardQueryResponse> results = clubRepository.getClubList(
                category, type, recruiting, grades, size, cursor, sort, getCurrentUserEmail()
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
            nextCursor = generateNextCursor(lastItem, sort);
        }

        List<ClubCardServiceResponse> clubs = results.stream()
                .map(this::toServiceResponse)
                .toList();

        return new ClubListServiceResponse(clubs, clubs.size(), hasNext, nextCursor);
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
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 조회할 동아리 수
     * @return 멤버수 순으로 정렬된 인기 동아리 목록
     * */
    public ClubListServiceResponse getPopularClubs(int page, int size) {
        // page * size 만큼 건너뛰고 size 개수만큼 조회
        List<ClubCardQueryResponse> results = clubRepository.getPopularClubs(
                page * size, size, getCurrentUserEmail(), popularityConfig.getMinScore()  // minScore 추가
        );

        List<ClubCardServiceResponse> clubs = results.stream()
                .map(this::toServiceResponse)
                .toList();

        return new ClubListServiceResponse(clubs, clubs.size(), false, null);
    }

    /**
     * 전체 인기 동아리 목록 조회 (필터링 지원)
     * "더보기" 클릭 시 보여지는 전체 인기 동아리 목록을 조회합니다.
     * 카테고리, 분류, 모집여부 필터링과 함께 무한스크롤을 지원합니다.
     * 
     * @param category 동아리 카테고리 필터
     * @param type 동아리 분류 필터
     * @param recruiting 모집 여부 필터
     * @param size 조회할 개수
     * @param cursor 커서 (무한스크롤용)
     * @return 멤버수 기준으로 정렬된 인기 동아리 목록
     */
    public ClubListServiceResponse getPopularClubsWithFilters(
            ClubCategory category,
            ClubType type,
            Boolean recruiting,
            int size,
            String cursor) {

        // 인기순(멤버수 기준) 정렬로 고정
//        List<ClubCardQueryResponse> results = clubRepository.getClubList(
//                category, type, recruiting, size, cursor, "popular", getCurrentUserEmail()
//        );

        // 새로운 복합 인기도 기준 메서드 적용
        List<ClubCardQueryResponse> results = clubRepository.getPopularClubsWithFilters(
                category, type, recruiting, size, cursor, getCurrentUserEmail(), popularityConfig.getMinScore()
        );

        // hasNext 확인을 위해 size+1로 조회했으므로
        boolean hasNext = results.size() > size;
        if (hasNext) {
            results = results.subList(0, size);  // 실제 size만큼만 반환
        }

        // 다음 커서 생성
        String nextCursor = hasNext && !results.isEmpty() ?
                results.get(results.size() - 1).id() : null;

        List<ClubCardServiceResponse> clubs = results.stream()
                .map(this::toServiceResponse)
                .toList();

        return new ClubListServiceResponse(clubs, clubs.size(), hasNext, nextCursor);
    }

    /**
     * 정렬 방식에 따라 다음 커서 생성
     *
     * @param lastItem 마지막 조회된 아이템
     * @param sort 정렬 방식
     * @return 다음 커서 문자열
     */
    private String generateNextCursor(ClubCardQueryResponse lastItem, String sort) {
        switch (sort) {
            case "latest":
                // 최신순: 생성일 기준 커서
                // 현재는 ID 기준으로 대체 (생성일 정보가 ClubCardQueryResponse에 없음)
                return lastItem.id();

            case "popular":
            case "member_count":
            default:
                // 인기순/멤버순/기본값: ID 기준 커서
                return lastItem.id();
        }
    }
}
