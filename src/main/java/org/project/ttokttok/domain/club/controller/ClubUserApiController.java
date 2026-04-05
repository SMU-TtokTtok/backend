package org.project.ttokttok.domain.club.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.controller.docs.ClubUserApiDocs;
import org.project.ttokttok.domain.club.controller.dto.response.ClubDetailResponse;
import org.project.ttokttok.domain.club.controller.dto.response.ClubListResponse;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.service.ClubUserService;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 동아리 관련 API 컨트롤러
 * 사용자가 동아리 정보를 조회할 수 있는 API들을 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs")
public class ClubUserApiController implements ClubUserApiDocs {

    private final ClubUserService clubUserService;

    /**
     * 동아리 상세 정보 조회 API
     * 특정 동아리의 상세 정보를 조회합니다.
     * 
     * @param username 인증된 사용자 이메일
     * @param clubId 조회할 동아리 ID
     * @return 동아리 상세 정보 (소개, 지원 정보, 멤버 수 등, 마감 임박 여부 포함)
     */
    @Override
    @GetMapping("/{clubId}/content")
    public ResponseEntity<ClubDetailResponse> getClubIntroduction(@AuthUserInfo String username,
                                                                  @PathVariable String clubId) {
        ClubDetailResponse response = ClubDetailResponse.from(
                clubUserService.getClubIntroduction(username, clubId)
        );

        return ResponseEntity.ok()
                .body(response);
    }


