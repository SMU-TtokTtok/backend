package org.project.ttokttok.domain.applyform.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.exception.ActiveApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.service.ApplyFormUserService;
import org.project.ttokttok.domain.applyform.service.dto.response.ActiveApplyFormServiceResponse;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplyFormUserController.class)
class ApplyFormUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplyFormUserService applyFormUserService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    @Test
    @WithMockUser
    @DisplayName("활성화된 지원폼 조회 API를 호출하면 200과 지원폼 정보를 반환한다")
    void getActiveApplyForm_ShouldReturnOk() throws Exception {
        // given
        String clubId = "club-1";
        List<Question> questions = List.of(
                new Question("q1", "이름", null, QuestionType.SHORT_ANSWER, true, null)
        );
        ActiveApplyFormServiceResponse serviceResponse = ActiveApplyFormServiceResponse.of(
                "form-1", "동아리 지원폼", "부제목", questions
        );
        given(applyFormUserService.getActiveApplyForm(clubId)).willReturn(serviceResponse);

        // when & then
        mockMvc.perform(get("/api/forms/{clubId}", clubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formId").value("form-1"))
                .andExpect(jsonPath("$.title").value("동아리 지원폼"))
                .andExpect(jsonPath("$.subTitle").value("부제목"))
                .andExpect(jsonPath("$.questions.length()").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("활성화된 지원폼이 없으면 404를 반환한다")
    void getActiveApplyForm_ShouldReturnNotFound() throws Exception {
        // given
        String clubId = "club-1";
        given(applyFormUserService.getActiveApplyForm(clubId))
                .willThrow(new ActiveApplyFormNotFoundException());

        // when & then
        mockMvc.perform(get("/api/forms/{clubId}", clubId))
                .andExpect(status().isNotFound());
    }
}
