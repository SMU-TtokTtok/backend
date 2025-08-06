package org.project.ttokttok.domain.favorite.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.project.ttokttok.domain.favorite.controller.dto.response.FavoriteListResponse;
import org.project.ttokttok.domain.favorite.controller.dto.response.FavoriteToggleResponse;
import org.project.ttokttok.domain.favorite.service.FavoriteService;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteListServiceRequest;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteToggleServiceRequest;
import org.project.ttokttok.domain.favorite.service.dto.response.FavoriteToggleServiceResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 즐겨찾기 관련 API 컨트롤러
 * 사용자의 즐겨찾기 추가/제거 및 조회 기능을 제공합니다.
 */
@Slf4j
@Tag(name = "[사용자] 즐겨찾기", description = "동아리 즐겨찾기 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 즐겨찾기 토글 API
     * 동아리를 즐겨찾기에 추가하거나 제거합니다.
     *
     * @param clubId 동아리 ID
     * @return 즐겨찾기 토글 결과
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
    @PostMapping("/toggle/{clubId}")
    public ResponseEntity<FavoriteToggleResponse> toggleFavorite(
            @Parameter(hidden = true) @AuthUserInfo String userEmail,
            @Parameter(description = "동아리 ID", required = true)
            @PathVariable String clubId) {

        log.info("[즐겨찾기 토글] 요청 시작 - clubId: {}, userEmail: {}", clubId, userEmail);
        
        try {
            if (userEmail == null) {
                log.warn("[즐겨찾기 토글] 인증 실패 - userEmail이 null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.debug("[즐겨찾기 토글] 서비스 요청 생성 - userEmail: {}, clubId: {}", userEmail, clubId);
            FavoriteToggleServiceRequest request = FavoriteToggleServiceRequest.of(userEmail, clubId);
            
            log.debug("[즐겨찾기 토글] 서비스 메서드 호출 시작");
            FavoriteToggleServiceResponse serviceResponse = favoriteService.toggleFavorite(request);
            
            log.debug("[즐겨찾기 토글] 서비스 응답 생성 - favorited: {}", serviceResponse.favorited());
            FavoriteToggleResponse response = FavoriteToggleResponse.from(serviceResponse);

            log.info("[즐겨찾기 토글] 요청 완료 - clubId: {}, userEmail: {}, result: {}", 
                    clubId, userEmail, serviceResponse.favorited() ? "추가" : "제거");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[즐겨찾기 토글] 예외 발생 - clubId: {}, userEmail: {}, error: {}", 
                    clubId, userEmail, e.getMessage(), e);
            throw e; // 예외를 다시 던져서 GlobalExceptionHandler가 처리하도록
        }
    }

    /**
     * 즐겨찾기 목록 조회 API
     * 사용자의 즐겨찾기한 동아리 목록을 조회합니다.
     *
     * @param userEmail 인증된 사용자 이메일
     * @return 즐겨찾기 동아리 목록
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
    @GetMapping
    public ResponseEntity<FavoriteListResponse> getFavoriteList(
            @Parameter(hidden = true) @AuthUserInfo String userEmail,

            @Parameter(description = "다음 페이지 커서 ID")
            @RequestParam(required = false) String cursor,

            @Parameter(description = "조회할 개수")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "정렬 (latest: 최신등록순, popular: 인기도순, member_count: 멤버많은순)")
            @RequestParam(defaultValue = "latest") String sort) {

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FavoriteListServiceRequest request = FavoriteListServiceRequest.builder()
                .userEmail(userEmail)
                .cursor(cursor)
                .size(size)
                .sort(sort)
                .build();

        FavoriteListResponse response = FavoriteListResponse.from(
                favoriteService.getFavoriteList(request)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 즐겨찾기 상태 확인 API
     * 특정 동아리의 즐겨찾기 상태를 확인합니다.
     *
     * @param userEmail 인증된 사용자 이메일
     * @param clubId 동아리 ID
     * @return 즐겨찾기 여부
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
    @GetMapping("/status/{clubId}")
    public ResponseEntity<Boolean> getFavoriteStatus(
            @Parameter(hidden = true) @AuthUserInfo String userEmail,
            @Parameter(description = "동아리 ID", required = true)
            @PathVariable String clubId) {

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isFavorited = favoriteService.isFavorited(userEmail, clubId);

        return ResponseEntity.ok(isFavorited);
    }
} 