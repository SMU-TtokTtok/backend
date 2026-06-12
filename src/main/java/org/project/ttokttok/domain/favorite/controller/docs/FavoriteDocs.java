package org.project.ttokttok.domain.favorite.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.favorite.controller.dto.response.FavoriteListResponse;
import org.project.ttokttok.domain.favorite.controller.dto.response.FavoriteToggleResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 즐겨찾기 관련 API 문서화 인터페이스
 */
@Tag(name = "[사용자] 즐겨찾기", description = "동아리 즐겨찾기 관련 API")
public interface FavoriteDocs {

    /**
     * 즐겨찾기 토글 API
     */
    @Operation(
            summary = "즐겨찾기 토글",
            description = """
                    동아리를 즐겨찾기에 추가하거나 제거합니다.
                    이미 즐겨찾기가 되어 있으면 제거하고, 즐겨찾기가 안되어 있으면 즐겨찾기에 추가합니다.
                    
                    **[사용 방법]**
                    1. 우측 상단의 `Authorize` 버튼을 클릭합니다.
                    2. 로그인 API를 통해 발급받은 `accessToken` 값을 `Value`에 붙여넣고 `Authorize` 버튼을 누릅니다.
                    3. API를 실행하면, 인증된 사용자의 정보로 요청이 전송됩니다.
                    
                    **[테스트 계정]**
                    - 이메일: `test@sangmyung.kr`
                    - 비밀번호: `TestPass123!`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "즐겨찾기 토글 성공"),
            @ApiResponse(responseCode = "404", description = "동아리를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    ResponseEntity<FavoriteToggleResponse> toggleFavorite(
            @Parameter(hidden = true) String userEmail,
            @Parameter(description = "동아리 ID", required = true)
            @PathVariable String clubId);

    /**
     * 즐겨찾기 목록 조회 API
     */
    @Operation(
            summary = "즐겨찾기 목록 조회",
            description = """
                    현재 로그인한 사용자의 즐겨찾기 동아리 목록을 조회합니다.

                    **[사용 방법]**
                    1. 우측 상단의 `Authorize` 버튼을 클릭합니다.
                    2. 로그인 API를 통해 발급받은 `accessToken` 값을 `Value`에 붙여넣고 `Authorize` 버튼을 누릅니다.
                    3. API를 실행하면, 인증된 사용자의 정보로 요청이 전송됩니다.
                    
                    **[테스트 계정]**
                    - 이메일: `test@sangmyung.kr`
                    - 비밀번호: `TestPass123!`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "즐겨찾기 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    ResponseEntity<FavoriteListResponse> getFavoriteList(
            @Parameter(hidden = true) String userEmail,
            @Parameter(description = "다음 페이지 커서 ID")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "조회할 개수")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 (latest: 최신등록순, popular: 인기도순, member_count: 멤버많은순)")
            @RequestParam(defaultValue = "latest") String sort);

    /**
     * 즐겨찾기 상태 확인 API
     */
    @Operation(
            summary = "즐겨찾기 상태 확인",
            description = """
                    특정 동아리가 즐겨찾기 되어 있는지 확인합니다.
                    
                    **[사용 방법]**
                    1. 우측 상단의 `Authorize` 버튼을 클릭합니다.
                    2. 로그인 API를 통해 발급받은 `accessToken` 값을 `Value`에 붙여넣고 `Authorize` 버튼을 누릅니다.
                    3. API를 실행하면, 인증된 사용자의 정보로 요청이 전송됩니다.
                    
                    **[테스트 계정]**
                    - 이메일: `test@sangmyung.kr`
                    - 비밀번호: `TestPass123!`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "즐겨찾기 상태 확인 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    ResponseEntity<Boolean> getFavoriteStatus(
            @Parameter(hidden = true) String userEmail,
            @Parameter(description = "동아리 ID", required = true)
            @PathVariable String clubId);
}
