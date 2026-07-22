package org.project.ttokttok.domain.clubboard.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse;
import org.project.ttokttok.domain.clubboard.controller.dto.response.ClubBoardListResponse.ClubBoardSummary;
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

    @Test
    @WithMockUser
    @DisplayName("게시판 목록 조회 API를 기본 파라미터로 호출한다")
    void getBoardListWithDefaultParams() throws Exception {
        String thumbnailUrl = "https://cdn.example.com/board-images/uuid_thumb.png";
        ClubBoardSummary summary = new ClubBoardSummary(
                "board-1", "제목", "내용", "동아리", thumbnailUrl, true, LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        given(clubBoardUserService.getBoardList(CLUB_ID, 20, null))
                .willReturn(ClubBoardListResponse.of(List.of(summary), false, null));

        mockMvc.perform(get("/api/clubs/{clubId}/boards", CLUB_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boards[0].boardId").value("board-1"))
                .andExpect(jsonPath("$.boards[0].thumbnailUrl").value(thumbnailUrl))
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
}
