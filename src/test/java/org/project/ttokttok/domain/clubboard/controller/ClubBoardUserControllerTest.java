package org.project.ttokttok.domain.clubboard.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardDetailResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse.ClubBoardSummary;
import org.project.ttokttok.domain.clubboard.exception.ClubBoardNotFoundException;
import org.project.ttokttok.domain.clubboard.service.ClubBoardUserService;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClubBoardUserController.class)
class ClubBoardUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubBoardUserService clubBoardUserService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    private static final String CLUB_ID = "club-1";
    private static final String THUMBNAIL_URL = "https://cdn.example.com/board-images/uuid_thumb.png";

    @Test
    @WithMockUser
    @DisplayName("게시판 목록 조회 API를 기본 파라미터로 호출한다")
    void getBoardListWithDefaultParams() throws Exception {
        ClubBoardSummary summary = new ClubBoardSummary(
                "board-1", THUMBNAIL_URL, LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        given(clubBoardUserService.getBoardList(CLUB_ID, 20, null))
                .willReturn(ClubBoardListResponse.of(List.of(summary), false, null));

        mockMvc.perform(get("/api/clubs/{clubId}/boards", CLUB_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boards[0].boardId").value("board-1"))
                .andExpect(jsonPath("$.boards[0].thumbnailUrl").value(THUMBNAIL_URL))
                .andExpect(jsonPath("$.boards[0].content").doesNotExist())
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(clubBoardUserService).getBoardList(CLUB_ID, 20, null);
    }

    @Test
    @WithMockUser
    @DisplayName("size와 cursor 파라미터를 전달하여 게시판 목록을 조회한다")
    void getBoardListWithCursorAndSize() throws Exception {
        given(clubBoardUserService.getBoardList(eq(CLUB_ID), eq(5), eq("cursor-1")))
                .willReturn(ClubBoardListResponse.of(List.of(), true, "cursor-2"));

        mockMvc.perform(get("/api/clubs/{clubId}/boards", CLUB_ID)
                        .param("size", "5")
                        .param("cursor", "cursor-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").value("cursor-2"));
    }

    @Test
    @WithMockUser
    @DisplayName("게시판 상세 조회 API를 호출한다")
    void getBoardDetail() throws Exception {
        ClubBoardDetailResponse response = new ClubBoardDetailResponse(
                "board-1", "제목", "본문 전체 내용", THUMBNAIL_URL, "동아리", LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        given(clubBoardUserService.getBoardDetail(CLUB_ID, "board-1"))
                .willReturn(response);

        mockMvc.perform(get("/api/clubs/{clubId}/boards/{boardId}", CLUB_ID, "board-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardId").value("board-1"))
                .andExpect(jsonPath("$.title").value("제목"))
                .andExpect(jsonPath("$.content").value("본문 전체 내용"))
                .andExpect(jsonPath("$.thumbnailUrl").value(THUMBNAIL_URL))
                .andExpect(jsonPath("$.clubName").value("동아리"));
    }

    @Test
    @WithMockUser
    @DisplayName("존재하지 않는 게시글의 상세 조회는 404를 반환한다")
    void getBoardDetailNotFound() throws Exception {
        given(clubBoardUserService.getBoardDetail(CLUB_ID, "missing"))
                .willThrow(new ClubBoardNotFoundException());

        mockMvc.perform(get("/api/clubs/{clubId}/boards/{boardId}", CLUB_ID, "missing")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
