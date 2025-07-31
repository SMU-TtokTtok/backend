package org.project.ttokttok.domain.applicant.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.service.ApplicantUserService;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicantUserApiController.class)
class ApplicantUserApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicantUserService applicantUserService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    @Test
    @WithMockUser
    @DisplayName("지원서 제출 요청이 성공적으로 처리 완료되는지 테스트한다.")
    void apply_ShouldReturnOk() throws Exception {
        String formId = "test-form-id";
        String validApplyFormJson = """
                {
                  "name": "홍길동",
                  "age": 22,
                  "major": "컴퓨터공학과",
                  "email": "honggildong@example.com",
                  "phone": "010-1234-5678",
                  "studentStatus": "ENROLLED",
                  "grade": "FIRST_GRADE",
                  "gender": "MALE",
                  "applyFormId": "form-abc-123",
                  "answers": [
                    {
                      "questionId": "q1",
                      "value": "이 지원서를 작성한 이유는 컴퓨터공학에 관심이 많기 때문입니다."
                    },
                    {
                      "questionId": "q2",
                      "value": ["리더십", "팀워크"]
                    },
                    {
                      "questionId": "q3",
                      "value": "1"
                    },
                    {
                      "questionId": "q4",
                      "value": null
                    }
                  ]
                }
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                validApplyFormJson.getBytes()
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "q4",
                "resume.pdf",
                "multipart/form-data",
                "test file content".getBytes()
        );

        MockMultipartFile questionIdsPart = new MockMultipartFile(
                "questionIds",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "[\"q4\"]".getBytes()
        );

        mockMvc.perform(multipart("/api/user/applies/{formId}", formId)
                        .file(requestPart)
                        .file(filePart)
                        .file(questionIdsPart)
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}