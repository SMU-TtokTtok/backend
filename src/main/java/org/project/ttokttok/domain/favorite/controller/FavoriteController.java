package org.project.ttokttok.domain.favorite.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.project.ttokttok.domain.favorite.controller.docs.FavoriteDocs;
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
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteController implements FavoriteDocs {

    private final FavoriteService favoriteService;

    /**
     * 즐겨찾기 토글 API
     * 동아리를 즐겨찾기에 추가하거나 제거합니다.
     *
     * @param clubId 동아리 ID
     * @return 즐겨찾기 토글 결과
     */
    @Override
    @PostMapping("/toggle/{clubId}")
    public ResponseEntity<FavoriteToggleResponse> toggleFavorite(
            @AuthUserInfo String userEmail,
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
    @Override
    @GetMapping
    public ResponseEntity<FavoriteListResponse> getFavoriteList(
            @AuthUserInfo String userEmail,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
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
    @Override
    @GetMapping("/status/{clubId}")
    public ResponseEntity<Boolean> getFavoriteStatus(
            @AuthUserInfo String userEmail,
            @PathVariable String clubId) {

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isFavorited = favoriteService.isFavorited(userEmail, clubId);

        return ResponseEntity.ok(isFavorited);
    }
}
 