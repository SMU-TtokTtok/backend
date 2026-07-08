package org.project.ttokttok.domain.memo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.memo.controller.dto.request.MemoRequest;
import org.project.ttokttok.domain.memo.service.MemoService;
import org.project.ttokttok.domain.memo.service.dto.request.CreateMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.DeleteMemoServiceRequest;
import org.project.ttokttok.domain.memo.service.dto.request.UpdateMemoServiceRequest;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemoApiController.class)
class MemoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemoService memoService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    private static final String USERNAME = "admin1234";
    private static final String APPLICANT_ID = "applicant-1";
    private static final String MEMO_ID = "memo-1";

    private void givenAuthenticatedAdmin() throws Exception {
        given(authUserInfoResolver.supportsParameter(any())).willReturn(true);
        given(authUserInfoResolver.resolveArgument(any(), any(), any(), any())).willReturn(USERNAME);
    }

    @Test
    @WithMockUser
    @DisplayName("메모 생성 API를 호출하면 memoId를 응답한다")
    void createMemo() throws Exception {
        // given
        givenAuthenticatedAdmin();
        MemoRequest request = new MemoRequest("면접 태도가 좋았음");
        given(memoService.createMemo(eq(USERNAME), any(CreateMemoServiceRequest.class)))
                .willReturn(MEMO_ID);

        // when & then
        mockMvc.perform(post("/api/admin/applies/{applicantId}/memos", APPLICANT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memoId").value(MEMO_ID));
    }

    @Test
    @WithMockUser
    @DisplayName("메모 생성 시 content가 비어있으면 400을 응답한다")
    void createMemo_BlankContent_BadRequest() throws Exception {
        // given
        givenAuthenticatedAdmin();
        MemoRequest request = new MemoRequest(" ");

        // when & then
        mockMvc.perform(post("/api/admin/applies/{applicantId}/memos", APPLICANT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("메모 수정 API를 호출하면 성공 메시지를 응답한다")
    void updateMemo() throws Exception {
        // given
        givenAuthenticatedAdmin();
        MemoRequest request = new MemoRequest("수정된 메모 내용");

        // when & then
        mockMvc.perform(patch("/api/admin/applies/{applicantId}/memos/{memoId}", APPLICANT_ID, MEMO_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("메모 수정에 성공했습니다."));

        verify(memoService).updateMemo(eq(USERNAME), any(UpdateMemoServiceRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("메모 삭제 API를 호출하면 성공 메시지를 응답한다")
    void deleteMemo() throws Exception {
        // given
        givenAuthenticatedAdmin();

        // when & then
        mockMvc.perform(delete("/api/admin/applies/{applicantId}/memos/{memoId}", APPLICANT_ID, MEMO_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("메모 삭제에 성공했습니다."));

        verify(memoService).deleteMemo(eq(USERNAME), any(DeleteMemoServiceRequest.class));
    }
}
