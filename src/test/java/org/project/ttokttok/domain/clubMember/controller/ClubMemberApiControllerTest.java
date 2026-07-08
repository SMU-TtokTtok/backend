package org.project.ttokttok.domain.clubMember.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.service.ClubMemberService;
import org.project.ttokttok.domain.clubMember.service.dto.request.ChangeRoleServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberSearchRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.DeleteMemberServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberCountServiceResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberPageServiceResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberSearchServiceResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ExcelServiceResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClubMemberApiController.class)
class ClubMemberApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubMemberService clubMemberService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AuthUserInfoResolver authUserInfoResolver;

    private static final String USERNAME = "clubadmin1";
    private static final String CLUB_ID = "club-1";

    private void stubAuthUserInfo() throws Exception {
        given(authUserInfoResolver.supportsParameter(any())).willReturn(true);
        given(authUserInfoResolver.resolveArgument(any(), any(), any(), any())).willReturn(USERNAME);
    }

    @Test
    @WithMockUser
    @DisplayName("동아리 부원 목록을 페이지 단위로 조회한다")
    void getClubMembers() throws Exception {
        // given
        stubAuthUserInfo();
        ClubMemberPageServiceResponse response = ClubMemberPageServiceResponse.builder()
                .currentPage(1)
                .totalPage(1)
                .totalCount(0)
                .clubMembers(List.of())
                .build();
        given(clubMemberService.getClubMembers(eq(USERNAME), eq(CLUB_ID), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/members/{clubId}", CLUB_ID)
                        .param("page", "1")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPage").value(1))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("동아리 부원 총 수를 조회한다")
    void getClubMemberCount() throws Exception {
        // given
        stubAuthUserInfo();
        ClubMemberCountServiceResponse response = ClubMemberCountServiceResponse.builder()
                .totalCount(4)
                .firstGradeCount(1)
                .secondGradeCount(1)
                .thirdGradeCount(1)
                .fourthGradeCount(1)
                .build();
        given(clubMemberService.getClubMembersCount(CLUB_ID, USERNAME)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/members/{clubId}/total-count", CLUB_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(4))
                .andExpect(jsonPath("$.firstGradeCount").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("부원의 역할을 변경한다")
    void changeRole() throws Exception {
        // given
        stubAuthUserInfo();
        String memberId = "member-1";

        // when & then
        mockMvc.perform(patch("/api/admin/members/{clubId}/{memberId}/role", CLUB_ID, memberId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"EXECUTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("부원 역할 변경 완료: " + memberId));

        verify(clubMemberService, times(1)).changeRole(eq(USERNAME), any(ChangeRoleServiceRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("부원을 삭제한다")
    void deleteMember() throws Exception {
        // given
        stubAuthUserInfo();
        String memberId = "member-1";

        // when & then
        mockMvc.perform(delete("/api/admin/members/{clubId}/{memberId}", CLUB_ID, memberId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("부원 삭제 완료: " + memberId));

        verify(clubMemberService, times(1)).deleteMember(eq(USERNAME), any(DeleteMemberServiceRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("부원 목록을 엑셀 파일로 다운로드한다")
    void downloadMembersExcel() throws Exception {
        // given
        stubAuthUserInfo();
        byte[] excelBytes = {1, 2, 3};
        given(clubMemberService.downloadMembersAsExcel(CLUB_ID, USERNAME))
                .willReturn(new ExcelServiceResponse("테스트동아리", excelBytes));

        // when & then
        mockMvc.perform(get("/api/admin/members/{clubId}/download", CLUB_ID))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser
    @DisplayName("키워드로 부원을 검색한다")
    void searchMembers() throws Exception {
        // given
        stubAuthUserInfo();
        List<ClubMemberSearchServiceResponse> serviceResponses = List.of(
                ClubMemberSearchServiceResponse.of("member-1", Grade.FIRST_GRADE, "홍길동", "컴퓨터공학과", MemberRole.MEMBER)
        );
        given(clubMemberService.clubMemberSearch(eq(USERNAME), any(ClubMemberSearchRequest.class)))
                .willReturn(serviceResponses);

        // when & then
        mockMvc.perform(get("/api/admin/members/{clubId}/search", CLUB_ID)
                        .param("keyword", "홍")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubMembers[0].name").value("홍길동"));
    }

    @Test
    @WithMockUser
    @DisplayName("동아리에 부원을 추가한다")
    void addMembers() throws Exception {
        // given
        stubAuthUserInfo();
        given(clubMemberService.addMember(eq(USERNAME), eq(CLUB_ID), any(), eq("MEMBER")))
                .willReturn("new-member-id");

        String requestBody = """
                {
                    "studentNum": 60201234,
                    "name": "홍길동",
                    "major": "컴퓨터공학과",
                    "grade": "FIRST_GRADE",
                    "email": "test1@sangmyung.kr",
                    "phoneNumber": "010-1111-1111",
                    "gender": "MALE"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/members/{clubId}/add", CLUB_ID)
                        .with(csrf())
                        .param("role", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value("new-member-id"));
    }
}
