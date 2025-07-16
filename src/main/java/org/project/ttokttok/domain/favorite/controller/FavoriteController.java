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
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 즐겨찾기 관련 API 컨트롤러
 * 사용자의 즐겨찾기 추가/제거 및 조회 기능을 제공합니다.
 */
@Slf4j
@Tag(name = "즐겨찾기", description = "동아리 즐겨찾기 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 즐겨찾기 토글 API
     * 동아리를 즐겨찾기에 추가하거나 제거합니다.
     *
     * @param tokenUserEmail 인증된 사용자 이메일
     * @param clubId 동아리 ID
     * @return 즐겨찾기 토글 결과
     */
    @Operation(
            summary = "즐겨찾기 토글",
            description = """
                    동아리를 즐겨찾기에 추가하거나 제거합니다. 이미 즐겨찾기가 되어 있으면 제거하고, 즐겨찾기가 안되어 있으면 즐겨찾기에 추가합니다.
    
                    ===프론트엔드 개발자를 위한 테스트 가이드===
    
                    **실제 프론트엔드 연동 시**
                    - 회원가입 -> 로그인 완료 후 JWT 토큰이 쿠키에 저장됨

                    ⚡ **Swagger UI에서 테스트 시:**
                    1. 회원가입 → 로그인 완료
                    2. tokenUserEmail에 **회원가입할 때 사용한 본인 이메일** 직접 입력
                    3. userEmail은 비워두기 (테스트용, 입력 불필요)
                    4. clubId에 테스트할 동아리 ID 입력 (예: club-025)

                    ⚡ **빠른 테스트 (회원가입 없이):**
                    1. tokenUserEmail에 "test@sangmyung.kr" 입력
                    2. userEmail에도 마찬가지로 "test@sangmyung.kr" 입력
                    3. clubId에 동아리 ID 입력

                    **주의사항:**
                    - Swagger UI에서는 tokenUserEmail을 수동으로 입력해야 합니다
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "즐겨찾기 토글 성공"),
            @ApiResponse(responseCode = "404", description = "동아리를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/toggle/{clubId}")
    public ResponseEntity<FavoriteToggleResponse> toggleFavorite(
            @Parameter(description = "JWT 토큰에서 추출한 사용자 이메일", required = false)
            @AuthUserInfo String tokenUserEmail,
            @Parameter(description = "동아리 ID", required = true)
            @PathVariable String clubId,
            @Parameter(description = "테스트용 사용자 이메일", required = false, hidden = false)
            @RequestParam(required = false) String userEmail) {

        // 테스트용 파라미터가 있으면 사용, 없으면 JWT에서 추출한 값 사용
        String actualUserEmail = userEmail != null ? userEmail : tokenUserEmail;

        if (actualUserEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FavoriteToggleServiceRequest request = FavoriteToggleServiceRequest.of(actualUserEmail, clubId);
        FavoriteToggleResponse response = FavoriteToggleResponse.from(
                favoriteService.toggleFavorite(request)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 즐겨찾기 목록 조회 API
     * 사용자의 즐겨찾기한 동아리 목록을 조회합니다.
     *
     * @param tokenUserEmail 인증된 사용자 이메일
     * @return 즐겨찾기 동아리 목록
     */
    @Operation(
            summary = "즐겨찾기 목록 조회",
            description = """
                    현재 로그인한 사용자의 즐겨찾기 동아리 목록을 조회합니다.

                    === 프론트엔드 개발자를 위한 테스트 가이드 ===
                    
                    **실제 프론트엔드 연동 시**
                    - 회원가입 -> 로그인 완료 후 JWT 토큰이 쿠키에 저장됨
                    
                    ⚡ **Swagger UI에서 테스트 시:**
                    1. 회원가입 → 로그인 완료
                    2. tokenUserEmail에 **회원가입할 때 사용한 본인 이메일** 직접 입력
                    3. userEmail은 비워두기 (테스트용, 입력 불필요)

                    ⚡ **빠른 테스트 (회원가입 없이):**
                    1. tokenUserEmail에 "test@sangmyung.kr" 입력
                    2. userEmail에도 마찬가지로 "test@sangmyung.kr" 입력
                    
                    **주의사항:**
                    - Swagger UI에서는 tokenUserEmail을 수동으로 입력해야 합니다
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "즐겨찾기 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping
    public ResponseEntity<FavoriteListResponse> getFavoriteList(
            @Parameter(description = "JWT 토큰에서 추출한 사용자 이메일", required = false)
            @AuthUserInfo String tokenUserEmail,

            @Parameter(description = "테스트용 사용자 이메일", required = false, hidden = false)
            @RequestParam(required = false) String userEmail,

            @Parameter(description = "다음 페이지 커서 ID")
            @RequestParam(required = false) String cursor,

            @Parameter(description = "조회할 개수")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "정렬 기준")
            @RequestParam(defaultValue = "latest") String sort) {

        // 테스트용 파라미터가 있으면 사용, 없으면 JWT에서 추출한 값 사용
        String actualUserEmail = userEmail != null ? userEmail : tokenUserEmail;

        if (actualUserEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FavoriteListServiceRequest request = FavoriteListServiceRequest.builder()
                .userEmail(actualUserEmail)
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
     * @param tokenUserEmail 인증된 사용자 이메일
     * @param clubId 동아리 ID
     * @return 즐겨찾기 여부
     */
    @Operation(
            summary = "즐겨찾기 상태 확인",
            description = """
                    특정 동아리가 즐겨찾기 되어 있는지 확인합니다.
                    
                    === 프론트엔드 개발자를 위한 테스트 가이드 ===
                    
                    **실제 프론트엔드 연동 시**
                    - 회원가입 -> 로그인 완료 후 JWT 토큰이 쿠키에 저장됨
                    
                    ⚡ **Swagger UI에서 테스트 시:**
                    1. 회원가입 → 로그인 완료
                    2. tokenUserEmail에 **회원가입할 때 사용한 본인 이메일** 직접 입력
                    3. userEmail은 비워두기 (테스트용, 입력 불필요)
                    4. clubId에 테스트할 동아리 ID 입력 (예: club-025)
                    5. 만약에 즐겨찾기 되어 있다면 true 로 나올것이고 안되어 있다면 false 로 나올것입니다. (동아리 카드에 별표 아이콘에 불들어오냐 안들어오냐 판단용)

                    ⚡ **빠른 테스트 (회원가입 없이):**
                    1. tokenUserEmail에 "test@sangmyung.kr" 입력
                    2. userEmail에도 마찬가지로 "test@sangmyung.kr" 입력
                    3. clubId에 테스트할 동아리 ID 입력 (예: club-025)
                    4. 마찬가지로 즐겨찾기 되어 있다면 true 로 나올것이고 안되어 있다면 false 로 나올것입니다. (동아리 카드에 별표 아이콘에 불들어오냐 안들어오냐 판단용)
                    
                    **주의사항:**
                    - Swagger UI에서는 tokenUserEmail을 수동으로 입력해야 합니다
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "즐겨찾기 상태 확인 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/status/{clubId}")
    public ResponseEntity<Boolean> getFavoriteStatus(
            @Parameter(description = "JWT 토큰에서 추출한 사용자 이메일", required = false)
            @AuthUserInfo String tokenUserEmail,
            @Parameter(description = "동아리 ID", required = true)
            @PathVariable String clubId,
            @Parameter(description = "테스트용 사용자 이메일", required = false, hidden = false)
            @RequestParam(required = false) String userEmail) {

        // 테스트용 파라미터가 있으면 사용, 없으면 JWT에서 추출한 값 사용
        String actualUserEmail = userEmail != null ? userEmail : tokenUserEmail;

        if (actualUserEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isFavorited = favoriteService.isFavorited(actualUserEmail, clubId);

        return ResponseEntity.ok(isFavorited);
    }
} 