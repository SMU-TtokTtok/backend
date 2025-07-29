package org.project.ttokttok.domain.applicant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.service.ApplicantUserService;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    @DisplayName("지원서 제출 요청이 성공적으로 처리되는지 테스트")
    void apply_ShouldReturnNoContent() throws Exception {
        // given
        String validApplyFormJson = """
            {
                "name": "홍길동",
                "age": 22,
                "major": "컴퓨터공학과",
                "email": "test@test.com",
                "phone": "010-1234-5678",
                "studentStatus": "ATTENDING",
                "grade": "SOPHOMORE",
                "gender": "MALE",
                "answers": []
            }
            """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                validApplyFormJson.getBytes()
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "files",
                "resume.pdf",
                "application/pdf",
                "test file content".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/applies")
                        .file(requestPart)
                        .file(filePart)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}