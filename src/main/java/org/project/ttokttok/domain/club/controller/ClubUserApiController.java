package org.project.ttokttok.domain.club.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.controller.dto.response.ClubDetailResponse;
import org.project.ttokttok.domain.club.controller.dto.response.ClubListResponse;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.service.ClubUserService;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 동아리 관련 API 컨트롤러
 * 사용자가 동아리 정보를 조회할 수 있는 API들을 제공합니다.
 */
@Slf4j
@Tag(name="동아리 조회", description = "사용자가 동아리 정보를 조회하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs")
public class ClubUserApiController {

    private final ClubUserService clubUserService;

    /**
     * 동아리 상세 정보 조회 API
     * 특정 동아리의 상세 정보를 조회합니다.
     * 
     * @param username 인증된 사용자 이메일
     * @param clubId 조회할 동아리 ID
     * @return 동아리 상세 정보 (소개, 지원 정보, 멤버 수 등)
     */
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
    @Operation(
            summary = "동아리 목록 조회",
            description = "메인 화면 동아리 목록을 무한스크롤로 조회합니다. 카테고리, 분류, 모집여부 필터링 가능."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터")
    })
    @GetMapping
    public ResponseEntity<ClubListResponse> getClubList(
            @Parameter(description = "카테고리 (스포츠, 예술, 문화 등)")
            @RequestParam(required = false) ClubCategory category,

            @Parameter(description = "분류 (중앙, 연합, 과 동아리)")
            @RequestParam(required = false) ClubType type,

            @Parameter(
                    description = "학년 (1학년, 2학년, 3학년, 4학년)",
                    style = ParameterStyle.FORM,
                    explode = Explode.TRUE
            )
            @RequestParam(required = false) List<ApplicableGrade> grades,

            @Parameter(description = "모집여부 (true : 모집중, false : 모집마감)")
            @RequestParam(required = false) Boolean recruiting,

            @Parameter(description = "조회 개수 (기본 20개)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "무한스크롤 커서 (첫 요청시 생략)")
            @RequestParam(required = false) String cursor,    // cursor 추가

            @Parameter(description = "정렬 (latest: 최신등록순, popular: 인기도순, member_count: 멤버많은순)")
            @RequestParam(defaultValue = "latest") String sort) {

        ClubListResponse response = ClubListResponse.from(
                clubUserService.getClubList(category, type, recruiting, grades, size, cursor, sort)
        );

        return ResponseEntity.ok(response);
    }


    /**
     * 메인 화면 배너용 인기 동아리 조회 API
     * 메인 화면 상단 배너에 표시될 인기 동아리를 조회합니다.
     * 화살표 버튼을 통해 다음/이전 페이지로 이동할 수 있습니다.
     *
     * @param page 페이지 번호 (0부터 시작, 기본 0)
     * @param size 페이지당 동아리 수 (기본 4개)
     * @return 멤버수 기준으로 정렬된 인기 동아리 목록
     * */
    @Operation(
            summary = "메인 배너 인기 동아리 조회",
            description = "메인 화면 상단 배너에 표시될 인기 동아리를 조회합니다. 4개씩 페이지네이션으로 제공됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터")
    })
    @GetMapping("/banner/popular")
    public ResponseEntity<ClubListResponse> getBannerPopularClubs(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "조회할 동아리 수 (기본 4개)")
            @RequestParam(defaultValue = "4") int size) {

        ClubListServiceResponse response = clubUserService.getPopularClubs(page, size);

        return ResponseEntity.ok(ClubListResponse.from(response));
    }

    /**
     * 전체 인기 동아리 목록 조회 API
     * "더보기" 클릭 시 보여지는 전체 인기 동아리 목록을 조회합니다.
     * 카테고리, 분류, 모집여부 필터링과 함께 무한스크롤 또는 페이지네이션 제공됩니다.
     *
     * @param category 동아리 카테고리 필터 (봉사, 예술, 문화 등) - 선택사항
     * @param type 동아리 분류 필터 (중앙, 연합, 학과) - 선택사항
     * @param recruiting 모집 여부 필터 (true: 모집중, false: 모집마감) - 선택사항
     * @param size 페이지 크기 (기본값: 20)
     * @param cursor 무한스크롤 커서 (첫 요청시 생략)
     * @return 멤버수 기준으로 정렬된 인기 동아리 목록
     * */
    @Operation(
            summary = "전체 인기 동아리 목록 조회",
            description = "멤버수가 많은 순으로 전체 인기 동아리를 조회합니다. 필터링 및 무한스크롤 지원."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터")
    })
    @GetMapping("/popular")
    public ResponseEntity<ClubListResponse> getPopularClubs(
            @Parameter(description = "카테고리 (스포츠, 예술, 문화 등)")
            @RequestParam(required = false) ClubCategory category,

            @Parameter(description = "분류 (중앙, 연합, 과 동아리)")
            @RequestParam(required = false) ClubType type,

            @Parameter(description = "모집여부 (true : 모집중, false : 모집마감)")
            @RequestParam(required = false) Boolean recruiting,

            @Parameter(description = "조회 개수 (기본 20개)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "무한스크롤 커서 (첫 요청시 생략)")
            @RequestParam(required = false) String cursor) {

        ClubListServiceResponse response = clubUserService.getPopularClubsWithFilters(category, type, recruiting, size, cursor);

        return ResponseEntity.ok(ClubListResponse.from(response));
    }
}
