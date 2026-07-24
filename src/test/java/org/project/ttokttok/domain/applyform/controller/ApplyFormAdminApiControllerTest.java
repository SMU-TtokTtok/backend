package org.project.ttokttok.domain.applyform.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.service.ApplyFormAdminService;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormCreateServiceRequest;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormUpdateServiceRequest;
import org.project.ttokttok.domain.applyform.service.dto.response.ApplyFormDetailServiceResponse;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.project.ttokttok.global.config.JsonConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplyFormAdminApiController.class)
@Import(JsonConfig.class)
class ApplyFormAdminApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplyFormAdminService applyFormAdminService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    private void mockAuthUser(String username) throws Exception {
        given(authUserInfoResolver.supportsParameter(any())).willReturn(true);
        given(authUserInfoResolver.resolveArgument(any(), any(), any(), any())).willReturn(username);
    }

    @Test
    @WithMockUser
    @DisplayName("지원폼 생성 API를 호출하면 201과 생성된 formId를 반환한다")
    void createApplyForm_ShouldReturnCreated() throws Exception {
        // given
        mockAuthUser("admin@sangmyung.kr");
        given(applyFormAdminService.createApplyForm(any(ApplyFormCreateServiceRequest.class)))
                .willReturn("form-1");

        String requestBody = """
                {
                  "hasInterview": true,
                  "recruitStartDate": "2026-07-01",
                  "recruitEndDate": "2026-07-31",
                  "applicableGrades": [1, 2],
                  "maxApplyCount": 1,
                  "interviewStartDate": "2026-08-01",
                  "interviewEndDate": "2026-08-05",
                  "title": "동아리 지원폼",
                  "subTitle": "2026년 상반기 모집",
                  "questions": [
                    {
                      "title": "이름",
                      "subTitle": null,
                      "questionType": "SHORT_ANSWER",
                      "isEssential": true,
                      "content": []
                    }
                  ]
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/forms/clubs/{clubId}", "club-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.formId").value("form-1"));
    }

    @Test
    @WithMockUser
    @DisplayName("지원폼 생성 시 제목이 비어있으면 400을 반환한다")
    void createApplyForm_ShouldReturnBadRequest_WhenTitleBlank() throws Exception {
        // given
        mockAuthUser("admin@sangmyung.kr");

        String requestBody = """
                {
                  "hasInterview": false,
                  "recruitStartDate": "2026-07-01",
                  "recruitEndDate": "2026-07-31",
                  "applicableGrades": [1],
                  "maxApplyCount": 1,
                  "title": "",
                  "subTitle": null,
                  "questions": []
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/forms/clubs/{clubId}", "club-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("지원폼 상세 조회 API를 호출한다")
    void getApplyFormsByClubId_ShouldReturnOk() throws Exception {
        // given
        mockAuthUser("admin@sangmyung.kr");

        List<Question> questions = List.of(
                new Question("q1", "이름", null, QuestionType.SHORT_ANSWER, true, null)
        );
        ApplyFormDetailServiceResponse serviceResponse = ApplyFormDetailServiceResponse.of(
                "form-1", "동아리 지원폼", "부제목", questions, List.of()
        );
        given(applyFormAdminService.getApplyFormDetail("admin@sangmyung.kr", "club-1"))
                .willReturn(serviceResponse);

        // when & then
        mockMvc.perform(get("/api/admin/forms/{clubId}", "club-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formId").value("form-1"))
                .andExpect(jsonPath("$.title").value("동아리 지원폼"));
    }

    @Test
    @WithMockUser
    @DisplayName("지원폼 수정 API를 호출하면 성공 메시지를 반환한다")
    void updateApplyForm_ShouldReturnOk() throws Exception {
        // given
        mockAuthUser("admin@sangmyung.kr");

        String requestBody = """
                {
                  "title": "새로운 제목"
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/admin/forms/{formId}", "form-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("지원 폼이 성공적으로 수정되었습니다."));

        org.mockito.Mockito.verify(applyFormAdminService)
                .updateApplyForm(any(ApplyFormUpdateServiceRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("이전 지원폼 질문 조회 API를 호출한다")
    void getPreviousQuestions_ShouldReturnOk() throws Exception {
        // given
        mockAuthUser("admin@sangmyung.kr");
        List<Question> questions = List.of(
                new Question("q1", "이름", null, QuestionType.SHORT_ANSWER, true, null)
        );
        given(applyFormAdminService.getPreviousApplyFormQuestions("admin@sangmyung.kr", "form-1"))
                .willReturn(questions);

        // when & then
        mockMvc.perform(get("/api/admin/forms/before/{formId}", "form-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beforeQuestions.length()").value(1))
                .andExpect(jsonPath("$.beforeQuestions[0].questionId").value("q1"));
    }

    @Test
    @WithMockUser
    @DisplayName("지원자 평가 종료 API를 호출하면 성공 메시지를 반환한다")
    void finishEvaluating_ShouldReturnOk() throws Exception {
        // given
        mockAuthUser("admin@sangmyung.kr");

        // when & then
        mockMvc.perform(post("/api/admin/forms/finish/{formId}", "form-1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("지원자 평가 종료가 성공적으로 완료되었습니다."));

        org.mockito.Mockito.verify(applyFormAdminService)
                .finishEvaluation("admin@sangmyung.kr", "form-1");
    }
}
