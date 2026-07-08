package org.project.ttokttok.domain.temp.applyform.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.temp.applyform.controller.dto.request.TempApplyFormSaveRequest;
import org.project.ttokttok.domain.temp.applyform.service.TempApplyFormService;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TempApplyFormController.class)
class TempApplyFormControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TempApplyFormService tempApplyFormService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    private void mockAuthUser(String email) throws Exception {
        given(authUserInfoResolver.supportsParameter(any())).willReturn(true);
        given(authUserInfoResolver.resolveArgument(any(), any(), any(), any())).willReturn(email);
    }

    @Test
    @WithMockUser
    @DisplayName("임시 지원폼 저장 API를 호출하면 201과 저장된 아이디를 반환한다")
    void saveTempApplyForm_ShouldReturnCreated() throws Exception {
        // given
        mockAuthUser("admin@sangmyung.kr");
        given(tempApplyFormService.saveTempApplyForm(any(TempApplyFormSaveRequest.class)))
                .willReturn("temp-form-1");

        String requestBody = """
                {
                  "clubId": "club-1",
                  "title": "임시 지원폼",
                  "subTitle": "부제목",
                  "applyStartDate": "2026-07-01",
                  "applyEndDate": "2026-07-31",
                  "hasInterview": true,
                  "maxApplyCount": 3,
                  "grades": ["FIRST_GRADE"],
                  "formJson": []
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/temp-applyform")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tempApplyFormId").value("temp-form-1"));
    }

    @Test
    @WithMockUser
    @DisplayName("임시 지원폼 저장 시 동아리 ID가 비어있으면 400을 반환한다")
    void saveTempApplyForm_ShouldReturnBadRequest_WhenClubIdBlank() throws Exception {
        // given
        mockAuthUser("admin@sangmyung.kr");

        String requestBody = """
                {
                  "clubId": ""
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/temp-applyform")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
