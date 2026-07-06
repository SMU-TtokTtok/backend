package org.project.ttokttok.domain.clubboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.admin.repository.AdminRepository;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.controller.dto.request.ClubBoardUpdateRequest;
import org.project.ttokttok.domain.clubboard.controller.dto.request.CreateBoardRequest;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.jwt.JwtFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClubBoardAdminControllerTest {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ClubBoardRepository clubBoardRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtFactory jwtFactory;

    private Club myClub;
    private String myAccessToken;
    private String otherAccessToken;

    @BeforeEach
    void setUp() {
        Admin myAdmin = adminRepository.save(Admin.adminJoin("boardadmin1", "password123!", "boardadmin1@sangmyung.kr"));
        Admin otherAdmin = adminRepository.save(Admin.adminJoin("boardadmin2", "password123!", "boardadmin2@sangmyung.kr"));

        myClub = clubRepository.save(Club.builder()
                .admin(myAdmin)
                .clubName("게시판 테스트 동아리")
                .clubUniv(ClubUniv.ENGINEERING)
                .build());

        clubRepository.save(Club.builder()
                .admin(otherAdmin)
                .clubName("다른 동아리")
                .clubUniv(ClubUniv.DESIGN)
                .build());

        myAccessToken = jwtFactory.generateValidToken(myAdmin.getUsername(), Role.ROLE_ADMIN);
        otherAccessToken = jwtFactory.generateValidToken(otherAdmin.getUsername(), Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("createBoard(): 게시글 생성에 성공한다.")
    void createBoard_success() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("제목입니다", "본문입니다");

        mockMvc.perform(post("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.boardId", notNullValue()));
    }

    @Test
    @DisplayName("createBoard(): 다른 동아리 관리자가 요청하면 403이 발생한다.")
    void createBoard_forbidden() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("제목입니다", "본문입니다");

        mockMvc.perform(post("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("updateBoard(): 게시글 수정에 성공한다.")
    void updateBoard_success() throws Exception {
        ClubBoard board = clubBoardRepository.save(ClubBoard.create("원래 제목", "원래 내용", myClub));

        ClubBoardUpdateRequest request = new ClubBoardUpdateRequest("수정된 제목", null);

        mockMvc.perform(patch("/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deleteBoard(): 게시글 삭제에 성공한다.")
    void deleteBoard_success() throws Exception {
        ClubBoard board = clubBoardRepository.save(ClubBoard.create("삭제될 제목", "삭제될 내용", myClub));

        mockMvc.perform(delete("/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deleteBoard(): 다른 동아리 관리자가 요청하면 403이 발생한다.")
    void deleteBoard_forbidden() throws Exception {
        ClubBoard board = clubBoardRepository.save(ClubBoard.create("삭제될 제목", "삭제될 내용", myClub));

        mockMvc.perform(delete("/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAccessToken))
                .andExpect(status().isForbidden());
    }
}
