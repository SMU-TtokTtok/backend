package org.project.ttokttok.domain.club.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.club.repository.dto.ClubCardQueryResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
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
     * 필터링 조건에 따라 동아리 목록을 페이징 조회합니다.
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
            int size,
            String cursor,
            String sort) {

        List<ClubCardQueryResponse> results = clubRepository.getClubList(
                category, type, recruiting, size, cursor, sort, getCurrentUserEmail()
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
     * @Param queryResponse Repository 에서 조회한 결과
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
}
