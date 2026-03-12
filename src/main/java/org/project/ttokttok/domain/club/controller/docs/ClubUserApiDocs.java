package org.project.ttokttok.domain.club.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.controller.dto.response.ClubDetailResponse;
import org.project.ttokttok.domain.club.controller.dto.response.ClubListResponse;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * 동아리 사용자 조회 API 문서화 인터페이스
 * 사용자가 동아리 정보를 조회할 수 있는 API들의 Swagger 문서를 정의합니다.
 */
@Tag(name = "[사용자] 동아리 조회", description = "사용자가 동아리 정보를 조회하는 API")
public interface ClubUserApiDocs {

    /**
     * 동아리 상세 정보 조회 API
     */
    @Operation(
            summary = "동아리 소개글 조회",
            description = """
                    동아리 타고 들어갔을때의 소개글과 모집인원, 지원가능 학년 등을 조회합니다.
                    마감 임박 여부(일주일 이내)도 포함됩니다.
                    """
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ClubDetailResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 파라미터",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "동아리를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<ClubDetailResponse> getClubIntroduction(
            @Parameter(hidden = true)
            String username,

            @Parameter(
                    description = "조회할 동아리 ID",
                    required = true,
                    example = "UUID"
            )
            String clubId
    );

    /**
     * 동아리 목록 조회 API (메인화면 + 필터링 통합)
     */
    @Operation(
            summary = "동아리 목록 조회",
            description = """
                    메인 화면 동아리 목록을 무한스크롤로 조회합니다.
                    카테고리, 분류, 모집여부 필터링 가능.
                    
                    **필터링 옵션**:
                    - `category`: 동아리 카테고리 (스포츠, 예술, 문화 등)
                    - `type`: 동아리 분류 (CENTRAL: 중앙, UNION: 연합, DEPARTMENT: 과동아리)
                    - `clubUniv`: 대학 구분 (과동아리 선택 시 사용)
                    - `grades`: 지원 가능 학년 (복수 선택 가능)
                    - `recruiting`: 모집 여부 (true: 모집중, false: 모집마감)
                    
                    **정렬 옵션**:
                    - `latest`: 최신등록순 (기본값)
                    - `popular`: 인기도순
                    - `member_count`: 멤버많은순
                    
                    **무한스크롤**:
                    - 첫 요청: cursor 없이 요청
                    - 다음 요청: 응답의 nextCursor 값을 사용
                    """
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ClubListResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 파라미터",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<ClubListResponse> getClubList(
            @Parameter(description = "카테고리 (스포츠, 예술, 문화 등)")
            String category,

            @Parameter(description = "분류 (전체: null, 중앙: CENTRAL, 연합: UNION, 과동아리: DEPARTMENT)")
            String type,

            @Parameter(description = "대학 구분 (글로벌지역학부, 디자인대학, 공대, 융합기술대, 예술대)")
            ClubUniv clubUniv,

            @Parameter(
                    description = "학년 (1학년, 2학년, 3학년, 4학년)",
                    style = ParameterStyle.FORM,
                    explode = Explode.TRUE
            )
            List<ApplicableGrade> grades,

            @Parameter(description = "모집여부 (전체: null, 모집중: true, 모집마감: false)")
            String recruiting,

            @Parameter(description = "조회 개수 (기본 20개)", example = "20")
            int size,

            @Parameter(description = "무한스크롤 커서 (첫 요청시 생략)")
            String cursor,

            @Parameter(
                    description = "정렬 (latest: 최신등록순, popular: 인기도순, member_count: 멤버많은순)",
                    example = "latest",
                    schema = @Schema(allowableValues = {"latest", "popular", "member_count"})
            )
            String sort,

            @Parameter(hidden = true)
            String userEmail
    );

    /**
     * 메인 화면 배너용 인기 동아리 조회 API
     */
    @Operation(
            summary = "메인 배너 인기 동아리 조회",
            description = """
                    메인 화면 상단 배너에 표시될 모든 인기 동아리를 한번에 조회합니다.
                    (멤버수 x 0.7) + (즐겨찾기 수 x 0.3) 기준으로 정렬됩니다.
                    """
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ClubListResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 파라미터",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<ClubListResponse> getBannerPopularClubs(
            @Parameter(hidden = true)
            String userEmail
    );

    /**
     * 전체 인기 동아리 목록 조회 API
     */
    @Operation(
            summary = "전체 인기 동아리 목록 조회",
            description = """
                    전체 인기 동아리를 조회합니다.
                    '인기도순', '멤버많은순', '최신등록순' 정렬 및 무한스크롤을 지원합니다.
                    
                    **정렬 옵션**:
                    - `popular`: 인기도순 (기본값)
                    - `member_count`: 멤버많은순
                    - `latest`: 최신등록순
                    
                    **무한스크롤**:
                    - 첫 요청: cursor 없이 요청
                    - 다음 요청: 응답의 nextCursor 값을 사용
                    """
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ClubListResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 파라미터",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<ClubListResponse> getPopularClubs(
            @Parameter(description = "조회 개수 (기본 20개)", example = "20")
            int size,

            @Parameter(description = "무한스크롤 커서 (첫 요청시 생략)")
            String cursor,

            @Parameter(
                    description = "정렬 (popular: 인기도순, member_count: 멤버많은순, latest: 최신등록순)",
                    example = "popular",
                    schema = @Schema(allowableValues = {"popular", "member_count", "latest"})
            )
            String sort,

            @Parameter(hidden = true)
            String userEmail
    );

    /**
     * 동아리 필터링 옵션 조회 API
     */
    @Operation(
            summary = "동아리 필터링 옵션 조회",
            description = """
                    동아리 목록 필터링에 사용할 수 있는 모든 옵션을 조회합니다.
                    
                    **반환 옵션**:
                    - `types`: 동아리 타입 옵션 (전체, 중앙동아리, 연합동아리, 과동아리)
                    - `recruitingOptions`: 모집여부 옵션 (전체, 모집중, 모집마감)
                    - `universities`: 대학 구분 옵션 (과동아리 선택 시 사용)
                    """
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 파라미터",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<Map<String, Object>> getFilterOptions();

    /**
     * 동아리 검색 API
     */
    @Operation(
            summary = "동아리 검색",
            description = """
                    동아리 이름을 기준으로 검색합니다.
                    검색 키워드가 동아리 이름에 포함되는 결과를 반환합니다.
                    
                    **정렬 옵션**:
                    - `latest`: 최신등록순 (기본값)
                    - `popular`: 인기도순
                    - `member_count`: 멤버많은순
                    
                    **무한스크롤**:
                    - 첫 요청: cursor 없이 요청
                    - 다음 요청: 응답의 nextCursor 값을 사용
                    """
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "검색 성공",
                            content = @Content(schema = @Schema(implementation = ClubListResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 파라미터",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<ClubListResponse> searchClubs(
            @Parameter(
                    description = "검색 키워드 (동아리 이름)",
                    required = true,
                    example = "밴드"
            )
            String keyword,

            @Parameter(
                    description = "정렬 (latest: 최신등록순, popular: 인기도순, member_count: 멤버많은순)",
                    example = "latest",
                    schema = @Schema(allowableValues = {"latest", "popular", "member_count"})
            )
            String sort,

            @Parameter(description = "무한스크롤 커서 (첫 요청시 생략)")
            String cursor,

            @Parameter(description = "조회 개수 (기본 20개)", example = "20")
            int size,

            @Parameter(hidden = true)
            String userEmail
    );
}

