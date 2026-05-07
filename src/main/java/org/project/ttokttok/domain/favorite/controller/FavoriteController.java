package org.project.ttokttok.domain.favorite.controller;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.favorite.controller.docs.FavoriteDocs;
import org.project.ttokttok.domain.favorite.controller.dto.response.FavoriteListResponse;
import org.project.ttokttok.domain.favorite.controller.dto.response.FavoriteToggleResponse;
import org.project.ttokttok.domain.favorite.service.FavoriteService;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteListServiceRequest;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteToggleServiceRequest;
import org.project.ttokttok.domain.favorite.service.dto.response.FavoriteToggleServiceResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 즐겨찾기 관련 API 컨트롤러
 * 사용자의 즐겨찾기 추가/제거 및 조회 기능을 제공합니다.
 */
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

        FavoriteToggleServiceRequest request = FavoriteToggleServiceRequest.of(userEmail, clubId);
        FavoriteToggleServiceResponse serviceResponse = favoriteService.toggleFavorite(request);
        FavoriteToggleResponse response = FavoriteToggleResponse.from(serviceResponse);

        return ResponseEntity.ok(response);
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

        boolean isFavorited = favoriteService.isFavorited(userEmail, clubId);

        return ResponseEntity.ok(isFavorited);
    }
}
 