    /**
     * 동아리 목록 조회 API (메인화면 + 필터링 통합)
     * 메인 화면에서 동아리 목록을 필터링하여 페이징 조회합니다.
     * 
     * @param category 동아리 카테고리 필터 (봉사, 예술, 문화 등) - 선택사항
     * @param type 동아리 분류 필터 (중앙, 연합, 학과) - 선택사항
     * @param recruiting 모집 여부 필터 (true: 모집중, false: 모집마감) - 선택사항
     * @param size 페이지 크기 (기본값: 20)
     * @param sort 정렬 방식 (latest: 최신순, popular: 인기순) - 기본값: latest
     * @return 필터링된 동아리 목록과 페이징 정보
     */
    //FIXME - 마감 임박 추가 (지원 마감기간이 일주일 이내로 남은 동아리 boolean 필드 추가)
    @Override
    @GetMapping
    public ResponseEntity<ClubListResponse> getClubList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) ClubUniv clubUniv,
            @RequestParam(required = false) List<ApplicableGrade> grades,
            @RequestParam(required = false) String recruiting,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "latest") String sort,
            @AuthUserInfo String userEmail) {

        // type 파라미터 처리
        ClubType clubType = null;
        if (type != null && !type.equals("null")) {
            try {
                clubType = ClubType.valueOf(type);
            } catch (IllegalArgumentException e) {
                // 잘못된 type 값이면 null로 처리 (전체)
                clubType = null;
            }
        }

        // category 파라미터 처리
        ClubCategory clubCategory = null;
        if (category != null && !category.equals("null")) {
            try {
                clubCategory = ClubCategory.valueOf(category);
            } catch (IllegalArgumentException e) {
                // 잘못된 category 값이면 null로 처리 (전체)
                clubCategory = null;
            }
        }

        // recruiting 파라미터 처리
        Boolean recruitingStatus = null;
        if (recruiting != null && !recruiting.equals("null")) {
            if (recruiting.equals("true")) {
                recruitingStatus = true;
            } else if (recruiting.equals("false")) {
                recruitingStatus = false;
            }
            // 잘못된 값이면 null로 처리 (전체)
        }

        ClubListResponse response = ClubListResponse.from(
                clubUserService.getClubList(clubCategory, clubType, clubUniv, recruitingStatus, grades, size, cursor, sort, userEmail)
        );

        return ResponseEntity.ok(response);
    }


    /**
     * 메인 화면 배너용 인기 동아리 조회 API
     * 메인 화면 상단 배너에 표시될 인기 동아리를 조회합니다.
     * 화살표 버튼을 통해 다음/이전 페이지로 이동할 수 있습니다.
     *
     * @return (멤버수 x 0.7) + (즐겨찾기 수 x 0.3) 기준으로 정렬된 인기 동아리 목록
     * */
    @Override
    @GetMapping("/banner/popular")
    public ResponseEntity<ClubListResponse> getBannerPopularClubs(@AuthUserInfo String userEmail) {
        // 프론트엔드 요청으로 기존의 page, size 페이지네이션 방식의 파라미터 제거
        ClubListServiceResponse response = clubUserService.getAllPopularClubs(userEmail);
        return ResponseEntity.ok(ClubListResponse.from(response));
    }

    /**
     * 전체 인기 동아리 목록 조회 API
     * "더보기" 클릭 시 보여지는 전체 인기 동아리 목록을 조회합니다.
     * "인기도순", "멤버많은순", "최신등록순" 정렬과 무한스크롤을 지원합니다.
     *
     * @param size 페이지 크기 (기본값: 20)
     * @param cursor 무한스크롤 커서 (첫 요청시 생략)
     * @param sort 정렬 방식 (popular : 인기도순, member_count : 멤버많은 순, latest : 최신등록 순) - 기본값 : popular
     * @return 멤버수 기준으로 정렬된 인기 동아리 목록
     * */
    //FIXME - 마감 임박 추가 (지원 마감기간이 일주일 이내로 남은 동아리 boolean 필드 추가)
    @Override
    @GetMapping("/popular")
    public ResponseEntity<ClubListResponse> getPopularClubs(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "popular") String sort,
            @AuthUserInfo String userEmail) {

        ClubListServiceResponse response = clubUserService.getPopularClubsWithFilters(size, cursor, sort, userEmail);

        return ResponseEntity.ok(ClubListResponse.from(response));
    }

    /**
     * 동아리 필터링 옵션 조회 API
     * 프론트엔드에서 사용할 수 있는 모든 필터링 옵션을 제공합니다.
     * 
     * @return 사용 가능한 필터링 옵션들
     */
    @Override
    @GetMapping("/filter-options")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        Map<String, Object> options = new HashMap<>();
        
        // 동아리 타입 옵션
        List<Map<String, String>> typeOptions = Arrays.asList(
                Map.of("value", "null", "label", "전체"),
                Map.of("value", "CENTRAL", "label", "중앙동아리"),
                Map.of("value", "UNION", "label", "연합동아리"),
                Map.of("value", "DEPARTMENT", "label", "과동아리")
        );
        
        // 모집여부 옵션
        List<Map<String, String>> recruitingOptions = Arrays.asList(
                Map.of("value", "null", "label", "전체"),
                Map.of("value", "true", "label", "모집중"),
                Map.of("value", "false", "label", "모집마감")
        );
        
        // 대학 구분 옵션 (과동아리 선택 시 사용)
        List<Map<String, String>> universityOptions = Arrays.asList(
                Map.of("value", "GLOBAL_AREA", "label", "글로벌지역학부"),
                Map.of("value", "DESIGN", "label", "디자인대학"),
                Map.of("value", "ENGINEERING", "label", "공대"),
                Map.of("value", "CONVERGENCE_TECHNOLOGY", "label", "융합기술대"),
                Map.of("value", "ARTS", "label", "예술대")
        );
        
        options.put("types", typeOptions);
        options.put("recruitingOptions", recruitingOptions);
        options.put("universities", universityOptions);
        
        return ResponseEntity.ok(options);
    }

    /**
     * 동아리 검색 API
     * 동아리 이름을 기준으로 검색합니다.
     * 검색 결과는 커서 기반 페이지네이션을 지원합니다.
     * 검색 키워드가 동아리 이름에 포함되는 결과를 반환합니다.
     *
     * @param keyword 검색 키워드 (동아리 이름)
     * @param sort 정렬 기준 (latest, member, popular)
     * @param cursor 커서 기반 페이지네이션을 위한 기준 ID
     * @param size 페이지당 로드할 개수 (기본값 20)
     */
    //FIXME - 마감 임박 추가 (지원 마감기간이 일주일 이내로 남은 동아리 boolean 필드 추가)
    @Override
    @GetMapping("/search")
    public ResponseEntity<ClubListResponse> searchClubs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthUserInfo String userEmail
    ) {
        ClubListResponse response = ClubListResponse.from(
                clubUserService.searchClubs(keyword, sort, cursor, size, userEmail)
        );

        return ResponseEntity.ok(response);
    }
}
