package org.project.ttokttok.domain.clubboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.global.exception.ErrorMessage;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:controllerAuthorization;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
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

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private FCMService fcmService;

    @Autowired
    private EntityManager entityManager;

    private Club myClub;
    private Club otherClub;
    private String myAccessToken;
    private String otherAccessToken;

    @BeforeEach
    void setUp() {
        Admin myAdmin = adminRepository.save(Admin.adminJoin("boardadmin1", "password123!"));
        Admin otherAdmin = adminRepository.save(Admin.adminJoin("boardadmin2", "password123!"));

        myClub = clubRepository.save(Club.builder()
                .admin(myAdmin)
                .clubName("게시판 테스트 동아리")
                .clubUniv(ClubUniv.ENGINEERING)
                .build());

        otherClub = clubRepository.save(Club.builder()
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
        entityManager.flush();
        entityManager.clear();
        long beforeCount = clubBoardRepository.count();

        mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.details").value(ErrorMessage.ADMIN_NAME_NOT_MATCH.getMessage()));
        entityManager.flush();
        entityManager.clear();
        assertThat(clubBoardRepository.count()).isEqualTo(beforeCount);
        verifyNoInteractions(s3Service, emailService, fcmService);
    }

    @Test
    @DisplayName("createBoard(): 본문의 줄바꿈이 저장 후에도 그대로 유지된다.")
    void createBoard_preservesLineBreaks() throws Exception {
        String content = "첫째 줄\n둘째 줄\n\n넷째 줄";
        CreateBoardRequest request = new CreateBoardRequest("제목입니다", content);

        String response = mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String boardId = objectMapper.readTree(response).get("boardId").asText();

        entityManager.flush();
        entityManager.clear();

        assertThat(clubBoardRepository.findById(boardId))
                .get()
                .extracting(ClubBoard::getContent)
                .isEqualTo(content);
    }

    @Test
    @DisplayName("createBoard(): 내용이 null이어도 생성에 성공하고 빈 내용으로 저장된다.")
    void createBoard_withoutContent() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("제목입니다", null);

        String response = mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String boardId = objectMapper.readTree(response).get("boardId").asText();

        assertThat(clubBoardRepository.findById(boardId))
                .get()
                .extracting(ClubBoard::getContent)
                .isEqualTo("");
    }

    @Test
    @DisplayName("createBoard(): 내용이 빈 문자열이어도 생성에 성공한다.")
    void createBoard_withBlankContent() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("제목입니다", "");

        mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("createBoard(): 제목이 255자를 넘으면 400이 발생한다.")
    void createBoard_titleTooLong() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("가".repeat(256), "본문입니다");

        mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("createBoard(): 제목이 255자면 생성에 성공한다.")
    void createBoard_titleAtMaxLength() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("가".repeat(255), "본문입니다");

        mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("createBoard(): 내용이 10000자를 넘으면 400이 발생한다.")
    void createBoard_contentTooLong() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("제목입니다", "가".repeat(10001));

        mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("createBoard(): 내용이 10000자면 생성에 성공한다.")
    void createBoard_contentAtMaxLength() throws Exception {
        CreateBoardRequest request = new CreateBoardRequest("제목입니다", "가".repeat(10000));

        mockMvc.perform(multipart("/api/admin/clubs/{clubId}/boards", myClub.getId())
                        .file(jsonPart(request))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("updateBoard(): 내용이 10000자를 넘으면 400이 발생한다.")
    void updateBoard_contentTooLong() throws Exception {
        ClubBoard board = saveBoard("원래 제목", "원래 내용");

        ClubBoardUpdateRequest request = new ClubBoardUpdateRequest(null, "가".repeat(10001));

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .file(jsonPart(request))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updateBoard(): 제목이 255자를 넘으면 400이 발생한다.")
    void updateBoard_titleTooLong() throws Exception {
        ClubBoard board = saveBoard("원래 제목", "원래 내용");

        ClubBoardUpdateRequest request = new ClubBoardUpdateRequest("가".repeat(256), null);

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .file(jsonPart(request))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isBadRequest());
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

        entityManager.flush();
        entityManager.clear();
        ClubBoard updated = clubBoardRepository.findById(board.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getContent()).isEqualTo("원래 내용");
        assertThat(updated.getThumbnailUrl()).isEqualTo(EXISTING_THUMBNAIL_URL);
        verifyNoInteractions(s3Service, emailService, fcmService);
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
        entityManager.flush();
        entityManager.clear();
        assertThat(clubBoardRepository.findById(board.getId())).get()
                .extracting(ClubBoard::getThumbnailUrl).isEqualTo(UPLOADED_THUMBNAIL_URL);
    }

    @Test
    @DisplayName("deleteBoard(): 게시글 삭제 시 S3 썸네일이 커밋 후 삭제로 예약된다.")
    void deleteBoard_success() throws Exception {
        ClubBoard board = saveBoard("삭제될 제목", "삭제될 내용");

        mockMvc.perform(delete("/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isOk());

        verify(s3Service).deleteFileAfterCommit(EXISTING_THUMBNAIL_URL);
        entityManager.flush();
        entityManager.clear();
        assertThat(clubBoardRepository.findById(board.getId())).isEmpty();
    }

    @Test
    @DisplayName("deleteBoard(): 다른 동아리 관리자가 요청하면 403이 발생한다.")
    void deleteBoard_forbidden() throws Exception {
        ClubBoard board = saveBoard("삭제될 제목", "삭제될 내용");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.details").value(ErrorMessage.ADMIN_NAME_NOT_MATCH.getMessage()));
        entityManager.flush();
        entityManager.clear();
        assertThat(clubBoardRepository.findById(board.getId())).isPresent();
        verifyNoInteractions(s3Service, emailService, fcmService);
    }

    @Test
    @DisplayName("수정 요청의 동아리가 관리 동아리와 다르면 게시글과 S3를 변경하지 않는다")
    void updateBoard_otherClub_forbidden() throws Exception {
        ClubBoard board = saveBoard("원래 제목", "원래 내용");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/clubs/{clubId}/boards/{boardId}",
                        myClub.getId(), board.getId())
                        .file(jsonPart(new ClubBoardUpdateRequest("새 제목", "새 내용")))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.details").value(ErrorMessage.ADMIN_NAME_NOT_MATCH.getMessage()));

        assertBoardUnchanged(board.getId());
        verifyNoInteractions(s3Service, emailService, fcmService);
    }

    @Test
    @DisplayName("자기 동아리 ID와 다른 동아리 게시글 ID로 수정하면 거부한다")
    void updateBoard_otherBoard_forbidden() throws Exception {
        ClubBoard board = clubBoardRepository.save(
                ClubBoard.create("원래 제목", "원래 내용", EXISTING_THUMBNAIL_URL, otherClub));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/clubs/{clubId}/boards/{boardId}",
                        myClub.getId(), board.getId())
                        .file(jsonPart(new ClubBoardUpdateRequest("새 제목", "새 내용")))
                        .file(thumbnailPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.details").value(ErrorMessage.ADMIN_NAME_NOT_MATCH.getMessage()));

        assertBoardUnchanged(board.getId());
        verifyNoInteractions(s3Service, emailService, fcmService);
    }

    @Test
    @DisplayName("자기 동아리 ID와 다른 동아리 게시글 ID로 삭제하면 거부한다")
    void deleteBoard_otherBoard_forbidden() throws Exception {
        ClubBoard board = clubBoardRepository.save(
                ClubBoard.create("원래 제목", "원래 내용", EXISTING_THUMBNAIL_URL, otherClub));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + myAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.details").value(ErrorMessage.ADMIN_NAME_NOT_MATCH.getMessage()));

        assertBoardUnchanged(board.getId());
        verifyNoInteractions(s3Service, emailService, fcmService);
    }

    @Test
    @DisplayName("토큰 없는 삭제 요청은 401이며 게시글과 외부 서비스에 영향이 없다")
    void deleteBoard_withoutToken_unauthorized() throws Exception {
        ClubBoard board = saveBoard("원래 제목", "원래 내용");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/admin/clubs/{clubId}/boards/{boardId}", myClub.getId(), board.getId()))
                .andExpect(status().isUnauthorized());

        assertBoardUnchanged(board.getId());
        verifyNoInteractions(s3Service, emailService, fcmService);
    }

    private void assertBoardUnchanged(String boardId) {
        entityManager.flush();
        entityManager.clear();
        ClubBoard board = clubBoardRepository.findById(boardId).orElseThrow();
        assertThat(board.getTitle()).isEqualTo("원래 제목");
        assertThat(board.getContent()).isEqualTo("원래 내용");
        assertThat(board.getThumbnailUrl()).isEqualTo(EXISTING_THUMBNAIL_URL);
    }
}
