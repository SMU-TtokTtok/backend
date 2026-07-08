package org.project.ttokttok.domain.club.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.service.ClubUserService;
import org.project.ttokttok.domain.club.service.dto.response.ClubDetailServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClubUserApiController.class)
class ClubUserApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubUserService clubUserService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    private static final String USER_EMAIL = "test@sangmyung.kr";
    private static final String CLUB_ID = "club-1";

    private void givenAuthenticatedUser() throws Exception {
        given(authUserInfoResolver.supportsParameter(any())).willReturn(true);
        given(authUserInfoResolver.resolveArgument(any(), any(), any(), any())).willReturn(USER_EMAIL);
    }

    @Test
    @WithMockUser
    @DisplayName("동아리 상세 조회 API를 호출한다")
    void getClubIntroduction() throws Exception {
        givenAuthenticatedUser();

        ClubDetailServiceResponse response = ClubDetailServiceResponse.builder()
                .name("테스트 동아리")
                .clubType(ClubType.CENTRAL)
                .clubCategory(ClubCategory.ACADEMIC)
                .customCategory("")
                .bookmarked(true)
                .recruiting(true)
                .isDeadlineImminent(false)
                .summary("한줄 소개")
                .profileImageUrl(null)
                .clubMemberCount(5)
                .applyStartDate(null)
                .applyDeadLine(null)
                .grades(Collections.emptySet())
                .maxApplyCount(10)
                .content("소개 내용")
                .build();
        given(clubUserService.getClubIntroduction(USER_EMAIL, CLUB_ID)).willReturn(response);

        mockMvc.perform(get("/api/clubs/{clubId}/content", CLUB_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("테스트 동아리"))
                .andExpect(jsonPath("$.bookmarked").value(true))
                .andExpect(jsonPath("$.clubMemberCount").value(5));
    }

    @Test
    @WithMockUser
    @DisplayName("동아리 목록 조회 API를 호출한다")
    void getClubList() throws Exception {
        givenAuthenticatedUser();

        ClubListServiceResponse response = new ClubListServiceResponse(List.of(), 0, 0L, false, null);
        given(clubUserService.getClubList(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(20),
                any(), org.mockito.ArgumentMatchers.eq("latest"), org.mockito.ArgumentMatchers.eq(USER_EMAIL)))
                .willReturn(response);

        mockMvc.perform(get("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs").isArray())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("메인 배너 인기 동아리 조회 API를 호출한다")
    void getBannerPopularClubs() throws Exception {
        givenAuthenticatedUser();

        ClubListServiceResponse response = new ClubListServiceResponse(List.of(), 0, 0L, false, null);
        given(clubUserService.getAllPopularClubs(USER_EMAIL)).willReturn(response);

        mockMvc.perform(get("/api/clubs/banner/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("필터 옵션 조회 API를 호출한다")
    void getFilterOptions() throws Exception {
        mockMvc.perform(get("/api/clubs/filter-options")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types").isArray())
                .andExpect(jsonPath("$.recruitingOptions").isArray())
                .andExpect(jsonPath("$.universities").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("동아리 검색 API를 호출한다")
    void searchClubs() throws Exception {
        givenAuthenticatedUser();

        ClubListServiceResponse response = new ClubListServiceResponse(List.of(), 0, 3L, false, null);
        given(clubUserService.searchClubs(org.mockito.ArgumentMatchers.eq("코딩"), org.mockito.ArgumentMatchers.eq("latest"),
                any(), org.mockito.ArgumentMatchers.eq(20), org.mockito.ArgumentMatchers.eq(USER_EMAIL)))
                .willReturn(response);

        mockMvc.perform(get("/api/clubs/search")
                        .param("keyword", "코딩")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3));
    }
}
