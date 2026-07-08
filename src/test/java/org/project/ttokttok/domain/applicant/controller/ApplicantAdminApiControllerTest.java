package org.project.ttokttok.domain.applicant.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.service.ApplicantAdminService;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantFinalizationRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantPageServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantSearchServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantStatusServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.StatusUpdateServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantDetailServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantFinalizeServiceResponse;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantPageServiceResponse;
import org.project.ttokttok.global.annotationresolver.auth.AuthUserInfoResolver;
import org.project.ttokttok.global.auth.jwt.service.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicantAdminApiController.class)
class ApplicantAdminApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicantAdminService applicantAdminService;

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
    @DisplayName("지원자 목록 조회 API를 호출하면 200을 반환한다")
    void getApplicantsPage() throws Exception {
        mockAuthUser("admin@sangmyung.kr");
        given(applicantAdminService.getApplicantPage(any(ApplicantPageServiceRequest.class)))
                .willReturn(ApplicantPageServiceResponse.toEmpty());

        mockMvc.perform(get("/api/admin/applies").param("kind", "DOCUMENT"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("지원자 상세 조회 API를 호출하면 200과 지원자 정보를 반환한다")
    void getApplicantDetail() throws Exception {
        mockAuthUser("admin@sangmyung.kr");
        ApplicantDetailServiceResponse detail = ApplicantDetailServiceResponse.of(
                "홍길동", 22, "컴퓨터과학과", "hong@sangmyung.kr", "010-1234-5678",
                StudentStatus.ENROLLED, Grade.FIRST_GRADE, Gender.MALE, List.of(), List.of()
        );
        given(applicantAdminService.getApplicantDetail(any(), any())).willReturn(detail);

        mockMvc.perform(get("/api/admin/applies/{applicantId}", "applicant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.major").value("컴퓨터과학과"));
    }

    @Test
    @WithMockUser
    @DisplayName("지원자 검색 API를 호출하면 200을 반환한다")
    void applicantPageSearch() throws Exception {
        mockAuthUser("admin@sangmyung.kr");
        given(applicantAdminService.searchApplicantByKeyword(any(ApplicantSearchServiceRequest.class)))
                .willReturn(ApplicantPageServiceResponse.toEmpty());

        mockMvc.perform(get("/api/admin/applies/search").param("name", "홍").param("kind", "DOCUMENT"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("합격자 목록 조회 API를 호출하면 200을 반환한다")
    void getPassedApplicantsPage() throws Exception {
        mockAuthUser("admin@sangmyung.kr");
        given(applicantAdminService.getApplicantsByStatus(any(ApplicantStatusServiceRequest.class)))
                .willReturn(ApplicantPageServiceResponse.toEmpty());

        mockMvc.perform(get("/api/admin/applies/passed").param("kind", "DOCUMENT"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("불합격자 목록 조회 API를 호출하면 200을 반환한다")
    void getFailedApplicantsPage() throws Exception {
        mockAuthUser("admin@sangmyung.kr");
        given(applicantAdminService.getApplicantsByStatus(any(ApplicantStatusServiceRequest.class)))
                .willReturn(ApplicantPageServiceResponse.toEmpty());

        mockMvc.perform(get("/api/admin/applies/failed").param("kind", "DOCUMENT"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("지원자 평가 상태 변경 API를 호출하면 200과 완료 메시지를 반환한다")
    void updateApplicantEvaluation() throws Exception {
        mockAuthUser("admin@sangmyung.kr");
        doNothing().when(applicantAdminService).updateApplicantStatus(any(StatusUpdateServiceRequest.class));

        mockMvc.perform(patch("/api/admin/applies/evaluations/{applicantId}", "applicant-1").with(csrf())
                        .param("kind", "DOCUMENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PASS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("지원자 상태가 성공적으로 업데이트되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("지원자 상태를 확정하는 API를 호출하면 200과 확정 결과를 반환한다")
    void finalizeApplicantsStatus() throws Exception {
        mockAuthUser("admin@sangmyung.kr");
        given(applicantAdminService.finalizeApplicantsStatus(any(ApplicantFinalizationRequest.class)))
                .willReturn(ApplicantFinalizeServiceResponse.of(3, 5));

        mockMvc.perform(put("/api/admin/applies/{clubId}/finalize", "club-1").with(csrf())
                        .param("kind", "DOCUMENT"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("지원자에게 결과 메일 전송 API를 호출하면 200과 완료 메시지를 반환한다")
    void sendEmailToApplicants() throws Exception {
        mockAuthUser("admin@sangmyung.kr");
        doNothing().when(applicantAdminService)
                .sendResultMailToApplicants(any(), any(), any(), any());

        String body = "{\"pass\":{\"title\":\"합격 안내\",\"body\":\"축하합니다\"},"
                + "\"fail\":{\"title\":\"불합격 안내\",\"body\":\"감사합니다\"}}";

        mockMvc.perform(post("/api/admin/applies/{clubId}/send-email", "club-1").with(csrf())
                        .param("kind", "DOCUMENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 전송이 완료되었습니다."));
    }
}
