package org.project.ttokttok.domain.favorite.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.favorite.service.FavoriteService;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteListServiceRequest;
import org.project.ttokttok.domain.favorite.service.dto.request.FavoriteToggleServiceRequest;
import org.project.ttokttok.domain.favorite.service.dto.response.FavoriteListServiceResponse;
import org.project.ttokttok.domain.favorite.service.dto.response.FavoriteToggleServiceResponse;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    @Test
    @WithMockUser
    @DisplayName("즐겨찾기 토글 API를 호출한다")
    void toggleFavorite() throws Exception {
        // given
        String userEmail = "test@test.com";
        String clubId = "club-1";
        given(authUserInfoResolver.supportsParameter(any())).willReturn(true);
        given(authUserInfoResolver.resolveArgument(any(), any(), any(), any())).willReturn(userEmail);
        given(favoriteService.toggleFavorite(any(FavoriteToggleServiceRequest.class)))
                .willReturn(FavoriteToggleServiceResponse.of(clubId, true));

        // when & then
        mockMvc.perform(post("/api/favorites/toggle/{clubId}", clubId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubId").value(clubId))
                .andExpect(jsonPath("$.favorited").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("즐겨찾기 목록 조회 API를 호출한다")
    void getFavoriteList() throws Exception {
        // given
        String userEmail = "test@test.com";
        given(authUserInfoResolver.supportsParameter(any())).willReturn(true);
        given(authUserInfoResolver.resolveArgument(any(), any(), any(), any())).willReturn(userEmail);
        given(favoriteService.getFavoriteList(any(FavoriteListServiceRequest.class)))
                .willReturn(new FavoriteListServiceResponse(Collections.emptyList(), null, false));

        // when & then
        mockMvc.perform(get("/api/favorites")
                        .param("size", "10")
                        .param("sort", "latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteClubs").isArray())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("즐겨찾기 상태 확인 API를 호출한다")
    void getFavoriteStatus() throws Exception {
        // given
        String userEmail = "test@test.com";
        String clubId = "club-1";
        given(authUserInfoResolver.supportsParameter(any())).willReturn(true);
        given(authUserInfoResolver.resolveArgument(any(), any(), any(), any())).willReturn(userEmail);
        given(favoriteService.isFavorited(userEmail, clubId)).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/favorites/status/{clubId}", clubId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}
