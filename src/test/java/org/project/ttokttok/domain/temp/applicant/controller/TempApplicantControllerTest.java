package org.project.ttokttok.domain.temp.applicant.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.temp.applicant.service.TempApplicantService;
import org.project.ttokttok.domain.temp.applicant.service.dto.request.TempApplicantSaveServiceRequest;
import org.project.ttokttok.domain.temp.applicant.service.dto.response.TempApplicantDataServiceResponse;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TempApplicantController.class)
class TempApplicantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TempApplicantService tempApplicantService;

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
    @DisplayName("임시 지원서 저장 API를 호출하면 201과 저장된 아이디를 반환한다")
    void saveTempApplicant_ShouldReturnCreated() throws Exception {
        // given
        mockAuthUser("test@sangmyung.kr");
        given(tempApplicantService.saveTempApplicant(any(TempApplicantSaveServiceRequest.class)))
                .willReturn("temp-applicant-1");

        String requestJson = """
                {
                  "name": "홍길동",
                  "age": 22,
                  "major": "컴퓨터공학과",
                  "email": "test@sangmyung.kr",
                  "phone": "010-1234-5678",
                  "studentStatus": "ENROLLED",
                  "grade": "FIRST_GRADE",
                  "gender": "MALE",
                  "answers": [
                    {"questionId": "q1", "value": "답변입니다"}
                  ]
                }
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/user/temp-applicant/{formId}", "form-1")
                        .file(requestPart)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tempApplicantId").value("temp-applicant-1"));
    }

    @Test
    @WithMockUser
    @DisplayName("임시 지원서 저장 API 호출 시 파일이 함께 전달되면 정상 처리된다")
    void saveTempApplicant_WithFile_ShouldReturnCreated() throws Exception {
        // given
        mockAuthUser("test@sangmyung.kr");
        given(tempApplicantService.saveTempApplicant(any(TempApplicantSaveServiceRequest.class)))
                .willReturn("temp-applicant-2");

        String requestJson = """
                {
                  "name": "홍길동",
                  "answers": []
                }
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes()
        );
        MockMultipartFile filePart = new MockMultipartFile(
                "files", "resume.pdf", "application/pdf", "content".getBytes()
        );
        MockMultipartFile questionIdsPart = new MockMultipartFile(
                "questionIds", "", MediaType.APPLICATION_JSON_VALUE, "[\"q-file\"]".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/user/temp-applicant/{formId}", "form-1")
                        .file(requestPart)
                        .file(filePart)
                        .file(questionIdsPart)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tempApplicantId").value("temp-applicant-2"));
    }

    @Test
    @WithMockUser
    @DisplayName("임시 지원서 조회 API를 호출하면 200과 저장된 데이터를 반환한다")
    void getTempApplicant_ShouldReturnOk() throws Exception {
        // given
        mockAuthUser("test@sangmyung.kr");
        Map<String, Object> data = new HashMap<>();
        data.put("name", "홍길동");

        given(tempApplicantService.getTempApplicantData("test@sangmyung.kr", "form-1"))
                .willReturn(TempApplicantDataServiceResponse.builder()
                        .hasTempData(true)
                        .data(data)
                        .build());

        // when & then
        mockMvc.perform(get("/api/user/temp-applicant/{formId}", "form-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasTempData").value(true))
                .andExpect(jsonPath("$.data.name").value("홍길동"));
    }

    @Test
    @WithMockUser
    @DisplayName("임시 지원서가 없으면 hasTempData가 false로 반환된다")
    void getTempApplicant_ShouldReturnHasTempDataFalse() throws Exception {
        // given
        mockAuthUser("test@sangmyung.kr");
        given(tempApplicantService.getTempApplicantData("test@sangmyung.kr", "form-1"))
                .willReturn(TempApplicantDataServiceResponse.builder()
                        .hasTempData(false)
                        .data(null)
                        .build());

        // when & then
        mockMvc.perform(get("/api/user/temp-applicant/{formId}", "form-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasTempData").value(false));
    }
}
