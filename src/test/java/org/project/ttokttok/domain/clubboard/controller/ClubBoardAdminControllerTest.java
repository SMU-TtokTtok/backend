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
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClubBoardAdminControllerTest {

    // 저장된 게시글이 이미 갖고 있는 기존 썸네일 URL과 신규 업로드 결과 URL을 구분해
    // "기존 파일이 삭제 예약되었는지"를 정확히 검증한다.
    private static final String EXISTING_THUMBNAIL_URL = "https://cdn.example.com/board-images/uuid_existing.png";
    private static final String UPLOADED_THUMBNAIL_URL = "https://cdn.example.com/board-images/uuid_uploaded.png";

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

    @MockitoBean
    private S3Service s3Service;

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

        given(s3Service.uploadFile(any(MultipartFile.class), anyString())).willReturn(UPLOADED_THUMBNAIL_URL);
    }

    private MockMultipartFile jsonPart(Object request) throws Exception {
        return new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
    }

    private MockMultipartFile thumbnailPart() {
        return new MockMultipartFile("thumbnail", "thumb.png", "image/png", "img".getBytes());
    }

    private ClubBoard saveBoard(String title, String content) {
        return clubBoardRepository.save(ClubBoard.create(title, content, EXISTING_THUMBNAIL_URL, myClub));
    }

    @Test
    @DisplayName("createBoard(): 썸네일과 함께 게시글 생성에 성공한다.")
    void createBoard_success() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("제목입니다", "본문입니다");

        mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.boardId", notNullValue()));

        verify(s3Service).uploadFile(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("createBoard(): 썸네일 파트가 없으면 400이 발생한다.")
    void createBoard_missingThumbnail() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("제목입니다", "본문입니다");

        mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("createBoard(): 다른 동아리 관리자가 요청하면 403이 발생한다.")
    void createBoard_forbidden() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("제목입니다", "본문입니다");

        mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("updateBoard(): 게시글 수정에 성공한다.")
    void updateBoard_success() throws Exception {
        ClubBoard board = saveBoard("원래 제목", "원래 내용");

        ClubBoardUpdateRequest request = new ClubBoardUpdateRequest("수정된 제목", null);

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .file(jsonPart(request))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("updateBoard(): 썸네일만 보내도 교체에 성공하고 기존 파일이 커밋 후 삭제로 예약된다.")
    void updateBoard_replaceThumbnailOnly() throws Exception {
        ClubBoard board = saveBoard("원래 제목", "원래 내용");

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isOk());

        verify(s3Service).uploadFile(any(MultipartFile.class), anyString());
        // 신규 업로드본은 롤백 보상 훅, 기존 파일은 커밋 후 삭제로 각각 예약된다.
        verify(s3Service).deleteFileOnRollback(UPLOADED_THUMBNAIL_URL);
        verify(s3Service).deleteFileAfterCommit(EXISTING_THUMBNAIL_URL);
    }

    @Test
    @DisplayName("deleteBoard(): 게시글 삭제 시 S3 썸네일이 커밋 후 삭제로 예약된다.")
    void deleteBoard_success() throws Exception {
        ClubBoard board = saveBoard("삭제될 제목", "삭제될 내용");

        mockMvc.perform(delete("/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isOk());

        verify(s3Service).deleteFileAfterCommit(EXISTING_THUMBNAIL_URL);
    }

    @Test
    @DisplayName("deleteBoard(): 다른 동아리 관리자가 요청하면 403이 발생한다.")
    void deleteBoard_forbidden() throws Exception {
        ClubBoard board = saveBoard("삭제될 제목", "삭제될 내용");

        mockMvc.perform(delete("/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAccessToken))
                .andExpect(status().isForbidden());
    }
}
